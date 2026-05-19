package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherTool implements ToolFunction {

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public Object apply(Map<String, Object> arguments) {

        try {

            // Validate input
            if (arguments == null || !arguments.containsKey("location")) {
                return "Error: location is required";
            }

            Object locationObj = arguments.get("location");

            if (!(locationObj instanceof String location) || location.isBlank()) {
                return "Error: invalid location";
            }

            // Step 1: Get coordinates from Open-Meteo Geocoding API
            String geoUrl =
                    "https://geocoding-api.open-meteo.com/v1/search?name="
                            + URLEncoder.encode(location, StandardCharsets.UTF_8)
                            + "&count=1&language=en&format=json";

            HttpRequest geoRequest = HttpRequest.newBuilder()
                    .uri(URI.create(geoUrl))
                    .GET()
                    .build();

            HttpResponse<String> geoResponse = client.send(
                    geoRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            JSONObject geoJson = new JSONObject(geoResponse.body());

            if (!geoJson.has("results")) {
                return "Location not found: " + location;
            }

            JSONArray results = geoJson.getJSONArray("results");

            if (results.isEmpty()) {
                return "Location not found: " + location;
            }

            JSONObject place = results.getJSONObject(0);

            double latitude = place.getDouble("latitude");
            double longitude = place.getDouble("longitude");
            String resolvedName = place.getString("name");
            String country = place.optString("country", "");

            // Step 2: Fetch weather from Open-Meteo Forecast API
            String weatherUrl =
                    "https://api.open-meteo.com/v1/forecast"
                            + "?latitude=" + latitude
                            + "&longitude=" + longitude
                            + "&current_weather=true";

            HttpRequest weatherRequest = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .GET()
                    .build();

            HttpResponse<String> weatherResponse = client.send(
                    weatherRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            JSONObject weatherJson = new JSONObject(weatherResponse.body());

            if (!weatherJson.has("current_weather")) {
                return "Weather data unavailable for " + resolvedName;
            }

            JSONObject current = weatherJson.getJSONObject("current_weather");

            double temperature = current.getDouble("temperature");
            double windspeed = current.getDouble("windspeed");
            int weatherCode = current.getInt("weathercode");

            return """
                    Weather for %s, %s
                    Latitude: %.4f
                    Longitude: %.4f

                    Temperature: %.1f°C
                    Wind Speed: %.1f km/h
                    Weather Code: %d
                    """.formatted(
                    resolvedName,
                    country,
                    latitude,
                    longitude,
                    temperature,
                    windspeed,
                    weatherCode
            );

        } catch (IOException | InterruptedException e) {
            return "Error fetching weather data: " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }
}