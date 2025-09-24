package com.example.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Component
public class WeatherTool extends AbstractMcpTool {

    private final Random random = new Random();

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return "Get current weather information for a specified location (mock data for demonstration)";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode location = objectMapper.createObjectNode();
        location.put("type", "string");
        location.put("description", "Location to get weather for (city, country)");
        properties.set("location", location);

        ObjectNode units = objectMapper.createObjectNode();
        units.put("type", "string");
        units.put("description", "Temperature units (celsius, fahrenheit)");
        units.put("default", "celsius");
        units.set("enum", objectMapper.valueToTree(new String[]{"celsius", "fahrenheit"}));
        properties.set("units", units);

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(new String[]{"location"}));

        return schema;
    }

    @Override
    protected Object doExecute(JsonNode parameters) throws Exception {
        requireParameter(parameters, "location");

        String location = getStringParameter(parameters, "location");
        String units = getStringParameter(parameters, "units", "celsius");

        if (location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }

        logger.info("Getting weather for location: {} in {}", location, units);

        WeatherData weatherData = generateMockWeatherData(location, units);

        return Map.of(
                "location", location,
                "units", units,
                "current", Map.of(
                        "temperature", weatherData.temperature,
                        "condition", weatherData.condition,
                        "humidity", weatherData.humidity,
                        "windSpeed", weatherData.windSpeed,
                        "pressure", weatherData.pressure
                ),
                "forecast", Map.of(
                        "summary", weatherData.forecastSummary
                ),
                "timestamp", weatherData.timestamp,
                "source", "Mock Weather Service"
        );
    }

    private WeatherData generateMockWeatherData(String location, String units) {
        WeatherData data = new WeatherData();

        String[] conditions = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Snowy", "Stormy"};
        String[] forecastSummaries = {
                "Clear skies expected for the next few days",
                "Partly cloudy with occasional sunshine",
                "Overcast conditions with light precipitation possible",
                "Rain expected throughout the week",
                "Cold weather with possible snow",
                "Stormy weather pattern moving through the area"
        };

        data.condition = conditions[random.nextInt(conditions.length)];
        data.forecastSummary = forecastSummaries[random.nextInt(forecastSummaries.length)];

        if ("fahrenheit".equals(units)) {
            data.temperature = 32 + (random.nextInt(80));
        } else {
            data.temperature = -10 + random.nextInt(35);
        }

        data.humidity = 30 + random.nextInt(60);
        data.windSpeed = random.nextInt(25);
        data.pressure = 980 + random.nextInt(60);
        data.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return data;
    }

    private static class WeatherData {
        int temperature;
        String condition;
        int humidity;
        int windSpeed;
        int pressure;
        String forecastSummary;
        String timestamp;
    }
}