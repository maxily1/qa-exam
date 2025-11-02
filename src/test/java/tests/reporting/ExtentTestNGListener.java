package tests.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.testng.*;

import java.util.concurrent.ConcurrentHashMap;

public class ExtentTestNGListener implements ITestListener, ISuiteListener {

    private static final ConcurrentHashMap<String, ExtentTest> tests = new ConcurrentHashMap<>();
    private ExtentReports extent;

    @Override
    public void onStart(ISuite suite) {
        extent = ExtentManager.getInstance();
    }

    @Override
    public void onFinish(ISuite suite) {
        if (extent != null) {
            extent.flush();
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        ExtentTest test = extent.createTest(name)
                .assignCategory(result.getTestContext().getSuite().getName());
        tests.put(name, test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        tests.get(result.getMethod().getMethodName()).pass("Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        tests.get(result.getMethod().getMethodName()).fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        tests.get(result.getMethod().getMethodName()).skip("Skipped");
    }
}
