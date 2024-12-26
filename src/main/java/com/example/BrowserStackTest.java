package com.example;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.MutableCapabilities;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserStackTest {
    public static final String USERNAME = "nitishagautam_ViOuSY";
    public static final String ACCESS_KEY = "JbencoDurqXdBkmk9T6H";
    public static final String URL = "https://" + USERNAME + ":" + ACCESS_KEY + "@hub.browserstack.com/wd/hub";

    @Test
    @Parameters({"os", "os_version", "browser", "browser_version", "device", "realMobile"})
    public void test(String os, String osVersion, String browser, String browserVersion, String device, boolean realMobile) throws Exception {
        // W3C-compliant capabilities
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("os", os);
        browserstackOptions.put("osVersion", osVersion);
        browserstackOptions.put("browserName", browser);
        browserstackOptions.put("browserVersion", browserVersion);
        browserstackOptions.put("sessionName", "ElPaisScraper Test");
        browserstackOptions.put("consoleLogs", "verbose");
        browserstackOptions.put("networkLogs", true);

        if (realMobile) {
            browserstackOptions.put("deviceName", device);
            browserstackOptions.put("realMobile", true);
        }

        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("bstack:options", browserstackOptions);

        WebDriver driver = new RemoteWebDriver(new URL(URL), caps);

        try {
            // Retry logic for network interruptions
            retry(() -> driver.get("https://elpais.com/"), 3);

            // Reuse existing methods from ElPaisScraper
            ElPaisScraper.handleCookieConsent(driver);
            List<Article> articles = ElPaisScraper.scrapeOpinionSection(driver);
            List<String> translatedTitles = ElPaisScraper.translateTitles(articles);
            ElPaisScraper.analyzeHeaders(translatedTitles);
        } finally {
            driver.quit();
        }
    }

    private static void retry(Runnable action, int maxRetries) {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                action.run();
                return; // Exit if successful
            } catch (Exception e) {
                retries++;
                System.out.println("Retry " + retries + " due to: " + e.getMessage());
                if (retries == maxRetries) {
                    throw e; // Rethrow exception after all retries fail
                }
            }
        }
    }
}
