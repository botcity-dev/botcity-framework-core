package dev.botcity.framework.web.browsers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;

public interface BrowserConfig {
    /**
     * Retrieve the default options from the browser curated by BotCity.
     * <p>
     *
     * @param headless           Whether or not to use the headless mode.
     * @param downloadFolderPath The default path in which to save files.
     * @param userDataDir        The directory to use as user profile.
     * @param pageLoadStrategy   The page load strategy.
     * @return The Browser options.
     */
    MutableCapabilities defaultOptions(boolean headless, String downloadFolderPath, String userDataDir, PageLoadStrategy pageLoadStrategy);

    /**
     * Fetch the default capabilities from the browser.
     * <p>
     *
     * @return {@link MutableCapabilities} with the default capabilities defined.
     */
    MutableCapabilities defaultCapabilities();

    /**
     * Wait for all downloads to finish.
     * <b>Important</b>: This method overwrites the current page with the downloads page.
     * <p>
     *
     * @param driver The {@link WebDriver} instance.
     * @return True if all downloads finished, false otherwise.
     */
    Object waitForDownloads(WebDriver driver);

    /**
     * Get driver name from the browser.
     * <p>
     *
     * @return The driver name.
     */
    String getDriverName();

    /**
     * Get driver instance.
     * <p>
     *
     * @return {@link WebDriver} instance.
     */
    WebDriver getWebDriverDriver();

    /**
     * Get browser executor.
     * <p>
     *
     * @param driver The {@link WebDriver} instance.
     * @return {@link HttpCommandExecutor} instance.
     */
    HttpCommandExecutor executor(WebDriver driver);

    /**
     * Get the session id.
     * <p>
     *
     * @return {@link SessionId} instance.
     */
    SessionId getSessionId();
}
