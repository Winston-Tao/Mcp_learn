package com.example.mcp.tools;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class CalculatorTool extends AbstractMcpTool {

    public CalculatorTool() {
        super(
            "calculator",
            "Performs basic mathematical calculations",
            createInputSchema()
        );
    }

    private static Map<String, Object> createInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "operation", Map.of(
                    "type", "string",
                    "enum", List.of("add", "subtract", "multiply", "divide"),
                    "description", "The mathematical operation to perform"
                ),
                "a", Map.of(
                    "type", "number",
                    "description", "First operand"
                ),
                "b", Map.of(
                    "type", "number",
                    "description", "Second operand"
                )
            ),
            "required", List.of("operation", "a", "b")
        );
    }

    @Override
    protected Mono<Void> validateArguments(Map<String, Object> arguments) {
        String operation = (String) arguments.get("operation");
        Object a = arguments.get("a");
        Object b = arguments.get("b");

        if (operation == null) {
            return Mono.error(new IllegalArgumentException("Missing operation"));
        }

        if (a == null || b == null) {
            return Mono.error(new IllegalArgumentException("Missing operands"));
        }

        if (!List.of("add", "subtract", "multiply", "divide").contains(operation)) {
            return Mono.error(new IllegalArgumentException("Invalid operation: " + operation));
        }

        if ("divide".equals(operation) && getDoubleValue(b) == 0.0) {
            return Mono.error(new IllegalArgumentException("Division by zero"));
        }

        return Mono.empty();
    }

    @Override
    protected Mono<List<Map<String, Object>>> doExecute(Map<String, Object> arguments) {
        return Mono.fromCallable(() -> {
            String operation = (String) arguments.get("operation");
            double a = getDoubleValue(arguments.get("a"));
            double b = getDoubleValue(arguments.get("b"));

            double result = switch (operation) {
                case "add" -> a + b;
                case "subtract" -> a - b;
                case "multiply" -> a * b;
                case "divide" -> a / b;
                default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
            };

            String resultText = String.format("%.2f %s %.2f = %.2f", a, getOperatorSymbol(operation), b, result);
            return List.of(createTextContent(resultText));
        });
    }

    private double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format: " + value);
            }
        }
        throw new IllegalArgumentException("Invalid number type: " + value.getClass());
    }

    private String getOperatorSymbol(String operation) {
        return switch (operation) {
            case "add" -> "+";
            case "subtract" -> "-";
            case "multiply" -> "*";
            case "divide" -> "/";
            default -> operation;
        };
    }
}