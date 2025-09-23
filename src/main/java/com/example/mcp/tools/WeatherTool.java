package com.example.mcp.tools;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class WeatherTool extends AbstractMcpTool {

    private final Random random = new Random();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WeatherTool() {
        super(
            "weather",
            "Gets current weather information for a specified location (simulated data)",
            createInputSchema()
        );
    }

    private static Map<String, Object> createInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "location", Map.of(
                    "type", "string",
                    "description", "The city or location to get weather for"
                ),
                "units", Map.of(
                    "type", "string",
                    "enum", List.of("celsius", "fahrenheit"),
                    "description", "Temperature units",
                    "default", "celsius"
                )
            ),
            "required", List.of("location")
        );
    }

    @Override
    protected Mono<Void> validateArguments(Map<String, Object> arguments) {
        String location = (String) arguments.get("location");

        if (location == null || location.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Location is required"));
        }

        String units = (String) arguments.get("units");
        if (units != null && !List.of("celsius", "fahrenheit").contains(units)) {
            return Mono.error(new IllegalArgumentException("Invalid units: " + units + ". Must be 'celsius' or 'fahrenheit'"));
        }

        return Mono.empty();
    }

    @Override
    protected Mono<List<Map<String, Object>>> doExecute(Map<String, Object> arguments) {
        return Mono.fromCallable(() -> {
            String location = (String) arguments.get("location");
            String units = (String) arguments.getOrDefault("units", "celsius");

            WeatherData weather = generateWeatherData(location, units);
            String weatherReport = formatWeatherReport(weather);

            return List.of(createTextContent(weatherReport));
        });
    }

    private WeatherData generateWeatherData(String location, String units) {
        String[] conditions = {"sunny", "partly cloudy", "cloudy", "rainy", "stormy", "snowy"};
        String condition = conditions[random.nextInt(conditions.length)];

        // Generate temperature based on units
        double temperature;
        if ("fahrenheit".equals(units)) {
            temperature = 32 + random.nextDouble() * 68; // 32-100°F
        } else {
            temperature = random.nextDouble() * 35; // 0-35°C
        }

        double humidity = 20 + random.nextDouble() * 60; // 20-80%
        double windSpeed = random.nextDouble() * 25; // 0-25 km/h or mph
        double pressure = 980 + random.nextDouble() * 50; // 980-1030 hPa

        return new WeatherData(
            location,
            condition,
            temperature,
            units,
            humidity,
            windSpeed,
            pressure,
            LocalDateTime.now()
        );
    }

    private String formatWeatherReport(WeatherData weather) {
        String tempUnit = "celsius".equals(weather.units) ? "°C" : "°F";
        String windUnit = "celsius".equals(weather.units) ? "km/h" : "mph";

        return String.format("""
            🌤️ Weather Report for %s

            📍 Location: %s
            🌡️  Temperature: %.1f%s
            ☁️  Condition: %s
            💧 Humidity: %.1f%%
            💨 Wind Speed: %.1f %s
            📊 Pressure: %.1f hPa
            🕐 Last Updated: %s

            Note: This is simulated weather data for demonstration purposes.
            """,
            weather.location,
            weather.location,
            weather.temperature, tempUnit,
            weather.condition,
            weather.humidity,
            weather.windSpeed, windUnit,
            weather.pressure,
            weather.timestamp.format(formatter)
        );
    }

    private static class WeatherData {
        final String location;
        final String condition;
        final double temperature;
        final String units;
        final double humidity;
        final double windSpeed;
        final double pressure;
        final LocalDateTime timestamp;

        WeatherData(String location, String condition, double temperature, String units,
                   double humidity, double windSpeed, double pressure, LocalDateTime timestamp) {
            this.location = location;
            this.condition = condition;
            this.temperature = temperature;
            this.units = units;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.pressure = pressure;
            this.timestamp = timestamp;
        }
    }
}