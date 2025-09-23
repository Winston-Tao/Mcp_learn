#!/usr/bin/env bash
# Debian 自动安装 Java 21 + Maven（默认 3.9.9）
# - 优先从 Debian 源安装 openjdk-21（bookworm-backports）
# - 失败则回退到 Adoptium/Temurin JDK 21
# - 安装 Maven（可改 MAVEN_VERSION），写入 /etc/profile.d/ 环境变量
# - 幂等、带基本校验
set -euo pipefail

# ===== 可自定义区域 =====
MAVEN_VERSION="${MAVEN_VERSION:-3.9.9}"
MAVEN_BASE_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"
MAVEN_ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_SHA_URL="${MAVEN_BASE_URL}/${MAVEN_ARCHIVE}.sha512"
MAVEN_TGZ_URL="${MAVEN_BASE_URL}/${MAVEN_ARCHIVE}"

# Adoptium 最新 GA（JDK 21, linux x64, hotspot）
TEMURIN_URL="${TEMURIN_URL:-https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/adoptium}"

# 安装路径
JVM_DIR="/usr/lib/jvm"
TEMURIN_HOME="${JVM_DIR}/temurin-21"
MAVEN_DIR="/opt/apache-maven-${MAVEN_VERSION}"
MAVEN_LINK="/opt/maven"
JAVA_PROFILE="/etc/profile.d/java.sh"
MAVEN_PROFILE="/etc/profile.d/maven.sh"

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "需要命令 $1，但未发现。请先安装。"; exit 1; }
}

need_root() {
  if [ "$EUID" -ne 0 ]; then
    echo "请用 sudo 或 root 运行此脚本。"
    exit 1
  fi
}

detect_debian_codename() {
  . /etc/os-release
  echo "${VERSION_CODENAME:-}"
}

install_pkgs() {
  apt-get update
  apt-get install -y curl wget ca-certificates tar gnupg coreutils
}

install_openjdk21_from_repo_if_possible() {
  local codename="$1"
  if [ "$codename" = "bookworm" ]; then
    echo "检测到 Debian ${codename}，尝试通过 backports 安装 openjdk-21-jdk ..."
    echo "deb http://deb.debian.org/debian ${codename}-backports main" > /etc/apt/sources.list.d/${codename}-backports.list
    apt-get update
    if apt-get -t ${codename}-backports install -y openjdk-21-jdk; then
      echo "openjdk-21-jdk 已通过 backports 安装。"
      return 0
    fi
  fi
  # 其他版本：尝试直接装（有些发行会自带）
  echo "尝试直接通过仓库安装 openjdk-21-jdk（若失败将回落到 Adoptium）..."
  if apt-get install -y openjdk-21-jdk; then
    echo "openjdk-21-jdk 已通过仓库安装。"
    return 0
  fi
  return 1
}

install_temurin21_fallback() {
  echo "回退到 Adoptium/Temurin 安装 JDK 21 ..."
  mkdir -p "${JVM_DIR}"
  tmpdir="$(mktemp -d)"
  pushd "${tmpdir}" >/dev/null
  echo "下载：${TEMURIN_URL}"
  curl -fsSL -o temurin21.tar.gz "${TEMURIN_URL}"
  mkdir -p "${TEMURIN_HOME}"
  tar -xzf temurin21.tar.gz -C "${TEMURIN_HOME}" --strip-components=1
  popd >/dev/null
  rm -rf "${tmpdir}"

  # 配置 alternatives
  update-alternatives --install /usr/bin/java  java  "${TEMURIN_HOME}/bin/java"  201
  update-alternatives --install /usr/bin/javac javac "${TEMURIN_HOME}/bin/javac" 201
  update-alternatives --set java  "${TEMURIN_HOME}/bin/java"
  update-alternatives --set javac "${TEMURIN_HOME}/bin/javac"

  echo "Temurin JDK 21 安装完成。"
}

write_java_profile() {
  # 尝试自动探测 java home
  local JAVA_BIN
  JAVA_BIN="$(command -v java || true)"
  local JAVA_HOME_VAL=""
  if [ -n "$JAVA_BIN" ]; then
    # /usr/lib/jvm/java-21-openjdk-amd64/bin/java -> JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    JAVA_HOME_VAL="$(readlink -f "$JAVA_BIN" | sed 's:/bin/java::')"
    JAVA_HOME_VAL="$(dirname "$JAVA_HOME_VAL")"
  fi
  if [ -z "$JAVA_HOME_VAL" ] || [ ! -d "$JAVA_HOME_VAL" ]; then
    # 回退到 temurin
    JAVA_HOME_VAL="${TEMURIN_HOME}"
  fi

  cat > "${JAVA_PROFILE}" <<EOF
# Java 21 环境变量（自动生成）
export JAVA_HOME="${JAVA_HOME_VAL}"
export PATH="\$JAVA_HOME/bin:\$PATH"
EOF
  chmod +x "${JAVA_PROFILE}"
}

install_maven() {
  echo "安装 Maven ${MAVEN_VERSION} ..."
  mkdir -p /opt
  if [ -d "${MAVEN_DIR}" ]; then
    echo "检测到 ${MAVEN_DIR} 已存在，跳过解压。"
  else
    tmpdir="$(mktemp -d)"
    pushd "${tmpdir}" >/dev/null
    echo "下载：${MAVEN_TGZ_URL}"
    curl -fsSL -O "${MAVEN_TGZ_URL}"
    echo "下载 sha512：${MAVEN_SHA_URL}"
    curl -fsSL -O "${MAVEN_SHA_URL}"

    echo "校验 Maven 包 sha512 ..."
    # sha512 格式可能是“<hash>  <filename>”
    if sha512sum -c "${MAVEN_ARCHIVE}.sha512" 2>/dev/null || \
       (echo "$(cat ${MAVEN_ARCHIVE}.sha512 | awk '{print $1}')  ${MAVEN_ARCHIVE}" | sha512sum -c -); then
      echo "校验通过。"
    else
      echo "校验失败，安装中止。"
      exit 1
    fi

    tar -xzf "${MAVEN_ARCHIVE}" -C /opt
    popd >/dev/null
    rm -rf "${tmpdir}"
  fi

  ln -sfn "${MAVEN_DIR}" "${MAVEN_LINK}"

  # 写环境变量
  cat > "${MAVEN_PROFILE}" <<EOF
# Maven 环境变量（自动生成）
export M2_HOME="${MAVEN_LINK}"
export PATH="\$M2_HOME/bin:\$PATH"
EOF
  chmod +x "${MAVEN_PROFILE}"
}

print_versions() {
  # 让当前 shell 也能马上用（新开终端将自动生效）
  # shellcheck disable=SC1091
  source "${JAVA_PROFILE}"
  # shellcheck disable=SC1091
  source "${MAVEN_PROFILE}" || true

  echo "=== 版本检查 ==="
  java -version || true
  echo
  mvn -version || true
}

main() {
  need_root
  need_cmd apt-get
  install_pkgs

  codename="$(detect_debian_codename || true)"
  echo "检测到 Debian 代号：${codename:-unknown}"

  if install_openjdk21_from_repo_if_possible "${codename:-}"; then
    echo "Java 21 已通过 Debian 仓库安装。"
  else
    install_temurin21_fallback
  fi

  write_java_profile
  install_maven
  print_versions

  echo
  echo "✅ 完成！请重新打开一个终端，或运行："
  echo "   source ${JAVA_PROFILE} && source ${MAVEN_PROFILE}"
  echo
  echo "提示：如需卸载："
  echo "  - apt 装的 openjdk：  sudo apt remove --purge openjdk-21-jdk -y"
  echo "  - Temurin 装的 JDK：  sudo rm -rf ${TEMURIN_HOME} && sudo update-alternatives --remove java ${TEMURIN_HOME}/bin/java || true && sudo update-alternatives --remove javac ${TEMURIN_HOME}/bin/javac || true"
  echo "  - Maven：              sudo rm -rf ${MAVEN_DIR} ${MAVEN_LINK} ${MAVEN_PROFILE}"
  echo "  - 环境变量文件：        sudo rm -f  ${JAVA_PROFILE}"
}

main "$@"
