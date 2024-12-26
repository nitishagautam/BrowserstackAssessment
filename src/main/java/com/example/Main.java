package com.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            String translatedText = RapidAPITranslator.translate("Hola mundo", "es", "en");
            System.out.println("Translated Text: " + translatedText); // Expected: "Hello world"
        } catch (IOException e) {
            System.err.println("Translation failed: " + e.getMessage());
        }
    }
}