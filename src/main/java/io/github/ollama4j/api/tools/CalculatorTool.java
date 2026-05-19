package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

public class CalculatorTool implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> args) {
        try {
            String op = (String) args.get("operation");
            double a = ((Number) args.get("a")).doubleValue();
            double b = ((Number) args.get("b")).doubleValue();

            switch (op.toLowerCase()) {
                case "add": return a + b;
                case "subtract": return a - b;
                case "multiply": return a * b;
                case "divide": 
                    if (b == 0) return "Error: Cannot divide by zero";
                    return a / b;
                default:
                    return "Error: Unknown operation " + op;
            }
        } catch (Exception e) {
            return "Error evaluating calculator expression: " + e.getMessage();
        }
    }
}
