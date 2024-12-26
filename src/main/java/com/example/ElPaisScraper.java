package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElPaisScraper {

    private static final String IMAGE_DIR = "images";
    private static boolean isCookieAccepted = false;

    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\gauta\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        File imageDir = new File(IMAGE_DIR);
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        WebDriver driver = new ChromeDriver();

        try {
            driver.manage().window().maximize(); // Ensure proper visibility
            driver.get("https://elpais.com/");
            handleCookieConsent(driver);
            List<Article> articles = scrapeOpinionSection(driver);
            List<String> translatedTitles = translateTitles(articles);
            analyzeHeaders(translatedTitles);
        } finally {
            driver.quit();
        }
    }

    public static void handleCookieConsent(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            // Wait for the page to fully load
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            System.out.println("Page fully loaded.");

            // Wait for the cookie dialog to be present and visible
            WebElement cookieDialog = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div#cookie-banner, div[role='dialog'], div[class*='cookie'], div.didomi-notice__interior-border")));
            System.out.println("Cookie consent dialog detected.");

            // Locate and click the accept button
            WebElement acceptButton = cookieDialog.findElement(
            		By.xpath("//button[@id='didomi-notice-agree-button']"));

            // Click using JavaScript to handle overlay issues
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptButton);
            System.out.println("Cookie consent accepted via JavaScript.");

            // Wait for the dialog to disappear
            wait.until(ExpectedConditions.invisibilityOf(cookieDialog));
            System.out.println("Cookie dialog successfully dismissed.");
        } catch (Exception e) {
            System.out.println("Failed to handle cookie consent dialog: " + e.getMessage());

            // Fallback: Remove the cookie dialog using JavaScript if retries fail
            try {
                System.out.println("Fallback: Removing cookie consent dialog via JavaScript.");
                ((JavascriptExecutor) driver).executeScript(
                        "document.querySelectorAll('div#cookie-banner, div[role=\"dialog\"], div[class*=\"cookie\"], div.didomi-notice__interior-border')"
                        + ".forEach(e => e.remove());");
                Thread.sleep(2000); // Allow page to stabilize
            } catch (Exception jsException) {
                System.out.println("Fallback failed: Unable to remove cookie dialog.");
            }
        }
    }

    public static List<Article> scrapeOpinionSection(WebDriver driver) throws IOException, InterruptedException {
        List<Article> articles = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Navigate to Opinion section
        WebElement opinionLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Opinión")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", opinionLink);
        Thread.sleep(3000);

        // Wait for articles to load
        List<WebElement> articleElements = driver.findElements(By.cssSelector("article"));
        System.out.println("Total articles found: " + articleElements.size());

        // Process first 5 articles
        for (int i = 0; i < Math.min(5, articleElements.size()); i++) {
            WebElement articleElement = articleElements.get(i);
            try {
                String title = articleElement.findElement(By.tagName("h2")).getText();
                String content = articleElement.findElement(By.cssSelector("p")).getText();
                System.out.println("Processing article " + (i + 1) + ": " + title);

                // Extract image
                String imageUrl = "";
                try {
                    WebElement imgElement = articleElement.findElement(By.tagName("img"));
                    imageUrl = imgElement.getAttribute("src");
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        imageUrl = imgElement.getAttribute("data-src");
                    }

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        System.out.println("Image found for article: " + title + " | Image URL: " + imageUrl);
                        saveImage(imageUrl, IMAGE_DIR + "/" + title.replaceAll("\\s+", "_") + ".jpg");
                    } else {
                        System.out.println("No valid image URL found for article: " + title);
                    }
                } catch (Exception e) {
                    System.out.println("No image available for this article: " + title);
                }

                articles.add(new Article(title, content, imageUrl));
            } catch (Exception e) {
                System.out.println("Skipping article " + (i + 1) + " due to incomplete data.");
            }
        }

        return articles;
    }



    private static void saveImage(String imageUrl, String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            byte[] imageBytes = new URL(imageUrl).openStream().readAllBytes();
            fos.write(imageBytes);
        }
    }

    public static List<String> translateTitles(List<Article> articles) throws IOException {
        List<String> translatedTitles = new ArrayList<>();
        for (Article article : articles) {
            String translatedText = RapidAPITranslator.translate(article.title, "es", "en");
            System.out.println("Original Title: " + article.title);
            System.out.println("Translated Title: " + translatedText);
            translatedTitles.add(translatedText);
        }
        return translatedTitles;
    }

    static void analyzeHeaders(List<String> translatedTitles) {
        Map<String, Integer> wordCounts = new ConcurrentHashMap<>();
        for (String title : translatedTitles) {
            String[] words = title.toLowerCase().split("\\W+");
            for (String word : words) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }
        System.out.println("\nRepeated Words:");
        wordCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 2)
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
    }
}