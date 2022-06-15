package dev.botcity.framework.web;

import com.microsoft.edge.seleniumtools.EdgeDriver;
import com.microsoft.edge.seleniumtools.EdgeOptions;
import dev.botcity.framework.web.browsers.*;

import org.openqa.selenium.MutableCapabilities;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class WebDriverFactory {

    /**
     * Factory method to create a Browser config instance.
     * <p>
     *
     * @param browser      the target browser.
     * @param headless     true to run the browser in headless mode.
     * @param options      the browser options.
     * @param capabilities the browser capabilities.
     * @param driverPath   the path to the driver.
     * @param downloadPath the path to the download folder.
     * @return the Browser config instance.
     */
    public static BrowserConfig getWebDriver(Browser browser, boolean headless, MutableCapabilities options, MutableCapabilities capabilities, String driverPath, String downloadPath) {
        switch (browser) {
            case FIREFOX:
                return getFirefox(headless, options, capabilities, driverPath, downloadPath);
            case CHROME:
                return getChrome(headless, options, capabilities, driverPath, downloadPath);
            case EDGE:
                return getEdge(headless, options, capabilities, driverPath, downloadPath);
        }
        throw new RuntimeException("Browser not supported: " + browser);
    }

    /**
     * Returns Firefox Browser config instance.
     * <p>
     *
     * @param headless     true to run the browser in headless mode.
     * @param options      the browser options.
     * @param capabilities the browser capabilities.
     * @param driverPath   the path to the driver.
     * @param downloadPath the path to the download folder.
     * @return the Firefox Browser config instance.
     */
    private static BrowserConfig getFirefox(boolean headless, MutableCapabilities options, MutableCapabilities capabilities, String driverPath, String downloadPath) {
        FirefoxConfig firefoxConfig = new FirefoxConfig();

        MutableCapabilities firefoxOptions;
        if (options == null) {
            firefoxOptions = firefoxConfig.defaultOptions(headless, downloadPath, null);
        } else {
            firefoxOptions = options;
        }

        MutableCapabilities firefoxCapabilities = capabilities;
        if (capabilities == null) {
            firefoxCapabilities = firefoxConfig.defaultCapabilities();
        }

        if (!driverPath.isEmpty()) {
            System.setProperty("webdriver.gecko.driver", driverPath);
        }

        firefoxCapabilities.asMap().forEach((key, value) -> {
            if (value != null) {
                firefoxOptions.setCapability(key, value);
            }
        });

        firefoxConfig.setDriver(new FirefoxDriver((FirefoxOptions) firefoxOptions));
        return firefoxConfig;
    }

    /**
     * Returns Chrome Browser config instance.
     * <p>
     *
     * @param headless     true to run the browser in headless mode.
     * @param options      the browser options.
     * @param capabilities the browser capabilities.
     * @param driverPath   the path to the driver.
     * @param downloadPath the path to the download folder.
     * @return the Chrome Browser config instance.
     */
    private static BrowserConfig getChrome(boolean headless, MutableCapabilities options, MutableCapabilities capabilities, String driverPath, String downloadPath) {
        ChromeConfig chromeConfig = new ChromeConfig();

        MutableCapabilities chromeOptions;
        if (options == null) {
            chromeOptions = chromeConfig.defaultOptions(headless, downloadPath, null);
        } else {
            chromeOptions = options;
        }

        MutableCapabilities chromeCapabilities = capabilities;
        if (capabilities == null) {
            chromeCapabilities = chromeConfig.defaultCapabilities();
        }

        if (!driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
        }

        chromeCapabilities.asMap().forEach((key, value) -> {
            if (value != null) {
                chromeOptions.setCapability(key, value);
            }
        });

        chromeConfig.setDriver(new ChromeDriver((ChromeOptions) chromeOptions));
        return chromeConfig;
    }

    /**
     * Returns Edge Browser config instance.
     * <p>
     *
     * @param headless     true to run the browser in headless mode.
     * @param options      the browser options.
     * @param capabilities the browser capabilities.
     * @param driverPath   the path to the driver.
     * @param downloadPath the path to the download folder.
     * @return the Edge Browser config instance.
     */
    private static BrowserConfig getEdge(boolean headless, MutableCapabilities options, MutableCapabilities capabilities, String driverPath, String downloadPath) {
        EdgeConfig edgeConfig = new EdgeConfig();

        MutableCapabilities edgeOptions;
        if (options == null) {
            edgeOptions = edgeConfig.defaultOptions(headless, downloadPath, null);
        } else {
            edgeOptions = options;
        }

        MutableCapabilities edgeCapabilities = capabilities;
        if (capabilities == null) {
            edgeCapabilities = edgeConfig.defaultCapabilities();
        }

        if (!driverPath.isEmpty()) {
            System.setProperty("webdriver.edge.driver", driverPath);
        }

        edgeCapabilities.asMap().forEach((key, value) -> {
            if (value != null) {
                edgeOptions.setCapability(key, value);
            }
        });

        edgeConfig.setDriver(new EdgeDriver((EdgeOptions) edgeOptions));
        return edgeConfig;
    }
}
