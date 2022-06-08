package dev.botcity.framework.web.browsers;

import lombok.*;

import com.google.gson.Gson;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.util.Collections;

@Data
public class ChromeConfig implements BrowserConfig {
    private WebDriver driver;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Gson gson = new Gson();

    @SneakyThrows
    @Override
    public MutableCapabilities defaultOptions(boolean headless, String downloadFolderPath, String userDataDir) {
        ChromeOptions options = new ChromeOptions();
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

        // Disable What's New banner for new chrome installs
        options.addArguments("--disable-features=ChromeWhatsNewUI");
        options.addArguments("--disable-blink-features=AutomationControlled");

        // Disable banner for Browser being remote - controlled
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        if (headless) {
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--hide-scrollbars");
            options.addArguments("--mute-audio");
        }

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

        Map<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("printing.print_preview_sticky_settings.appState", this.gson.toJson(appState));
        chromePrefs.put("download.default_directory", downloadFolderPath);
        chromePrefs.put("savefile.default_directory", downloadFolderPath);
        chromePrefs.put("safebrowsing.enabled", true);
        chromePrefs.put("credentials_enable_service", false);
        chromePrefs.put("profile.password_manager_enabled", false);
        chromePrefs.put("plugins.always_open_pdf_externally", true);

        Map<String, String> selectionRules = new HashMap<>();
        selectionRules.put("kind", "local");
        selectionRules.put("namePattern", "Save as PDF");
        chromePrefs.put("printing.default_destination_selection_rules", selectionRules);

        options.setExperimentalOption("prefs", chromePrefs);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");

        options.addArguments("--kiosk-printing");

        return options;
    }

    @Override
    public DesiredCapabilities defaultCapabilities() {
        return DesiredCapabilities.chrome();
    }

    @Override
    public Object waitForDownloads(WebDriver driver) {
        if (!driver.getCurrentUrl().startsWith("chrome://downloads")) {
            driver.get("chrome://downloads/");
        }
        return ((JavascriptExecutor) driver).executeScript("var items = document.querySelector('downloads-manager').shadowRoot.getElementById('downloadsList').items; if (items.every(e => e.state === \"COMPLETE\")) return items.map(e => e.fileUrl || e.file_url);");
    }

    @Override
    public String getDriverName() {
        return "chromedriver";
    }

    @Override
    public WebDriver getWebDriverDriver() {
        return this.driver;
    }

    @Override
    public HttpCommandExecutor executor(WebDriver driver) {
        return (HttpCommandExecutor) ((ChromeDriver) driver).getCommandExecutor();
    }

    @Override
    public SessionId getSessionId() {
        return ((ChromeDriver) this.driver).getSessionId();
    }
}
