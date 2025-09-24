package com.example.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class TemplatePromptProvider extends AbstractPromptProvider {

    @Override
    public String getName() {
        return "code_review";
    }

    @Override
    public String getDescription() {
        return "Generate a comprehensive code review prompt with customizable focus areas";
    }

    @Override
    public List<Map<String, Object>> getArguments() {
        return List.of(
                Map.of(
                        "name", "language",
                        "description", "Programming language of the code being reviewed",
                        "required", true,
                        "type", "string"
                ),
                Map.of(
                        "name", "focus_areas",
                        "description", "Areas to focus on during review (e.g., security, performance, maintainability)",
                        "required", false,
                        "type", "array",
                        "default", List.of("code_quality", "best_practices", "security")
                ),
                Map.of(
                        "name", "experience_level",
                        "description", "Target experience level (beginner, intermediate, advanced)",
                        "required", false,
                        "type", "string",
                        "default", "intermediate"
                ),
                Map.of(
                        "name", "include_suggestions",
                        "description", "Whether to include improvement suggestions",
                        "required", false,
                        "type", "boolean",
                        "default", true
                )
        );
    }

    @Override
    protected Object doGetPrompt(JsonNode arguments) throws Exception {
        requireArgument(arguments, "language");

        String language = getStringArgument(arguments, "language");
        String experienceLevel = getStringArgument(arguments, "experience_level", "intermediate");
        boolean includeSuggestions = getBooleanArgument(arguments, "include_suggestions", true);

        List<String> focusAreas = List.of("code_quality", "best_practices", "security");
        if (arguments.has("focus_areas") && arguments.get("focus_areas").isArray()) {
            focusAreas = objectMapper.convertValue(arguments.get("focus_areas"), List.class);
        }

        String prompt = generateCodeReviewPrompt(language, focusAreas, experienceLevel, includeSuggestions);

        return Map.of(
                "name", getName(),
                "description", getDescription(),
                "arguments", Map.of(
                        "language", language,
                        "focus_areas", focusAreas,
                        "experience_level", experienceLevel,
                        "include_suggestions", includeSuggestions
                ),
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", Map.of(
                                        "type", "text",
                                        "text", prompt
                                )
                        )
                ),
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    private String generateCodeReviewPrompt(String language, List<String> focusAreas,
                                          String experienceLevel, boolean includeSuggestions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert ").append(language).append(" code reviewer. ");
        prompt.append("Please conduct a thorough code review with the following guidelines:\n\n");

        prompt.append("**Experience Level**: ").append(experienceLevel).append("\n");
        prompt.append("**Focus Areas**:\n");
        for (String area : focusAreas) {
            prompt.append("- ").append(formatFocusArea(area)).append("\n");
        }

        prompt.append("\n**Review Process**:\n");
        prompt.append("1. **Code Quality**: Assess readability, maintainability, and overall structure\n");
        prompt.append("2. **Best Practices**: Check adherence to ").append(language).append(" conventions and standards\n");
        prompt.append("3. **Security**: Identify potential security vulnerabilities\n");
        prompt.append("4. **Performance**: Evaluate efficiency and optimization opportunities\n");
        prompt.append("5. **Testing**: Review test coverage and quality\n\n");

        if (includeSuggestions) {
            prompt.append("**Output Format**:\n");
            prompt.append("- Provide specific, actionable feedback\n");
            prompt.append("- Include code snippets for improvements where applicable\n");
            prompt.append("- Rate the overall code quality (1-10 scale)\n");
            prompt.append("- Prioritize issues by severity (High/Medium/Low)\n\n");
        }

        prompt.append("Please provide a constructive review that helps improve code quality ");
        prompt.append("while considering the ").append(experienceLevel).append(" experience level.");

        return prompt.toString();
    }

    private String formatFocusArea(String area) {
        return area.replace("_", " ")
                  .substring(0, 1).toUpperCase() +
                  area.replace("_", " ").substring(1);
    }

    @Override
    public Object getMetadata() {
        return Map.of(
                "category", "development",
                "tags", List.of("code-review", "quality-assurance", "development"),
                "languages", List.of("java", "python", "javascript", "typescript", "go", "rust", "c++", "c#"),
                "template_version", "1.0"
        );
    }
}