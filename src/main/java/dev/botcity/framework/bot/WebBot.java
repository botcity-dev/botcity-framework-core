package dev.botcity.framework.bot;

import lombok.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;

import dev.botcity.framework.web.*;
import dev.botcity.framework.web.browsers.BrowserConfig;
import dev.botcity.framework.web.exceptions.ElementNotAvailableException;
import dev.botcity.framework.web.browsers.Browser;

import org.marvinproject.framework.image.MarvinImage;
import org.marvinproject.framework.io.MarvinImageIO;
import org.marvinproject.plugins.collection.MarvinPluginCollection;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;

/**
 * Base class for Web Bots.
 * Users must implement the `action` method in their classes.
 */
@Data
public class WebBot {
    private String driverPath = "";
    private boolean headless = false;
    private MutableCapabilities options;
    private Browser browser = Browser.CHROME;
    private MutableCapabilities capabilities;
    private String downloadPath = System.getProperty("user.dir");
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Setter(AccessLevel.NONE)
    private WebDriver driver;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private State element = new State();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String clipboard = "";

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private int[] DIMENSIONS = {1600, 900};

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final int DEFAULT_SLEEP_AFTER_ACTION = 300;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private int x = 0;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private int y = 0;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private double colorSensibility = 0.04;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, String> images = new HashMap<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BrowserConfig config;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private CVFind cvFind = new CVFind();

    /**
     * Resolve relative path to absolute path.
     * <p>
     *
     * @param path Relative path.
     * @return Returns {@link File} instance.
     */
    private File resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty.");
        }

        if (path.startsWith("~")) {
            path = Paths.get(System.getProperty("user.home"), path.substring(1))
                    .normalize()
                    .toString();
        }

        return new File(path).getAbsoluteFile();
    }

    public void setDriverPath(String path) {
        File filepath = resolvePath(path);
        if (!filepath.exists()) {
            throw new IllegalArgumentException("WebDriver does not exist: " + path);
        }
        this.driverPath = filepath.getAbsolutePath();
    }

    public void setDownloadPath(String path) {
        File filepath = resolvePath(path);
        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        this.downloadPath = filepath.getAbsolutePath();
    }

    /**
     * Starts the selected browser.
     */
    public void startBrowser() {
        this.config = WebDriverFactory.getWebDriver(this.browser, this.headless, this.options, this.capabilities, this.driverPath, this.downloadPath);
        this.driver = this.config.getWebDriverDriver();
        setScreenResolution();
    }

    /**
     * Stops the Chrome browser and clean up the User Data Directory.
     * <p>
     * <b>Warning</b>:
     * <p>
     * After invoking this method, you will need to reassign your custom options and capabilities.
     */
    public void stopBrowser() {
        if (this.driver == null) return;

        try {
            this.driver.close();
            this.driver.quit();
        } catch (Exception ignored) {
        } finally {
            this.options = null;
            this.capabilities = null;
            this.driver = null;
        }
    }

    /**
     * The WebDriver driver instance.
     *
     * @return The {@link org.openqa.selenium.WebDriver} driver instance
     */
    public WebDriver getDriver() {
        return this.driver;
    }

    /**
     * Configures the browser dimensions.
     * <p>
     *
     * @param width  The desired width.
     * @param height The desired height.
     */
    public void setScreenResolution(int width, int height) {
        Dimension dimension = new Dimension(width, height);
        if (!this.headless) {
            Dimension viewportSize = getViewportSize();
            Dimension pageSize = getPageSize();
            dimension = new Dimension((viewportSize.getWidth() - pageSize.getWidth()) + width, (viewportSize.getHeight() - pageSize.getHeight()) + height);
        }
        this.driver.manage().window().setSize(dimension);
    }

    /**
     * Configures the browser dimensions with initial value (1600, 900).
     */
    private void setScreenResolution() {
        setScreenResolution(this.DIMENSIONS[0], this.DIMENSIONS[1]);
    }

    /**
     * Shortcut to maximize window on Windows OS.
     */
    public void maximizeWindow() {
        this.driver.manage().window().maximize();
    }

    /**
     * Capture and returns a screenshot from the browser.
     * <p>
     *
     * @param region a {@link Region} instance with the left, top, width and height to crop the screen image.
     * @return The screenshot {@link MarvinImage} object.
     */
    @SneakyThrows
    public MarvinImage getScreenImage(Region region) {
        byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        MarvinImage screenshot = new MarvinImage(ImageIO.read(bis));

        if (region != null) {
            MarvinPluginCollection.crop(screenshot.clone(), screenshot, 60, 32, 182, 62);
            return screenshot;
        }

        return screenshot;
    }

    /**
     * Capture and returns a screenshot from the browser.
     * <p>
     *
     * @return The screenshot {@link MarvinImage} object.
     */
    public MarvinImage getScreenImage() {
        return getScreenImage(null);
    }

    /**
     * Capture a screenshot.
     * <p>
     *
     * @return The screenshot {@link MarvinImage} object.
     */
    public MarvinImage getScreenshot() {
        return getScreenImage();
    }

    /**
     * Saves a screenshot in a given path.
     * <p>
     *
     * @param output the filepath in which to save the screenshot.
     * @return The filepath in which to save the screenshot.
     */
    @SneakyThrows
    public String saveScreenshot(String output) {
        MarvinImage image = getScreenImage();
        MarvinImageIO.saveImage(image, output);
        return output;
    }

    /**
     * Capture a screenshot.
     * <p>
     *
     * @param output the filepath in which to save the screenshot.
     * @return The screenshot {@link MarvinImage} object.
     */
    public MarvinImage screenshot(String output) {
        MarvinImage image = getScreenImage();
        MarvinImageIO.saveImage(image, output);
        return image;
    }

    /**
     * Capture a screenshot from a region of the screen.
     * <p>
     *
     * @param region a {@link Region} instance with the left, top, width and height.
     * @return The screenshot {@link MarvinImage} object.
     */
    public MarvinImage screenCut(Region region) {
        Dimension screensize = getPageSize();
        int width = region.getWidth() != 0 ? region.getWidth() : screensize.width;
        int height = region.getHeight() != 0 ? region.getHeight() : screensize.height;

        MarvinImage imageOut = new MarvinImage();
        MarvinPluginCollection.crop(getScreenshot(), imageOut, region.getX(), region.getY(), width, height);
        return imageOut;
    }

    /**
     * Returns the browser current page size.
     * <p>
     *
     * @return The {@link org.openqa.selenium.Dimension} instance with page size.
     */
    public Dimension getPageSize() {
        Long width = (Long) this.executeJavascript("return parseInt(window.innerWidth)");
        Long height = (Long) this.executeJavascript("return parseInt(window.innerHeight)");
        return new Dimension(width.intValue(), height.intValue());
    }

    /**
     * Returns the display size in pixels.
     * <p>
     *
     * @return The {@link org.openqa.selenium.Dimension} instance with display size.
     */
    public Dimension displaySize() {
        return getPageSize();
    }

    /**
     * Returns the browser current viewport size.
     * <p>
     *
     * @return The {@link org.openqa.selenium.Dimension} instance with viewport size.
     */
    public Dimension getViewportSize() {
        int width = this.driver.manage().window().getSize().getWidth();
        int height = this.driver.manage().window().getSize().getHeight();
        return new Dimension(width, height);
    }

    /**
     * Opens the browser on the given URL.
     * <p>
     *
     * @param url The URL to be visited.
     */
    public void navigateTo(String url) {
        if (this.driver == null) {
            this.startBrowser();
        }

        this.driver.get(url);
    }

    /**
     * Opens the browser on the given URL.
     * <p>
     *
     * @param url The URL to be visited.
     */
    public void browse(String url) {
        navigateTo(url);
    }

    /**
     * Get a list of tab handlers.
     * <p>
     *
     * @return List of tab handlers.
     */
    public List<String> getTabs() {
        return new ArrayList<>(this.driver.getWindowHandles());
    }

    /**
     * Activate a tab given by the handle.
     * <p>
     *
     * @param handle The tab or window handle.
     */
    public void activateTab(String handle) {
        this.driver.switchTo().window(handle);
    }

    /**
     * Create a new tab and navigate to the given URL.
     * <p>
     *
     * @param url The desired URL.
     */
    public void createTab(String url) {
        executeJavascript(String.format("window.open('%s', '_blank');", url));
        List<String> tabs = getTabs();
        String lastTab = tabs.get(tabs.size() - 1);
        activateTab(lastTab);
    }

    /**
     * Creates a new window with the given URL.
     * <p>
     *
     * @param url The desired URL.
     */
    public void createWindow(String url) {
        executeJavascript(String.format("window.open('%s', '_blank', 'location=0');", url));
        List<String> tabs = getTabs();
        String lastTab = tabs.get(tabs.size() - 1);
        activateTab(lastTab);
    }

    /**
     * Close the current active page (tab or window).
     */
    public void closePage() {
        this.driver.close();

        List<String> tabs = getTabs();
        if (!tabs.isEmpty()) {
            String lastTab = getTabs().get(getTabs().size() - 1);
            activateTab(lastTab);
        }
    }

    /**
     * Returns the active page title.
     * <p>
     *
     * @return The page title.
     */
    public String pageTitle() {
        return this.driver.getTitle();
    }

    /**
     * Returns the active page source.
     * <p>
     *
     * @return {@link org.jsoup.nodes.Document} object for the page source.
     */
    public Document pageSource() {
        return Jsoup.parse(this.driver.getPageSource());
    }

    /**
     * Execute the given javascript code.
     * <p>
     *
     * @param code The code to be executed.
     * @param args The arguments to be passed to the code.
     * @return Returns the code output.
     */
    public Object executeJavascript(String code, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(code, args);
    }

    /**
     * Switch the WebBot driver to the specified iframe.
     * <p>
     *
     * @param element The iframe element.
     */
    public void enterIframe(WebElement element) {
        this.driver.switchTo().frame(element);
    }

    /**
     * Leave the iframe and switch the WebBot driver to the default content.
     */
    public void leaveIframe() {
        this.driver.switchTo().defaultContent();
    }

    /**
     * Get the total number of files of the same type.
     * <p>
     *
     * @param path          The path of the folder where the files are saved.
     * @param fileExtension The extension of the files to be searched for (e.g., .pdf, .txt).
     * @return The number of files of the given type.
     */
    public int getFileCount(String path, String fileExtension) {
        File filePath = new File(path);

        if (!filePath.exists()) throw new RuntimeException("The path does not exist");

        if (!filePath.isDirectory()) throw new RuntimeException("The path must be a directory");

        File[] files = filePath.listFiles();
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            if (file.getName().endsWith(fileExtension)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Pressing the browser’s back button.
     */
    public void back() {
        this.driver.navigate().back();
    }

    /**
     * Pressing the browser’s forward button.
     */
    public void forward() {
        this.driver.navigate().forward();
    }

    /**
     * Refresh the current page.
     */
    public void refresh() {
        this.driver.navigate().refresh();
    }

    /**
     * Find an element using the specified selector with selector type specified by `by`.
     * If more than one element is found, the first instance is returned.
     * <p>
     *
     * @param by               The selector type with the selector string.
     * @param ensureVisible    True to wait for the element to be visible.
     * @param ensureClickable  True to wait for the element to be clickable.
     * @param waitingTime      Maximum wait time (ms) to search for a hit.
     * @return The {@link org.openqa.selenium.WebElement} found.
     */
    public WebElement findElement(By by, boolean ensureVisible, boolean ensureClickable, long waitingTime) {
        ExpectedCondition<WebElement> condition;

        if (ensureVisible) {
            condition = ExpectedConditions.visibilityOfElementLocated(by);
        } else {
            condition = ExpectedConditions.presenceOfElementLocated(by);
        }

        if (ensureClickable) {
            condition = ExpectedConditions.elementToBeClickable(by);
        }

        try {
            return new WebDriverWait(this.driver, waitingTime / 1000).until(condition);
        } catch (TimeoutException e) {
            return null;
        }
    }

    /**
     * Find an element using the specified selector with selector type specified by `by`.
     * If more than one element is found, the first instance is returned.
     * <p>
     *
     * @param by The selector type with the selector string.
     * @return The {@link org.openqa.selenium.WebElement} found.
     */
    public WebElement findElement(By by) {
        return findElement(by, true, false, this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Find elements using the specified selector with selector type specified by `by`.
     * <p>
     *
     * @param by             The selector type with the selector string.
     * @param ensureVisible  True to wait for the element to be visible.
     * @param waitingTime    Maximum wait time (ms) to search for a hit.
     * @return The list of {@link org.openqa.selenium.WebElement} found.
     */
    public List<WebElement> findElements(By by, boolean ensureVisible, long waitingTime) {
        ExpectedCondition<List<WebElement>> condition;
        if (ensureVisible) {
            condition = ExpectedConditions.visibilityOfAllElementsLocatedBy(by);
        } else {
            condition = ExpectedConditions.presenceOfAllElementsLocatedBy(by);
        }

        try {
            return new WebDriverWait(this.driver, waitingTime / 1000).until(condition);
        } catch (TimeoutException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Find elements using the specified selector with selector type specified by `by`.
     * <p>
     *
     * @param by The selector type with the selector string.
     * @return The list of {@link org.openqa.selenium.WebElement} found.
     */
    public List<WebElement> findElements(By by) {
        return findElements(by, true, this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Return the last found dialog. Invoke first the `find_js_dialog` method to look up.
     * <p>
     *
     * @return A handle to the dialog.
     */
    public Alert getJsDialog() {
        return this.driver.switchTo().alert();
    }

    /**
     * Accepts or dismisses a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload).
     * This also cleans the dialog information in the local buffer.
     * <p>
     *
     * @param accept     Whether to accept or dismiss the dialog.
     * @param promptText The text to enter into the dialog prompt before accepting. Used only if this is a prompt dialog.
     * @return True if the dialog was handled successfully.
     */
    public boolean handleJsDialog(boolean accept, String promptText) {
        Alert alert = getJsDialog();

        if (alert == null) return false;

        if (promptText != null && !promptText.isEmpty()) {
            alert.sendKeys(promptText);
        }

        if (accept) {
            alert.accept();
        } else {
            alert.dismiss();
        }
        return true;
    }

    /**
     * Accepts or dismisses a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload).
     * <p>
     *
     * @param accept Whether to accept or dismiss the dialog.
     * @return True if the dialog was handled successfully.
     */
    public boolean handleJsDialog(boolean accept) {
        return handleJsDialog(accept, null);
    }

    /**
     * Wait for all downloads to be finished.
     * Beware that this method replaces the current page with the downloads window.
     * <p>
     *
     * @param timeout Timeout in millis.
     * @return True if the downloads window was found and all downloads finished. False if the timeout was reached.
     */
    public boolean waitForDownloads(int timeout) {
        if ((this.browser.equals(Browser.CHROME) || this.browser.equals(Browser.EDGE)) && this.headless) {
            long startTime = System.currentTimeMillis();
            while (true) {
                if ((System.currentTimeMillis() - startTime) > timeout) {
                    return false;
                }

                int downloadsCount = getFileCount(this.downloadPath, ".crdownload");
                if (downloadsCount == 0) return true;
                sleep(this.DEFAULT_SLEEP_AFTER_ACTION);
            }
        }

        try {
            new WebDriverWait(this.driver, timeout / 1000, 1).until(this.config::waitForDownloads);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Wait for a file to be available on disk.
     * <p>
     *
     * @param path    The path for the file to be executed.
     * @param timeout Maximum wait time (ms) to search for a hit.
     * @return True if the file was found. False if the timeout was reached.
     */
    @SneakyThrows
    public boolean waitForFile(String path, long timeout) {
        File file = new File(path);

        long startTime = System.currentTimeMillis();
        while (true) {
            if ((System.currentTimeMillis() - startTime) > timeout) {
                return false;
            }

            if (file.isFile() && file.canRead() && Files.size(file.toPath()) > 0) {
                return true;
            }

            this.sleep(this.DEFAULT_SLEEP_AFTER_ACTION);
        }
    }

    /**
     * Returns the last created file in a specific folder path.
     * <p>
     *
     * @param path          The path of the folder where the file is expected.
     * @param fileExtension The extension of the file to be searched for (e.g., .pdf, .txt).
     * @return The path of the last created file.
     */
    @SneakyThrows
    private Path getLastCreatedFile(String path, String fileExtension) {
        if (path.isEmpty()) {
            path = this.downloadPath;
        }

        Path dir = Paths.get(path);
        return Files.list(dir)
                .filter(f -> !Files.isDirectory(f) && f.toString().endsWith(fileExtension))
                .max(Comparator.comparingLong(f -> f.toFile().lastModified()))
                .orElse(null);
    }

    /**
     * Wait for a new file to be available on disk without the file path.
     * <p>
     *
     * @param path          The path of the folder where the file is expected.
     * @param fileExtension The extension of the file to be searched for (e.g., .pdf, .txt).
     * @param currentCount  The current number of files in the folder of the given type.
     * @param timeout       Maximum wait time (ms) to search for a hit.
     * @return The path of the last created file of the given type.
     */
    @SneakyThrows
    public String waitForNewFile(String path, String fileExtension, int currentCount, long timeout) {
        if (path.isEmpty()) {
            path = this.downloadPath;
        }

        if (currentCount == 0) {
            currentCount = getFileCount(path, fileExtension);
        }

        long startTime = System.currentTimeMillis();
        while (true) {
            if ((System.currentTimeMillis() - startTime) > timeout) {
                return "";
            }

            int fileCount = getFileCount(path, fileExtension);
            if (fileCount == currentCount + 1) {
                Path lastFile = getLastCreatedFile(path, fileExtension);
                if (lastFile != null && Files.size(lastFile) > 0) {
                    return lastFile.toString();
                }
            }

            sleep(this.DEFAULT_SLEEP_AFTER_ACTION);
        }
    }

    /**
     * Context manager to wait for a new page to load and activate it.
     * <p>
     *
     * @param activate    Whether or not to activate the new page.
     * @param waitingTime The maximum waiting time. Defaults to 10000.
     * @param function    The lambda function to be executed.
     */
    public void waitForNewPage(boolean activate, long waitingTime, Runnable function) {
        int tabsCount = getTabs().size();
        function.run();

        long startTime = System.currentTimeMillis();
        while (getTabs().size() == tabsCount) {
            if ((System.currentTimeMillis() - startTime) > waitingTime) {
                return;
            }
            sleep(this.DEFAULT_SLEEP_AFTER_ACTION);
        }

        List<String> tabs = getTabs();
        if (activate) {
            activateTab(tabs.get(tabs.size() - 1));
        }
    }

    /**
     * Context manager to wait for a new page to load and activate it.
     * <p>
     *
     * @param activate Whether or not to activate the new page.
     * @param function The lambda function to be executed.
     */
    public void waitForNewPage(boolean activate, Runnable function) {
        waitForNewPage(activate, 10000, function);
    }

    /**
     * Context manager to wait for a new page to load and activate it.
     * <p>
     *
     * @param function The lambda function to be executed.
     */
    public void waitForNewPage(Runnable function) {
        waitForNewPage(true, function);
    }

    /**
     * Wait for the element to be visible or hidden.
     * <p>
     *
     * @param element     The element to wait for.
     * @param visible     Whether to wait for the element to be visible.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if the element is visible. False if the timeout was reached.
     */
    public boolean waitForElementVisibilitiy(WebElement element, boolean visible, long waitingTime) {
        try {
            WebDriverWait webDriverWait = new WebDriverWait(this.driver, waitingTime / 1000);
            if (visible) {
                webDriverWait.until(ExpectedConditions.visibilityOf(element));
            } else {
                webDriverWait.until(ExpectedConditions.invisibilityOf(element));
            }

            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Wait for the element to be visible or hidden.
     * <p>
     *
     * @param element     The element to wait for.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if the element is visible. False if the timeout was reached.
     */
    public boolean waitForElementVisibilitiy(WebElement element, long waitingTime) {
        return waitForElementVisibilitiy(element, true, waitingTime);
    }

    /**
     * Wait until the WebElement element becomes stale (outdated).
     * <p>
     *
     * @param element     The element to monitor for staleness.
     * @param waitingTime Timeout in millis.
     * @return True if the element is stale. False if the timeout was reached.
     */
    public boolean waitForStaleElement(WebElement element, long waitingTime) {
        try {
            new WebDriverWait(this.driver, waitingTime / 1000)
                    .until(ExpectedConditions.stalenessOf(element));
            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Execute a webdriver command.
     * <p>
     *
     * @param command The command URL after the session part.
     * @param params  The payload to be serialized and sent to the webdriver.
     * @param reqType The type of request to be made.
     * @return The value of the response.
     */
    @SneakyThrows
    private HttpResponse webdriverCommand(String command, Map<String, Object> params, HttpMethod reqType) {
        HttpCommandExecutor executor = this.config.executor(this.driver);

        URL urlAddress = executor.getAddressOfRemoteServer();

        SessionId sessionId = this.config.getSessionId();
        String path = String.format("/session/%s/%s", sessionId.toString(), command);

        HttpClient httpClient = HttpClient.Factory.createDefault().createClient(urlAddress);

        HttpRequest request = new HttpRequest(reqType, path);

        if (params == null || params.isEmpty()) {
            request.setContent("{}".getBytes());
        } else {
            request.setContent(new Gson().toJson(params).getBytes());
        }

        return httpClient.execute(request);

    }

    /**
     * Print the current page as a PDF file.
     * <p>
     *
     * @param path         The path for the file to be saved.
     * @param printOptions Print options as defined at.
     * @return The saved file path.
     */
    @SneakyThrows
    public String printPdf(String path, Map<String, Object> printOptions) {
        String pgTitle = pageTitle().isEmpty() ? "document" : pageTitle();
        int timeout = 60_000;
        String defaultPath = Paths.get(this.downloadPath, pgTitle + ".pdf").toString();

        if ((this.browser.equals(Browser.CHROME) || this.browser.equals(Browser.EDGE)) && !this.headless) {
            int pdfCurrentCount = getFileCount(this.downloadPath, ".pdf");
            // Chrome still does not support headless webdriver print
            // but Firefox does.
            executeJavascript("window.print();");

            // We need to wait for the file to be available in this case.
            if (!pageTitle().isEmpty()) {
                waitForFile(defaultPath, timeout);
            } else {
                // Waiting when the file don 't have the page title in path
                waitForNewFile(this.downloadPath, ".pdf", pdfCurrentCount, timeout);
            }

            // Move the downloaded pdf file if the path is not empty
            if (!path.isEmpty()) {
                Path lastDownloadedPdf = getLastCreatedFile(this.downloadPath, ".pdf");
                Files.move(lastDownloadedPdf, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
                return path;
            }
            wait(2000);
            return defaultPath;
        }

        if (printOptions == null || printOptions.isEmpty()) {
            printOptions = new HashMap<>();
            printOptions.put("landscape", false);
            printOptions.put("displayHeaderFooter", false);
            printOptions.put("printBackground", true);
            printOptions.put("preferCSSPageSize", true);
            printOptions.put("marginTop", 0);
            printOptions.put("marginBottom", 0);
        }

        if (path.isEmpty()) {
            path = defaultPath;
        }

        WebDriverResponse jsonResponse = new Gson().fromJson(webdriverCommand("print", printOptions, HttpMethod.POST).getContentReader(), WebDriverResponse.class);
        byte[] bytes = Base64.getDecoder().decode(jsonResponse.getValue());
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
        }

        return path;
    }

    /**
     * Print the current page as a PDF file.
     * <p>
     *
     * @param path The path for the file to be saved.
     * @return The saved file path.
     */
    public String printPdf(String path) {
        return printPdf(path, null);
    }

    /**
     * Print the current page as a PDF file.
     * <p>
     *
     * @return The saved file path.
     */
    public String printPdf() {
        return printPdf("");
    }

    /**
     * Install an extension in the Firefox browser.
     * This will start the browser if it was not started yet.
     * <p>
     *
     * @param extensionPath The path of the .xpi extension to be loaded.
     */
    public void installFirefoxExtension(String extensionPath) {
        if (!this.browser.equals(Browser.FIREFOX)) {
            throw new IllegalStateException("This methods is only available for Firefox");
        }

        if (this.driver == null) {
            startBrowser();
        }

        ((FirefoxDriver) driver).installExtension(Paths.get(extensionPath));
    }

    /**
     * Configure the filepath for upload in a file element.
     * <b>Note:</b> This method does not submit the form.
     * <p>
     *
     * @param element  The file upload element.
     * @param filePath The path to the file to be uploaded.
     */
    public void setFileInputElement(WebElement element, File filePath) {
        element.sendKeys(filePath.getAbsolutePath());
    }

    /**
     * Press Page Down key.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void pageDown(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.DOWN);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Page Down key
     */
    public void pageDown() {
        pageDown(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press Page Up key.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void pageUp(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.UP);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Page Up key
     */
    public void pageUp() {
        pageUp(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    public void enter(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.ENTER);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key Enter.
     */
    public void enter() {
        enter(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Type a text char by char (individual key events).
     * <p>
     *
     * @param text     Text to be typed.
     * @param interval Interval (ms) between each key press.
     */
    public void type(String text, int interval) {
        Actions action = new Actions(this.driver);
        for (String ch : text.split("")) {
            action.sendKeys(ch);
            action.pause(interval / 1000);
        }
        action.perform();
        wait(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Type a text char by char (individual key events).
     * <p>
     *
     * @param text Text to be typed.
     */
    public void type(String text) {
        type(text, 0);
    }

    /**
     * Type a text char by char (individual key events).
     * <p>
     *
     * @param text Text to be typed.
     */
    public void kbType(String text) {
        type(text);
    }

    /**
     * Paste content from the clipboard.
     */
    public void paste() {
        type(this.clipboard);
    }

    /**
     * Press keys CTRL+C.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void controlC(int wait) {
        String cmd = "try {return document.activeElement.value.substring(document.activeElement.selectionStart,document.activeElement.selectionEnd);} catch(error) {return window.getSelection().toString();}";
        this.clipboard = (String) this.executeJavascript(cmd);
        wait(Math.max(0, wait));
    }

    /**
     * Press keys CTRL+C.
     */
    public void controlC() {
        controlC(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press keys CTRL+A.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void controlA(int wait) {
        Actions action = new Actions(this.driver);
        Keys key = Keys.CONTROL;

        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            key = Keys.COMMAND;
        }

        action.keyDown(key);
        action.sendKeys("a");
        action.keyUp(key);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press keys CTRL+A.
     */
    public void controlA() {
        controlA(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press keys CTRL+X.
     */
    public void controlV() {
        paste();
    }

    /**
     * Press key Esc.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void keyEsc(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.ESCAPE);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key Esc.
     */
    public void keyEsc() {
        keyEsc(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key Enter.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void keyEnter(int wait) {
        enter(wait);
    }

    /**
     * Press key Enter.
     */
    public void keyEnter() {
        keyEnter(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key Home.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void keyHome(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.HOME);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key Home.
     */
    public void keyHome() {
        keyHome(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key End.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void keyEnd(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.END);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key End.
     */
    public void keyEnd() {
        keyEnd(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key Delete.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void delete(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.DELETE);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key Delete.
     */
    public void delete() {
        delete(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key Space.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void space(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.SPACE);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key Space.
     */
    public void space() {
        space(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key Tab.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void tab(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.TAB);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press key Tab.
     */
    public void tab() {
        tab(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press a sequence of keys. Hold the keys in the specific order and releases them.
     * <p>
     *
     * @param interval Interval (ms) in which to press and release keys.
     * @param keys     List of Keys to be pressed.
     */
    private void typeKeysWithInterval(int interval, CharSequence... keys) {
        Actions action = new Actions(driver);

        for (CharSequence key : keys) {
            if (key instanceof String) {
                action.sendKeys(key);
            } else {
                action.keyDown(key);
            }
            action.pause(interval / 1000);
        }

        for (CharSequence key : keys) {
            if (key instanceof String) {
                continue;
            }
            action.keyUp(key);
            action.pause(interval / 1000);
        }

        action.perform();
    }

    /**
     * Press a sequence of keys. Hold the keys in the specific order and releases them.
     * <p>
     *
     * @param keys List of Keys to be pressed.
     */
    public void typeKeys(CharSequence... keys) {
        typeKeysWithInterval(100, keys);
    }

    /**
     * Press Left key.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void typeLeft(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.LEFT);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Left key.
     */
    public void typeLeft() {
        typeLeft(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press Right key.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void typeRight(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.RIGHT);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Right key.
     */
    public void typeRight() {
        typeRight(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press Down key
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void typeDown(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.DOWN);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Down key
     */
    public void typeDown() {
        typeDown(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press Up key
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void typeUp(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.UP);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Up key
     */
    public void typeUp() {
        typeUp(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Press key Right.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void keyRight(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.RIGHT);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Backspace key.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void backspace(int wait) {
        Actions action = new Actions(this.driver);
        action.sendKeys(Keys.BACK_SPACE);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Press Backspace key.
     */
    public void backspace() {
        backspace(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Hold key Shift.
     * <p>
     *
     * @param wait Wait interval (ms) after task.
     */
    public void holdShift(int wait) {
        Actions action = new Actions(this.driver);
        action.keyDown(Keys.SHIFT);
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Hold key Shift.
     */
    public void holdShift() {
        holdShift(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Release key Shift.
     * This method needs to be invoked after holding Shift or similar.
     */
    public void releaseShift() {
        Actions action = new Actions(this.driver);
        action.keyUp(Keys.SHIFT);
        action.perform();
    }

    /**
     * Copy content to the clipboard.
     * <p>
     *
     * @param text The text to be copied.
     */
    public void copyToClipboard(String text) {
        this.clipboard = text;
    }

    /**
     * Get the current content in the clipboard.
     * <p>
     *
     * @return Current clipboard content.
     */
    public String getClipboard() {
        return this.clipboard;
    }

    /**
     * Wait / Sleep for a given interval.
     *
     * @param ms The interval (ms) to wait.
     */
    @SneakyThrows
    public void wait(int ms) {
        Thread.sleep(ms);
    }

    /**
     * Wait / Sleep for a given interval.
     *
     * @param ms The interval (ms) to wait.
     */
    public void sleep(int ms) {
        wait(ms);
    }

    /**
     * Add an image into the state image map.
     * <p>
     *
     * @param label The image identifier.
     * @param path  The path for the image on disk.
     */
    public void addImage(String label, String path) {
        this.images.put(label, path);
    }

    /**
     * Return an image from teh state image map.
     * <p>
     *
     * @param label The image identifier.
     * @return The {@link MarvinImage} object.
     */
    private MarvinImage getImageFromMap(String label) {
        String imagePath = images.getOrDefault(label, label + ".png");
        return Resource.getResourceAsMarvinImage(this.classLoader, imagePath);
    }

    /**
     * Changes the current screen element the bot will interact when using click(), move(), and similar methods.
     * <p>
     *
     * @param state A screen element coordinates (left, top, width, height).
     */
    public void setCurrentElement(State state) {
        this.element = state;
    }

    /**
     * Return the last element found.
     * <p>
     *
     * @return The element coordinates (left, top, width, height)
     */
    public State getLastElement() {
        return this.element;
    }

    /**
     * Return the x position of the last element found.
     * <p>
     *
     * @return The x position.
     */
    public int getLastX() {
        return this.x;
    }

    /**
     * Return the y position of the last element found.
     * <p>
     *
     * @return The y position.
     */
    public int getLastY() {
        return this.y;
    }

    private State findSubimage(MarvinImage visualImage, MarvinImage screenImage, Region region, double matching, boolean findBest) {
        List<State> elements = cvFind.findAllElements(visualImage, screenImage, region, matching, true);
        if (findBest)
            return cvFind.findBestElement(elements);

        if (elements.isEmpty())
            return new State();

        return elements.get(0);
    }

    /**
     * Find an element defined by label on screen and returns its coordinates.
     * <p>
     *
     * @param visualImage The image to be found.
     * @param screenImage The screen image to search.
     * @param region      The region to search.
     * @param matching    Minimum score to consider a match in the element image recognition process.
     * @param best        Whether or not to search for the best value. If False the method returns on the first find.
     * @return A {@link State} instance with the x and y coordinates for the element.
     */
    private State getElementCoords(MarvinImage visualImage, MarvinImage screenImage, Region region, double matching, boolean best) {
        return findSubimage(visualImage, screenImage, region, matching, best);
    }

    /**
     * Find an element defined by label on screen and returns its coordinates.
     * <p>
     *
     * @param label    The image identifier.
     * @param region   The region to search.
     * @param matching Minimum score to consider a match in the element image recognition process.
     * @return A {@link State} instance with the x and y coordinates for the element.
     */
    public State getElementCoords(String label, Region region, double matching) {
        return getElementCoords(getImageFromMap(label), getScreenImage(), region, matching, false);
    }

    /**
     * Find an element defined by label on screen and returns its coordinates.
     * <p>
     *
     * @param label    The image identifier.
     * @param matching Minimum score to consider a match in the element image recognition process.
     * @return A {@link State} instance with the x and y coordinates for the element.
     */
    public State getElementCoords(String label, double matching) {
        return getElementCoords(getImageFromMap(label), getScreenImage(), null, matching, false);
    }

    /**
     * Find an element defined by label on screen and returns its centered coordinates.
     * <p>
     *
     * @param label    The image identifier.
     * @param matching Minimum score to consider a match in the element image recognition process.
     * @return Point with coordinates of the center of the element.
     */
    public java.awt.Point getElementCoordsCentered(String label, double matching) {
        State element = getElementCoords(getImageFromMap(label), getScreenImage(), null, matching, false);
        if (element.isAvailable()) {
            return element.getCenteredPosition();
        }
        return null;
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param visualElem  The image to be found.
     * @param region      The region to search.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @param best        Whether or not to keep looking until the best matching is found.
     * @return True if element was found, false otherwise.
     */
    public boolean findUntil(MarvinImage visualElem, Region region, int threshold, boolean grayscale, double matching, long waitingTime, boolean best) {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > waitingTime) {
                return false;
            }

            MarvinImage screen = getScreenImage();
            int searchWindowWidth = (region.getWidth() != 0 ? region.getWidth() : screen.getWidth());
            int searchWindowHeight = (region.getHeight() != 0 ? region.getHeight() : screen.getHeight());

            MarvinImage screenCopy = screen.clone();
            MarvinImage visualElemCopy = visualElem.clone();

            if (threshold > 0) {
                screenCopy = cvFind.threshold(screenCopy, threshold);
                visualElemCopy = cvFind.threshold(visualElemCopy, threshold);
            }

            if (grayscale) {
                screenCopy = cvFind.grayscale(screenCopy);
                visualElemCopy = cvFind.grayscale(visualElemCopy);
            }

            State element = getElementCoords(visualElemCopy, screenCopy, new Region(region.getX(), region.getY(), searchWindowWidth, searchWindowHeight), matching, best);
            if (element.isAvailable()) {
                this.element = element;
                return true;
            }
        }
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param region      The region to search.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @param best        Whether or not to keep looking until the best matching is found.
     * @return True if element was found, false otherwise.
     */
    public boolean findUntil(String label, Region region, int threshold, boolean grayscale, double matching, long waitingTime, boolean best) {
        return findUntil(getImageFromMap(label), region, threshold, grayscale, matching, waitingTime, best);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if element was found, false otherwise.
     */
    public boolean findUntil(String label, int threshold, boolean grayscale, double matching, long waitingTime) {
        return findUntil(getImageFromMap(label), new Region(), threshold, grayscale, matching, waitingTime, false);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param visualImage The image to be found.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @return True if element was found, false otherwise.
     */
    public boolean findUntil(MarvinImage visualImage, int threshold, boolean grayscale, double matching) {
        return findUntil(visualImage, new Region(), threshold, grayscale, matching, this.DEFAULT_SLEEP_AFTER_ACTION, false);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if element was found, false otherwise.
     */
    public boolean findText(String label, int threshold, double matching, long waitingTime) {
        return findUntil(label, threshold, true, matching, waitingTime);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if element was found, false otherwise.
     */
    public boolean findText(String label, double matching, long waitingTime) {
        return findUntil(label, 0, true, matching, waitingTime);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param matching    The matching index ranging from 0 to 1.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if element was found, false otherwise.
     */
    public boolean find(String label, double matching, int threshold, boolean grayscale, long waitingTime) {
        return findUntil(label, threshold, grayscale, matching, waitingTime);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param matching    The matching index ranging from 0 to 1.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if element was found, false otherwise.
     */
    public boolean find(String label, double matching, boolean grayscale, long waitingTime) {
        return findUntil(label, 0, grayscale, matching, waitingTime);
    }

    /**
     * Find an element defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return True if element was found, false otherwise.
     */
    public boolean find(String label, double matching, long waitingTime) {
        return findUntil(label, 0, false, matching, waitingTime);
    }

    /**
     * Find all elements defined by label on screen until a timeout happens.
     * <p>
     *
     * @param visualElem  The {@link MarvinImage} to find.
     * @param region      The region to search for the elements.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @param best        Whether or not to keep looking until the best matching is found.
     * @return A list with all element coordinates found.
     */
    public List<State> findAll(MarvinImage visualElem, Region region, int threshold, boolean grayscale, double matching, long waitingTime, boolean best) {
        long startTime = System.currentTimeMillis();
        List<State> elements = new ArrayList<>();

        while (true) {
            if (System.currentTimeMillis() - startTime > waitingTime) {
                return elements;
            }

            MarvinImage screen = getScreenImage();
            int width = (region.getWidth() != 0 ? region.getWidth() : screen.getWidth());
            int height = (region.getHeight() != 0 ? region.getHeight() : screen.getHeight());

            MarvinImage screenCopy = screen.clone();
            MarvinImage visualElemCopy = visualElem.clone();

            if (threshold > 0) {
                screenCopy = cvFind.threshold(screenCopy, threshold);
                visualElemCopy = cvFind.threshold(visualElemCopy, threshold);
            }

            if (grayscale) {
                screenCopy = cvFind.grayscale(screenCopy);
                visualElemCopy = cvFind.grayscale(visualElemCopy);
            }

            elements = cvFind.findAllElements(visualElemCopy, screenCopy, new Region(region.getX(), region.getY(), width, height), matching, best);
            if (!elements.isEmpty())
                return elements;
        }
    }

    /**
     * Find all elements defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param region      The region to search for the elements.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @param best        Whether or not to keep looking until the best matching is found.
     * @return A list with all element coordinates found.
     */
    public List<State> findAll(String label, Region region, int threshold, boolean grayscale, double matching, long waitingTime, boolean best) {
        return findAll(getImageFromMap(label), region, threshold, grayscale, matching, waitingTime, best);
    }

    /**
     * Find all elements defined by label on screen until a timeout happens.
     * <p>
     *
     * @param label       The image identifier.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return A list with all element coordinates found.
     */
    public List<State> findAll(String label, double matching, long waitingTime) {
        return findAll(label, new Region(), 0, false, matching, waitingTime, false);
    }

    /**
     * Find multiple elements defined by label on screen until a timeout happens.
     * <p>
     *
     * @param labels      A list of image identifiers.
     * @param region      The region to search for the elements.
     * @param threshold   The threshold to be applied when doing grayscale search.
     * @param grayscale   Whether or not to convert to grayscale before searching.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @param best        Whether or not to keep looking until the best matching is found.
     * @return A Map in which the key is the label and value are the element coordinates.
     */
    public Map<String, State> findMultiple(List<String> labels, Region region, int threshold, boolean grayscale, double matching, long waitingTime, boolean best) {
        long startTime = System.currentTimeMillis();
        Map<String, State> elements = new HashMap<>();

        while (true) {
            for (String label : labels) {
                if (System.currentTimeMillis() - startTime > waitingTime) {
                    return elements;
                }

                MarvinImage screen = getScreenImage();
                int width = (region.getWidth() != 0 ? region.getWidth() : screen.getWidth());
                int height = (region.getHeight() != 0 ? region.getHeight() : screen.getHeight());

                MarvinImage screenCopy = screen.clone();
                MarvinImage visualElemCopy = getImageFromMap(label);

                if (threshold > 0) {
                    screenCopy = cvFind.threshold(screenCopy, threshold);
                    visualElemCopy = cvFind.threshold(visualElemCopy, threshold);
                }

                if (grayscale) {
                    screenCopy = cvFind.grayscale(screenCopy);
                    visualElemCopy = cvFind.grayscale(visualElemCopy);
                }

                State element = getElementCoords(visualElemCopy, screenCopy, new Region(region.getX(), region.getY(), width, height), matching, best);
                if (element.isAvailable()) {
                    if (elements.containsKey(label))
                        continue;

                    elements.put(label, element);
                }

                if (elements.keySet().size() == labels.size())
                    return elements;
            }
        }
    }

    /**
     * Find multiple elements defined by label on screen until a timeout happens.
     * <p>
     *
     * @param labels      A list of image identifiers.
     * @param matching    The matching index ranging from 0 to 1.
     * @param waitingTime Maximum wait time (ms) to search for a hit.
     * @return A Map in which the key is the label and value are the element coordinates.
     */
    public Map<String, State> findMultiple(List<String> labels, double matching, long waitingTime) {
        return findMultiple(labels, new Region(), 0, false, matching, waitingTime, false);
    }

    /**
     * Click at the coordinate defined by x and y.
     * <p>
     *
     * @param x                     The X coordinate.
     * @param y                     The Y coordinate.
     * @param clicks                Number of times to click.
     * @param intervalBetweenClicks The interval between clicks in ms.
     * @param button                One of 'left', 'right'.
     */
    public void clickAt(int x, int y, int clicks, int intervalBetweenClicks, String button) {
        Actions action = new Actions(this.driver);
        moveTo(x, y, this.DEFAULT_SLEEP_AFTER_ACTION);
        for (int i = 0; i < clicks; i++) {
            if (button.equals("left")) {
                action.click();
            } else if (button.equals("right")) {
                action.contextClick();
            } else {
                throw new RuntimeException("Invalid value for button. Accepted values are left or right.");
            }
            action.pause(intervalBetweenClicks / 1000);
        }

        action.perform();
    }

    /**
     * Click at the coordinate defined by x and y.
     * <p>
     *
     * @param x      The X coordinate.
     * @param y      The Y coordinate.
     * @param clicks The number of clicks.
     */
    public void clickAt(int x, int y, int clicks) {
        clickAt(x, y, clicks, this.DEFAULT_SLEEP_AFTER_ACTION, "left");
    }

    /**
     * Click at the coordinate defined by x and y.
     * <p>
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void clickAt(int x, int y) {
        clickAt(x, y, 1, this.DEFAULT_SLEEP_AFTER_ACTION, "left");
    }

    /**
     * Click on the element.
     * <p>
     *
     * @param label    The image identifier
     * @param matching The matching index ranging from 0 to 1.
     */
    public void clickOn(String label, double matching) {
        State element = getElementCoords(label, matching);
        if (!element.isAvailable()) {
            throw new RuntimeException("Element not available. Cannot find " + label);
        }

        java.awt.Point position = element.getCenteredPosition();
        clickAt((int) position.getX(), (int) position.getY());
    }

    /**
     * Click on the element.
     * <p>
     *
     * @param label The image identifier.
     */
    public void clickOn(String label) {
        clickOn(label, 0.97);
    }

    /**
     * Right click at the coordinate defined by x and y.
     * <p>
     *
     * @param x      The X coordinate.
     * @param y      The Y coordinate.
     * @param clicks The number of clicks.
     */
    public void rightClickAt(int x, int y, int clicks) {
        clickAt(x, y, clicks, this.DEFAULT_SLEEP_AFTER_ACTION, "right");
    }

    /**
     * Right click at the coordinate defined by x and y.
     * <p>
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void rightClickAt(int x, int y) {
        clickAt(x, y, 1, this.DEFAULT_SLEEP_AFTER_ACTION, "right");
    }

    /**
     * Click on the last found element.
     * <p>
     *
     * @param clicks                Number of times to click.
     * @param intervalBetweenClicks The interval between clicks in ms.
     * @param button                One of 'left', 'right'.
     * @param wait                  Interval to wait after clicking on the element.
     */
    public void click(int clicks, int intervalBetweenClicks, String button, int wait) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        java.awt.Point centeredPostion = this.element.getCenteredPosition();
        this.clickAt((int) centeredPostion.getX(), (int) centeredPostion.getY(), clicks, intervalBetweenClicks, button);
        wait(Math.max(0, wait));
    }

    /**
     * Left click on the last found element.
     * <p>
     *
     * @param state The element to be clicked.
     */
    public void click(State state) {
        if (!state.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        clickAt(state.getX(), state.getY());
    }

    /**
     * Click on the last found element.
     * <p>
     *
     * @param wait Interval to wait after clicking on the element.
     */
    public void click(int wait) {
        click(1, this.DEFAULT_SLEEP_AFTER_ACTION, "left", wait);
    }

    /**
     * Click on the last found element.
     */
    public void click() {
        click(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Click Relative on the last found element.
     * <p>
     *
     * @param x Horizontal offset.
     * @param y Vertical offset.
     */
    public void clickRelative(int x, int y) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        x = this.element.getX() + x;
        y = this.element.getY() + y;
        clickAt(x, y, 1, this.DEFAULT_SLEEP_AFTER_ACTION, "left");
    }

    /**
     * Double Click Relative on the last found element.
     * <p>
     *
     * @param x Horizontal offset.
     * @param y Vertical offset.
     */
    public void doubleClickRelative(int x, int y) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        x = this.element.getX() + x;
        y = this.element.getY() + y;
        clickAt(x, y, 2, this.DEFAULT_SLEEP_AFTER_ACTION, "left");
    }

    /**
     * Double Click on the last found element.
     * <p>
     *
     * @param wait Interval to wait after clicking on the element.
     */
    public void doubleClick(int wait) {
        click(2, this.DEFAULT_SLEEP_AFTER_ACTION, "left", wait);
    }

    /**
     * Double Click on the last found element.
     */
    public void doubleClick() {
        doubleClick(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Triple Click on the last found element.
     * <p>
     *
     * @param wait Interval to wait after clicking on the element.
     */
    public void tripleClick(int wait) {
        click(3, this.DEFAULT_SLEEP_AFTER_ACTION, "left", wait);
    }

    /**
     * Triple Click on the last found element.
     */
    public void tripleClick() {
        tripleClick(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Triple Click Relative on the last found element.
     * <p>
     *
     * @param x Horizontal offset.
     * @param y Vertical offset.
     */
    public void tripleClickRelative(int x, int y) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        x = this.element.getX() + x;
        y = this.element.getY() + y;
        clickAt(x, y, 3, this.DEFAULT_SLEEP_AFTER_ACTION, "left");
    }

    /**
     * Right click on the last found element.
     * <p>
     *
     * @param state The element to be clicked.
     */
    public void rightClick(State state) {
        if (!state.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        rightClickAt(state.getX(), state.getY());
    }

    /**
     * Right click on the last found element.
     * <p>
     *
     * @param wait Interval to wait after clicking on the element.
     */
    public void rightClick(int wait) {
        click(1, this.DEFAULT_SLEEP_AFTER_ACTION, "right", wait);
    }

    /**
     * Right click on the last found element.
     */
    public void rightClick() {
        rightClick(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Right Click Relative on the last found element.
     * <p>
     *
     * @param x Horizontal offset.
     * @param y Vertical offset.
     */
    public void rightClickRelative(int x, int y) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        x = this.element.getX() + x;
        y = this.element.getY() + y;
        rightClickAt(x, y);
    }

    /**
     * Move the mouse relative to its current position.
     * <p>
     *
     * @param x    The X coordinate.
     * @param y    The Y coordinate.
     * @param wait Interval to wait after moving on the element.
     */
    public void moveTo(int x, int y, int wait) {
        int mx = x - this.x;
        int my = y - this.y;
        this.x = x;
        this.y = y;

        Actions action = new Actions(this.driver);
        action.moveByOffset(mx, my);
        action.perform();

        wait(Math.max(0, wait));
    }

    /**
     * Move the mouse relative to its current position.
     * <p>
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void moveTo(int x, int y) {
        moveTo(x, y, this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Mouse the move to the coordinate defined by x and y.
     * <p>
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void mouseMove(int x, int y) {
        moveTo(x, y);
    }

    /**
     * Move the mouse relative to its current position.
     * <p>
     *
     * @param x Horizontal offset.
     * @param y Vertical offset.
     */
    public void moveRelative(int x, int y) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        x = this.element.getX() + x;
        y = this.element.getY() + y;
        moveTo(x, y, this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Move to the center position of last found item.
     * <p>
     *
     * @param wait Interval to wait after moving on the element.
     */
    public void move(int wait) {
        if (!this.element.isAvailable()) {
            throw new ElementNotAvailableException("Element not available.");
        }

        this.moveTo(this.element.getX(), this.element.getY(), wait);
    }

    /**
     * Move to the center position of last found item.
     */
    public void move() {
        move(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Move randomly along the given x, y range.
     * <p>
     *
     * @param rangeX Horizontal range.
     * @param rangeY Vertical range.
     */
    public void moveRandom(int rangeX, int rangeY) {
        int x = (int) (Math.random() * rangeX);
        int y = (int) (Math.random() * rangeY);
        moveTo(x, y);
    }

    /**
     * Holds down the requested mouse button.
     * <p>
     *
     * @param wait Interval to wait after clicking on the element.
     */
    public void mouseDown(int wait) {
        Actions action = new Actions(this.driver);
        action.clickAndHold();
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Holds down the requested mouse button.
     */
    public void mouseDown() {
        mouseDown(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Releases the requested mouse button.
     * <p>
     *
     * @param wait Interval to wait after clicking on the element.
     */
    public void mouseUp(int wait) {
        Actions action = new Actions(this.driver);
        action.release();
        action.perform();
        wait(Math.max(0, wait));
    }

    /**
     * Releases the requested mouse button.
     */
    public void mouseUp() {
        mouseUp(this.DEFAULT_SLEEP_AFTER_ACTION);
    }

    /**
     * Scroll Down n clicks.
     * <p>
     *
     * @param clicks Number of times to scroll down.
     */
    public void scrollDown(int clicks) {
        for (int i = 0; i < clicks; i++) {
            executeJavascript("window.scrollTo(0, window.scrollY + 200)");
            sleep(200);
        }
    }

    /**
     * Scroll Up n clicks.
     * <p>
     *
     * @param clicks Number of times to scroll up.
     */
    public void scrollUp(int clicks) {
        for (int i = 0; i < clicks; i++) {
            executeJavascript("window.scrollTo(0, window.scrollY - 200)");
            sleep(200);
        }
    }
}
