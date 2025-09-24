package com.example.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CalculatorTool extends AbstractMcpTool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Perform basic mathematical calculations (addition, subtraction, multiplication, division)";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode operation = objectMapper.createObjectNode();
        operation.put("type", "string");
        operation.put("description", "Mathematical operation to perform");
        operation.set("enum", objectMapper.valueToTree(new String[]{"add", "subtract", "multiply", "divide"}));
        properties.set("operation", operation);

        ObjectNode a = objectMapper.createObjectNode();
        a.put("type", "number");
        a.put("description", "First operand");
        properties.set("a", a);

        ObjectNode b = objectMapper.createObjectNode();
        b.put("type", "number");
        b.put("description", "Second operand");
        properties.set("b", b);

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(new String[]{"operation", "a", "b"}));

        return schema;
    }

    @Override
    protected Object doExecute(JsonNode parameters) throws Exception {
        requireParameter(parameters, "operation");
        requireParameter(parameters, "a");
        requireParameter(parameters, "b");

        String operation = getStringParameter(parameters, "operation");
        double a = getDoubleParameter(parameters, "a");
        double b = getDoubleParameter(parameters, "b");

        double result;
        switch (operation.toLowerCase()) {
            case "add":
                result = a + b;
                break;
            case "subtract":
                result = a - b;
                break;
            case "multiply":
                result = a * b;
                break;
            case "divide":
                if (b == 0) {
                    throw new IllegalArgumentException("Division by zero is not allowed");
                }
                result = a / b;
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }

        logger.info("Calculator: {} {} {} = {}", a, operation, b, result);

        return Map.of(
                "operation", operation,
                "operands", Map.of("a", a, "b", b),
                "result", result,
                "expression", String.format("%.2f %s %.2f = %.2f", a, getOperatorSymbol(operation), b, result)
        );
    }

    private String getOperatorSymbol(String operation) {
        return switch (operation.toLowerCase()) {
            case "add" -> "+";
            case "subtract" -> "-";
            case "multiply" -> "*";
            case "divide" -> "/";
            default -> operation;
        };
    }
}