package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.Listeners;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Listeners({AllureTestNg.class})
public class TravelInsuranceTest {

    private WebDriver driver;
    private WebDriverWait wait;

    // URLs
    private static final String BASE = "https://digital.harel-group.co.il";
    private static final String HOME = BASE + "/travel-policy";
    private static final String DEST_PATH = "/travel-policy/wizard/destination";
    private static final String DATE_PATH = "/travel-policy/wizard/date";
    private static final String TRAVELERS_PATH = "/travel-policy/wizard/travelers";

    // Selectors
    private static final By BTN_NEW_PURCHASE = By.cssSelector("[data-hrl-bo='purchase-for-new-customer']");
    private static final By DEST_USA_TILE = By.cssSelector("[data-hrl-bo^='USA']");     // e.g. USA-selected / USA
    private static final By BTN_NEXT = By.cssSelector("[data-hrl-bo='wizard-next-button']");
    private static final By START_INPUT = By.cssSelector("#travel_start_date");
    private static final By END_INPUT = By.cssSelector("#travel_end_date");
    private static final By ARROW_FORWARD = By.cssSelector("[data-hrl-bo='arrow-forward']");
    private static final By CALENDAR_ANY = By.cssSelector(".MuiPickersBasePicker-container, [role='grid']");

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId IL = ZoneId.of("Asia/Jerusalem");

    @BeforeClass
    public void setUp() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1366,900",
                "--lang=he",
                "--remote-allow-origins=*"
        );
        driver = new ChromeDriver(opts);
        wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    public void travelFlow_FirstTimeBuyer_CalendarOnly_ComputedDates() throws IOException {
        // compute exact dates
        LocalDate today = LocalDate.now(IL);
        LocalDate startDate = today.plusDays(7);
        LocalDate endDate = startDate.plusDays(30);

        // go home
        driver.get(HOME);
        save("00-home");
        closeOverlays();
        click(BTN_NEW_PURCHASE, "01-click-first-time");
        waitUrlContains(DEST_PATH, "02-on-destination");

        // choose destination and continue
        click(DEST_USA_TILE, "03-choose-usa");
        click(BTN_NEXT, "04-next-to-dates");
        waitUrlContains(DATE_PATH, "05-on-date-step");
        waitVisible(CALENDAR_ANY, "05b-calendar-visible");

        // pick start (calendar only)
        openInput(START_INPUT, "06-open-start");
        clickCalendarDay(startDate, true, "07-pick-start-" + startDate);

        // pick end (calendar only)
        openInput(END_INPUT, "08-open-end");
        clickCalendarDay(endDate, true, "09-pick-end-" + endDate);

        // continue and verify travelers
        click(BTN_NEXT, "10-next-to-travelers");
        waitUrlContains(TRAVELERS_PATH, "11-on-travelers");
        Assert.assertTrue(driver.getCurrentUrl().contains(TRAVELERS_PATH), "Travelers step expected");
        save("12-travelers");
    }

    /* ===== helpers ===== */

    private void openInput(By input, String name) throws IOException {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(input));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center',inline:'center'});", el);
        el.click();
        waitVisible(CALENDAR_ANY, name);
    }

    private void clickCalendarDay(LocalDate date, boolean canPageForward, String snapName) throws IOException {
        String key = date.format(YMD);
        By btn = By.cssSelector("button[data-hrl-bo='" + key + "']");
        int tries = 0;

        while (true) {
            List<WebElement> found = driver.findElements(btn);
            if (!found.isEmpty() && found.get(0).isDisplayed() && found.get(0).isEnabled()) {
                WebElement day = found.get(0);
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center',inline:'center'});", day);
                day.click();
                save(snapName);
                return;
            }

            if (!canPageForward || tries++ > 12) break;
            List<WebElement> nexts = driver.findElements(ARROW_FORWARD);
            if (!nexts.isEmpty() && nexts.get(0).isDisplayed() && nexts.get(0).isEnabled()) {
                nexts.get(0).click();
                sleep(250);
            } else {
                break;
            }
        }
        save("fail-" + snapName);
        Assert.fail("Calendar day not clickable: " + key);
    }

    private void click(By locator, String snapName) throws IOException {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center',inline:'center'});", el);
        el.click();
        save(snapName);
    }

    private void waitUrlContains(String part, String snapName) throws IOException {
        boolean ok = wait.until(d -> d != null && d.getCurrentUrl().contains(part));
        save(ok ? snapName : ("fail-" + snapName));
        Assert.assertTrue(ok, "Timed out waiting for URL to contain: " + part);
    }

    private void waitVisible(By locator, String snapName) throws IOException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        save(snapName);
    }

    private void closeOverlays() throws IOException {
        String js =
            "try{document.querySelectorAll('.ReactModal__Overlay,.modal,[role=\"dialog\"]').forEach(function(x){x.style.display='none';x.style.pointerEvents='none';});}catch(e){};" +
            "try{document.querySelectorAll('iframe').forEach(function(f){f.style.pointerEvents='none';});}catch(e){};";
        ((JavascriptExecutor) driver).executeScript(js);
        save("popups-closed");
    }

    private void save(String name) throws IOException {
        File dir = new File("target");
        if (!dir.exists()) dir.mkdirs();
        // screenshot
        byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Files.write(new File(dir, name + ".png").toPath(), png);
        // html
        String html = driver.getPageSource();
        Files.write(new File(dir, name + ".html").toPath(), html.getBytes(StandardCharsets.UTF_8));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
