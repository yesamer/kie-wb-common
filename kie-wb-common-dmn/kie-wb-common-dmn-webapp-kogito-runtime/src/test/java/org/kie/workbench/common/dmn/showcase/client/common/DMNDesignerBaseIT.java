/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.dmn.showcase.client.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.assertj.XmlAssert;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public class DMNDesignerBaseIT {

    private static final Logger LOG = LoggerFactory.getLogger(DMNDesignerBaseIT.class);

    private static final String SET_CONTENT_TEMPLATE =
            "gwtEditorBeans.get('DMNDiagramEditor').get().setContent('', `%s`)";

    private static final String GET_CONTENT_TEMPLATE =
            "return gwtEditorBeans.get('DMNDiagramEditor').get().getContent()";

    private static final String INDEX_HTML = "target/kie-wb-common-dmn-webapp-kogito-runtime/index.html";

    private static final String INDEX_HTML_PATH = "file:///" + new File(INDEX_HTML).getAbsolutePath();

    private static final String DECISION_NAVIGATOR_EXPAND = "qe-docks-item-W-org.kie.dmn.decision.navigator";

    private static final String PROPERTIES_PANEL = "qe-docks-item-E-DiagramEditorPropertiesScreen";

    private static final Boolean HEADLESS = Boolean.valueOf(System.getProperty("org.kie.dmn.kogito.browser.headless"));

    private static final String SCREENSHOTS_DIR = System.getProperty("org.kie.dmn.kogito.screenshots.dir");

    private WebDriver driver;

    protected WebElement decisionNavigatorExpandButton;

    protected WebElement propertiesPanel;

    @BeforeClass
    public static void setupClass() {
        WebDriverManager.firefoxdriver().setup();
    }

    @Before
    public void openDMNDesigner() {
        driver = new FirefoxDriver(getFirefoxOptions());
        driver.get(INDEX_HTML_PATH);
        driver.manage().window().maximize();

        waitDMNDesignerElements();
    }

    private FirefoxOptions getFirefoxOptions() {
        final FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setHeadless(HEADLESS);
        return firefoxOptions;
    }

    @Rule
    public TestWatcher takeScreenShotAndCleanUp = new TestWatcher() {
        @Override
        protected void failed(final Throwable e,
                              final Description description) {
            saveScreenShot(description);
        }

        @Override
        protected void finished(final Description description) {
            quitDriver();
        }
    };

    protected void resetPage() {
        quitDriver();
        openDMNDesigner();
    }

    private void quitDriver() {
        getDriver().ifPresent(WebDriver::quit);
    }

    private Optional<WebDriver> getDriver() {
        return Optional.ofNullable(driver);
    }

    private void waitDMNDesignerElements() {
        decisionNavigatorExpandButton = waitOperation()
                .withMessage("Presence of decision navigator expand button is prerequisite for all tests")
                .until(visibilityOfElementLocated(className(DECISION_NAVIGATOR_EXPAND)));

        propertiesPanel = waitOperation()
                .withMessage("Presence of properties panel expand button is prerequisite for all tests")
                .until(visibilityOfElementLocated(className(PROPERTIES_PANEL)));
    }

    private final File screenshotDirectory = initScreenshotDirectory();

    /**
     * Use this for loading DMN model placed in src/test/resources
     * @param filePath relative path of the file
     * @return Text content of the file
     */
    protected String loadResource(final String filePath) throws IOException {
        final InputStream stream = this.getClass().getResourceAsStream(filePath);
        return String.join("", IOUtils.readLines(stream, StandardCharsets.UTF_8));
    }

    protected void setContent(final String xml) {
        ((JavascriptExecutor) driver).executeScript(String.format(SET_CONTENT_TEMPLATE, xml));
        waitOperation()
                .withMessage("Designer was not loaded")
                .until(visibilityOfElementLocated(className("uf-multi-page-editor")));
    }

    protected String getContent() {
        final Object result = ((JavascriptExecutor) driver).executeScript(GET_CONTENT_TEMPLATE);
        assertThat(result).isInstanceOf(String.class);
        return (String) result;
    }

    protected WebDriverWait waitOperation() {
        return new WebDriverWait(driver, Duration.ofSeconds(10).getSeconds());
    }

    protected void executeDMNTestCase(final String directory,
                                      final String file,
                                      final String logMessage) throws IOException {
        final List<String> ignoredAttributes = asList("id", "dmnElementRef");

        LOG.trace(logMessage);
        setContent(loadResource(directory + "/" + file));

        final String actual = getContent();
        assertThat(actual).isNotBlank();
        final String expected = loadResource(directory + "-expected/" + file);

        XmlAssert.assertThat(actual)
                .and(expected)
                .ignoreComments()
                .ignoreWhitespace()
                .withAttributeFilter(attr -> !ignoredAttributes.contains(attr.getName()))
                .areSimilar();
    }

    protected void saveScreenShot(final String... prefixes) {

        final File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        final List<String> fileNameParts = new ArrayList<>(asList(prefixes));
        final String filename = String.join("_", fileNameParts);

        try {
            copyFile(screenshotFile, new File(screenshotDirectory, filename + ".png"));
        } catch (IOException ioe) {
            LOG.error("Unable to take screenshot", ioe);
        }
    }

    private void saveScreenShot(final Description description) {
        final String testClassName = description.getTestClass().getSimpleName();
        final String testMethodName = description.getMethodName();
        saveScreenShot(testClassName, testMethodName);
    }

    private File initScreenshotDirectory() {
        if (SCREENSHOTS_DIR == null) {
            throw new IllegalStateException(
                    "Property org.kie.dmn.kogito.screenshots.dir (where screenshot taken by WebDriver will be put) was null");
        }
        File scd = new File(SCREENSHOTS_DIR);
        if (!scd.exists()) {
            boolean mkdirSuccess = scd.mkdir();
            if (!mkdirSuccess) {
                throw new IllegalStateException("Creation of screenshots dir failed " + scd);
            }
        }
        if (!scd.canWrite()) {
            throw new IllegalStateException("The screenshotDir must be writable" + scd);
        }
        return scd;
    }
}
