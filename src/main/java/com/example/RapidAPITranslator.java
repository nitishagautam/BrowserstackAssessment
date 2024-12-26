package com.example;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

public class RapidAPITranslator {
    private static final String API_URL = "https://google-translator9.p.rapidapi.com/v2";
    private static final String API_KEY = "YOUR_RAPIDAPI_KEY"; // Replace with your actual RapidAPI key

    public static String translate(String text, String from, String to) throws IOException {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text to translate cannot be null or empty.");
        }

        // Create JSON payload
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("q", text);
        jsonBody.put("source", from);
        jsonBody.put("target", to);

        // Create HTTP client
        OkHttpClient client = new OkHttpClient();

        // Build HTTP request
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(jsonBody.toString(), MediaType.get("application/json")))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RapidAPI-Key", "de51578e02msh8347da58e529326p175632jsnbb7a5c555528")
                .addHeader("X-RapidAPI-Host", "google-translator9.p.rapidapi.com")
                .build();

        // Execute the request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + " - " + response.body().string());
            }

            // Parse JSON response
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
        }
    }
}