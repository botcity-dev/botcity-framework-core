package dev.botcity.framework.web.browsers;

import lombok.*;

import com.google.gson.Gson;

import com.microsoft.edge.seleniumtools.EdgeOptions;

import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.util.Collections;

@Data
public class EdgeConfig implements BrowserConfig {

    private WebDriver driver;

    private Gson gson = new Gson();

    @Override
    public WebDriver getWebDriverDriver() {
        return this.driver;
    }

    @SneakyThrows
    @Override
    public MutableCapabilities defaultOptions(boolean headless, String downloadFolderPath, String userDataDir, PageLoadStrategy pageLoadStrategy) {
        EdgeOptions options = new EdgeOptions();
        
        switch(pageLoadStrategy) {
	    	case EAGER:
	    		options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);
	    		break;
	    	case NONE:
	    		options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.NONE);
	    		break;
	    	case NORMAL:
        		options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.NORMAL);
        		break;
        }
        
        options.addArguments("--remote-debugging-port=0");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-client-side-phishing-detection");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-hang-monitor");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-prompt-on-repost");
        options.addArguments("--disable-syncdisable-translate");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--safebrowsing-disable-auto-update");

        options.addArguments("--disable-blink-features=AutomationControlled");

        // Disable banner for Browser being remote-controlled
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        if (headless) {
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--hide-scrollbars");
            options.addArguments("--mute-audio");
        }
        
        // Check if user is root
        try {
			String user = System.getProperty("user.name");
			if(user.equals("root")) {
				options.addArguments("--no-sandbox");
			}
		} catch (Exception e) {}

        if (userDataDir == null || userDataDir.isEmpty()) {
            File tempDirectory = Files.createTempDirectory("botcity_").toFile();
            tempDirectory.deleteOnExit();
            userDataDir = tempDirectory.getAbsolutePath();
        }

        options.addArguments("--user-data-dir=" + userDataDir);

        if (downloadFolderPath.isEmpty()) {
            downloadFolderPath = System.getProperty("user.dir");
        }

        Map<String, String> recentDestinations = new HashMap<>();
        recentDestinations.put("id", "Save as PDF");
        recentDestinations.put("origin", "local");
        recentDestinations.put("account", "");

        Map<String, Object> appState = new HashMap<>();
        appState.put("recentDestinations", Collections.singletonList(recentDestinations));
        appState.put("selectedDestinationId", "Save as PDF");
        appState.put("version", 2);
        appState.put("isHeaderFooterEnabled", false);
        appState.put("marginsType", 2);
        appState.put("isCssBackgroundEnabled", true);

        Map<String, Object> edgePrefs = new HashMap<>();
        edgePrefs.put("printing.print_preview_sticky_settings.appState", this.gson.toJson(appState));
        edgePrefs.put("download.default_directory", downloadFolderPath);
        edgePrefs.put("savefile.default_directory", downloadFolderPath);
        edgePrefs.put("safebrowsing.enabled", true);
        edgePrefs.put("credentials_enable_service", false);
        edgePrefs.put("profile.password_manager_enabled", false);
        edgePrefs.put("plugins.always_open_pdf_externally", true);

        Map<String, String> selectionRules = new HashMap<>();
        selectionRules.put("kind", "local");
        selectionRules.put("namePattern", "Save as PDF");
        edgePrefs.put("printing.default_destination_selection_rules", selectionRules);

        options.setExperimentalOption("prefs", edgePrefs);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");

        options.addArguments("--kiosk-printing");

        System.setProperty("webdriver.edge.silentOutput", "true");

        return options;
    }
    
    public MutableCapabilities defaultOptions(boolean headless, String downloadFolderPath, String userDataDir) {
    	return defaultOptions(headless, downloadFolderPath, userDataDir, PageLoadStrategy.NORMAL);
    }

    @Override
    public MutableCapabilities defaultCapabilities() {
        return new EdgeOptions();
    }

    @SneakyThrows
    @Override
    public Object waitForDownloads(WebDriver driver) {
        if (!driver.getCurrentUrl().startsWith("edge://downloads")) {
            driver.get("edge://downloads/");
            Thread.sleep(1000);
        }
        return ((JavascriptExecutor) driver).executeScript("var items = Array.from(document.querySelector(\".downloads-list\").querySelectorAll('[role=\"listitem\"]')); if(items.every(e => e.querySelector('[role=\"progressbar\"]') == null))return true;");
    }

    @Override
    public String getDriverName() {
        return "msedgedriver";
    }

    @Override
    public HttpCommandExecutor executor(WebDriver driver) {
        return (HttpCommandExecutor) ((com.microsoft.edge.seleniumtools.EdgeDriver) driver).getCommandExecutor();
    }

    @Override
    public SessionId getSessionId() {
        return ((com.microsoft.edge.seleniumtools.EdgeDriver) driver).getSessionId();
    }
}
