package com.automation.playwright.tests.interview;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.config.Config;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.pages.LoginPage;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;

/**
 * Common Playwright interview tasks, solved on the Sauce Demo shop: locator
 * strategies, keyboard input, tabs, saved logins, JavaScript, screenshots,
 * dialogs, waiting for a response and checking images. Each test is a
 * short, complete answer that can be shown and explained.
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
		openLoginPage();

		Map<String, Locator> strategies = Map.of(
				"getByTestId", page().getByTestId("username"),
				"getByPlaceholder", page().getByPlaceholder("Username"),
				"CSS", page().locator("#user-name"),
				"XPath", page().locator("//input[@name='user-name']"),
				"getByRole", page().getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")));
		strategies.forEach((strategy, locator) -> {
			Steps.log("Find the user name field with " + strategy);
			// Exactly one match, and it is the same element (its data-test value)
			assertThat(locator).hasCount(1);
			assertThat(locator).hasAttribute("data-test", "username");
		});
		assertThat(page().getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"))).isEnabled();
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
		loginAs(TestData.STANDARD_USER);

		Steps.log("Open a second tab in the same context and go to the product list");
		Page second = page().context().newPage();
		try {
			second.navigate("/inventory.html");
			assertThat(second.getByTestId("inventory-item")).hasCount(6);
			assertEquals(page().context().pages().size(), 2, "Tabs open in the context");
		} finally {
			second.close();
		}
	}

	/**
	 * Saves the login (cookies and local storage) with storageState() and
	 * starts a new, separate context from it: that context is logged in at
	 * once. Suites use this to log in once and skip the login form after.
	 */
	@Test(description = "A saved login skips the login form")
	public void savedLoginSkipsTheLoginForm() {
		loginAs(TestData.STANDARD_USER);
		Steps.log("Save the storage state (cookies + local storage)");
		String state = page().context().storageState();

		Browser browser = page().context().browser();
		Steps.log("Start a new context from the saved state and open the product list");
		try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setBaseURL(Config.get().baseUrl()).setStorageState(state))) {
			Page fresh = context.newPage();
			fresh.navigate("/inventory.html");
			assertThat(fresh.getByTestId("inventory-item")).hasCount(6);
		}
	}

	/**
	 * Runs JavaScript in the page with evaluate(): reads the document state
	 * and the cart the shop keeps in local storage.
	 */
	@Test(description = "JavaScript reads the page and its local storage")
	public void javascriptReadsThePageAndItsStorage() {
		loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK);

		Object readyState = page().evaluate("() => document.readyState");
		Object cart = page().evaluate("() => localStorage.getItem('cart-contents')");
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
	 * unless a handler is registered; this one reads the text and accepts.
	 */
	@Test(description = "A browser dialog is read and accepted")
	public void browserDialogIsReadAndAccepted() {
		openLoginPage();
		AtomicReference<String> message = new AtomicReference<>();
		page().onceDialog(dialog -> {
			message.set(dialog.message());
			dialog.accept();
		});

		Steps.log("Open a confirm() dialog from JavaScript");
		Object answer = page().evaluate("() => confirm('Leave the shop?')");
		assertEquals(message.get(), "Leave the shop?", "Dialog text");
		assertEquals(answer, true, "confirm() returns true when accepted");
	}

	/**
	 * Waits for a network response caused by an action: waitForResponse()
	 * starts listening, runs the action, and returns the first matching response.
	 */
	@Test(description = "The login loads the product images")
	public void loginLoadsTheProductImages() {
		LoginPage login = openLoginPage();

		Response image = page().waitForResponse(response -> response.url().endsWith(".jpg"),
				() -> login.loginAs(TestData.STANDARD_USER));
		Steps.log("First product image: " + image.status() + " " + image.url());
		assertEquals(image.status(), 200, "Status of " + image.url());
	}

	/**
	 * Checks every product image: the standard user's images are all different
	 * and every address answers 200 (checked with the page's own request
	 * client, which shares the browser's cookies). The problem user gets the
	 * same wrong picture on every product.
	 */
	@Test(description = "Product images are not broken, except for the problem user")
	public void productImagesAreNotBrokenExceptForTheProblemUser() {
		List<String> images = loginAs(TestData.STANDARD_USER).imageSources();
		assertEquals(images.stream().distinct().count(), 6L, "Different images for 6 products: " + images);
		for (String src : images) {
			APIResponse response = page().request().get(Config.get().baseUrl() + src);
			Steps.log(response.status() + " " + src);
			assertTrue(response.ok(), "Image " + src + " answered " + response.status());
			response.dispose();
		}

		List<String> problemImages = new InventoryPage(page()).logout().loginAs(TestData.PROBLEM_USER).imageSources();
		Steps.log("Images of the problem user: " + problemImages.stream().distinct().toList());
		assertEquals(problemImages.stream().distinct().count(), 1L, "The problem user sees one picture everywhere");
	}
}
