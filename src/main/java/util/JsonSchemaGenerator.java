package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class JsonSchemaGenerator {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Simplistic JSON schema generator for demonstration.
     * In a full implementation, you'd use a robust JSON schema library (e.g., victools/jsonschema-generator).
     */
    public static String generateSchemaForProperties(Map<String, Object> properties, String[] required) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && required.length > 0) {
            schema.put("required", required);
        }
        
        try {
            return mapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate JSON schema", e);
        }
    }
}
