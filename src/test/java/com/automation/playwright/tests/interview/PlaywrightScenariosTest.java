package com.automation.playwright.tests.interview;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;
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

/**
 * Common Playwright interview tasks, solved on the Sauce Demo shop: locator
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
			// Exactly one match, and it is the same element (its data-test value)
			assertThat(strategy.getValue()).hasCount(1);
			assertThat(strategy.getValue()).hasAttribute("data-test", "username");
		}
		assertThat(login.loginButtonByRole()).isEnabled();
	}

	/**
	 * Logs in with the keyboard only (focus, type, Tab, Enter).
	 */
	@Test(description = "Login works with the keyboard only")
	public void loginWorksWithTheKeyboardOnly() {
		InventoryPage inventory = openLoginPage().loginWithKeyboard(TestData.STANDARD_USER);

		assertThat(inventory.items()).hasCount(6);
	}

	/**
	 * Opens a second tab in the same browser context: tabs of one context share
	 * cookies, so the second tab is logged in without logging in again.
	 */
	@Test(description = "A second tab shares the login")
	public void secondTabSharesTheLogin() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);

		InventoryPage secondTab = inventory.openInNewTab();
		try {
			assertThat(secondTab.items()).hasCount(6);
			assertEquals(inventory.openTabs(), 2, "Tabs open in the context");
		} finally {
			secondTab.close();
		}
	}

	/**
	 * Saves the login (cookies and local storage) with storageState() and
	 * starts a new, separate context from it: that context is logged in at
	 * once. Suites use this to log in once and skip the login form after.
	 */
	@Test(description = "A saved login skips the login form")
	public void savedLoginSkipsTheLoginForm() {
		String savedLogin = loginAs(TestData.STANDARD_USER).savedLogin();

		Steps.log("Start a new context from the saved login and open the product list");
		try (BrowserContext context = BrowserManager.newContextWith(savedLogin)) {
			InventoryPage inventory = new InventoryPage(context.newPage()).open();
			assertThat(inventory.items()).hasCount(6);
		}
	}

	/**
	 * Runs JavaScript in the page with evaluate(): reads the document state
	 * and the cart the shop keeps in local storage.
	 */
	@Test(description = "JavaScript reads the page and its local storage")
	public void javascriptReadsThePageAndItsStorage() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK);

		String readyState = inventory.readyState();
		String cart = inventory.cartInStorage();
		Steps.log("document.readyState = " + readyState + ", cart-contents = " + cart);
		assertEquals(readyState, "complete", "document.readyState");
		// The shop stores the product ids in the cart; the backpack is id 4
		assertEquals(cart, "[4]", "Cart in local storage");
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
	@Test(description = "The login loads the product images")
	public void loginLoadsTheProductImages() {
		Response image = openLoginPage().loginAndWaitForFirstImage(TestData.STANDARD_USER);

		Steps.log("First product image: " + image.status() + " " + image.url());
		assertEquals(image.status(), 200, "Status of " + image.url());
	}

	/**
	 * Checks every product image: the standard user's images are all different
	 * and every address answers 200. The problem user gets the same wrong
	 * picture on every product.
	 */
	@Test(description = "Product images are not broken, except for the problem user")
	public void productImagesAreNotBrokenExceptForTheProblemUser() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);
		Map<String, Integer> statuses = inventory.imageStatuses();
		assertEquals(statuses.size(), 6, "Different images for 6 products: " + statuses.keySet());
		statuses.forEach((src, status) -> assertEquals(status.intValue(), 200, "Status of " + src));

		List<String> problemImages = inventory.logout().loginAs(TestData.PROBLEM_USER).imageSources();
		Steps.log("Images of the problem user: " + problemImages.stream().distinct().toList());
		assertEquals(problemImages.stream().distinct().count(), 1L, "The problem user sees one picture everywhere");
	}
}
