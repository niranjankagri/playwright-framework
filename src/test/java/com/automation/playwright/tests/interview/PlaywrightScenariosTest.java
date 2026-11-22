package com.automation.playwright.tests.interview;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.browser.BrowserManager;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.BasePage.DialogResult;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.pages.LoginPage;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Cookie;

/**
 * Common Playwright interview tasks, solved on the Pattern Shop: locator
 * strategies, keyboard input, tabs, saved logins, JavaScript, screenshots,
 * dialogs, waiting for a response and checking images. Each test is a
 * short, complete answer that can be shown and explained; the Playwright
 * calls live in the page objects, the tests only use pages and check.
 */
@Test(groups = "interview")
public class PlaywrightScenariosTest extends UiTest {

	/**
	 * Finds the user name field in five ways: test id, placeholder, CSS id,
	 * XPath and role. User-facing locators (role, placeholder, text) are
	 * preferred in Playwright; CSS and XPath still work.
	 */
	@Test(description = "Five locator strategies find the same field")
	public void locatorStrategiesFindTheSameField() {
		LoginPage login = openLoginPage();

		for (Map.Entry<String, Locator> strategy : login.usernameByEveryStrategy().entrySet()) {
			Steps.log("Find the user name field with " + strategy.getKey());
			// Exactly one match, and it is the same element (its data-testid value)
			assertThat(strategy.getValue()).hasCount(1);
			assertThat(strategy.getValue()).hasAttribute("data-testid", "username");
		}
		assertThat(login.loginButtonByRole()).isEnabled();
	}

	/**
	 * Logs in with the keyboard only (focus, type, Tab, Enter).
	 */
	@Test(description = "[TC-118] Login works with the keyboard only")
	public void tc118_loginWorksWithTheKeyboardOnly() {
		InventoryPage inventory = openLoginPage().loginWithKeyboard(TestData.STANDARD_USER);

		assertThat(inventory.items()).hasCount(TestData.PRODUCT_COUNT);
	}

	/**
	 * Opens a second tab in the same browser context: tabs of one context share
	 * cookies, so the second tab is logged in without logging in again.
	 */
	@Test(description = "[TC-119] A second tab shares the login")
	public void tc119_secondTabSharesTheLogin() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);

		InventoryPage secondTab = inventory.openInNewTab();
		try {
			assertThat(secondTab.items()).hasCount(TestData.PRODUCT_COUNT);
			assertEquals(inventory.openTabs(), 2, "Tabs open in the context");
		} finally {
			secondTab.close();
		}
	}

	/**
	 * Saves the login (the session cookie) with storageState() and
	 * starts a new, separate context from it: that context is logged in at
	 * once. Suites use this to log in once and skip the login form after.
	 */
	@Test(description = "[TC-120] A saved login skips the login form")
	public void tc120_savedLoginSkipsTheLoginForm() {
		String savedLogin = loginAs(TestData.STANDARD_USER).savedLogin();

		Steps.log("Start a new context from the saved login and open the product list");
		try (BrowserContext context = BrowserManager.newContextWith(savedLogin)) {
			InventoryPage inventory = new InventoryPage(context.newPage()).open();
			assertThat(inventory.items()).hasCount(TestData.PRODUCT_COUNT);
		}
	}

	/**
	 * Runs JavaScript in the page with evaluate(): reads the document state and
	 * the cookies the page can see. The shop's session cookie is HttpOnly: the
	 * browser context has it, but JavaScript in the page cannot read it (so a
	 * script injected into the page could not steal the session).
	 */
	@Test(description = "[TC-121] JavaScript reads the page but not the HttpOnly session cookie")
	public void tc121_javascriptReadsThePageButNotTheSessionCookie() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK);

		String readyState = inventory.readyState();
		String visibleCookies = inventory.cookiesVisibleToJavaScript();
		Cookie session = inventory.sessionCookie().orElseThrow(() -> new AssertionError("No SESSION cookie after login"));
		Steps.log("document.readyState = " + readyState + ", document.cookie = \"" + visibleCookies + "\", SESSION httpOnly = " + session.httpOnly);
		assertEquals(readyState, "complete", "document.readyState");
		assertTrue(session.httpOnly, "The session cookie should be HttpOnly");
		assertFalse(visibleCookies.contains("SESSION"), "document.cookie should not show the session: " + visibleCookies);
	}

	/**
	 * Takes a screenshot of one element and checks it is a real PNG.
	 */
	@Test(description = "An element screenshot is a PNG")
	public void elementScreenshotIsAPng() {
		byte[] png = openLoginPage().formScreenshot();

		// Every PNG file starts with these 8 bytes
		byte[] signature = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
		assertTrue(png.length > 1000, "Screenshot size: " + png.length + " bytes");
		assertEquals(Arrays.copyOf(png, 8), signature, "PNG signature");
	}

	/**
	 * Handles a JavaScript dialog. Playwright dismisses dialogs by itself
	 * unless a handler is registered; the page registers one that reads the
	 * text and accepts.
	 */
	@Test(description = "A browser dialog is read and accepted")
	public void browserDialogIsReadAndAccepted() {
		DialogResult dialog = openLoginPage().confirmAndAccept("Leave the shop?");

		assertEquals(dialog.message(), "Leave the shop?", "Dialog text");
		assertEquals(dialog.answer(), true, "confirm() returns true when accepted");
	}

	/**
	 * Waits for a network response caused by an action: waitForResponse()
	 * starts listening, runs the action, and returns the first matching response.
	 */
	@Test(description = "[TC-122] The login loads the product images")
	public void tc122_loginLoadsTheProductImages() {
		Response image = openLoginPage().loginAndWaitForFirstImage(TestData.STANDARD_USER);

		Steps.log("First product image: " + image.status() + " " + image.url());
		assertEquals(image.status(), 200, "Status of " + image.url());
	}

	/**
	 * Checks every product image: the standard user's images are all different
	 * and every address answers 200. The problem user gets the same wrong
	 * picture on every product.
	 */
	@Test(description = "[TC-123] Product images are not broken, except for the problem user")
	public void tc123_productImagesAreNotBrokenExceptForTheProblemUser() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);
		Map<String, Integer> statuses = inventory.imageStatuses();
		assertEquals(statuses.size(), TestData.PRODUCT_COUNT, "Different images for " + TestData.PRODUCT_COUNT + " products: " + statuses.keySet());
		statuses.forEach((src, status) -> assertEquals(status.intValue(), 200, "Status of " + src));

		List<String> problemImages = inventory.logout().loginAs(TestData.PROBLEM_USER).imageSources();
		Steps.log("Images of the problem user: " + problemImages.stream().distinct().toList());
		assertEquals(problemImages.stream().distinct().count(), 1L, "The problem user sees one picture everywhere");
	}
}
