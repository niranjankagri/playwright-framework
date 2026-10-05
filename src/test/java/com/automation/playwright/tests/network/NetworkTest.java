package com.automation.playwright.tests.network;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.config.Config;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.pages.JsonPage;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Route;

/**
 * Network control, a Playwright strength: blocking requests, watching every
 * response, and answering a request with mocked data instead of the server.
 */
@Test(groups = "network")
public class NetworkTest extends UiTest {

	/**
	 * Blocks every image with page.route(...).abort(). The shop must still
	 * work: all products listed, prices shown, but the images not loaded.
	 */
	@Test(description = "The shop works with images blocked")
	public void shopWorksWithImagesBlocked() {
		AtomicInteger blocked = new AtomicInteger();
		page().onRequestFailed(request -> blocked.incrementAndGet());
		Steps.log("Block every .png, .jpg and .svg request");
		page().route("**/*.{png,jpg,jpeg,svg}", Route::abort);

		InventoryPage inventory = loginAs(TestData.STANDARD_USER);
		assertThat(inventory.items()).hasCount(6);
		// naturalWidth is 0 for an image that never loaded
		int width = inventory.firstImageWidth();
		Steps.log("Requests blocked: " + blocked.get() + ", first product image width: " + width);
		assertEquals(width, 0, "The product image should not have loaded");
		assertTrue(blocked.get() > 0, "Some image requests should have been blocked");
	}

	/**
	 * Listens to every response while shopping and fails on any 4xx/5xx
	 * answer or failed request from the shop itself (third-party analytics
	 * are ignored). Requests the browser cancelled itself do not count: when
	 * the page moves on, images still loading are aborted (Firefox reports
	 * NS_BINDING_ABORTED, Chromium net::ERR_ABORTED); that is not a server error.
	 */
	@Test(groups = "smoke", description = "No failing requests while shopping")
	public void noFailingRequestsWhileShopping() {
		String shop = Config.get().baseUrl();
		List<String> failures = new CopyOnWriteArrayList<>();
		AtomicInteger responses = new AtomicInteger();
		page().onResponse(response -> {
			if (response.url().startsWith(shop)) {
				responses.incrementAndGet();
				if (response.status() >= 400) {
					failures.add(response.status() + " " + response.url());
				}
			}
		});
		page().onRequestFailed(request -> {
			if (request.url().startsWith(shop) && !cancelledByBrowser(request.failure())) {
				failures.add(request.failure() + " " + request.url());
			}
		});

		loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK).openCart();
		Steps.log("Responses from the shop: " + responses.get() + ", failures: " + failures.size());
		assertTrue(responses.get() > 0, "The shop should have answered requests");
		assertTrue(failures.isEmpty(), "Failing requests: " + failures);
	}

	/**
	 * @param failure  the error text of a failed request.
	 * @return boolean true if the browser cancelled the request itself.
	 */
	private static boolean cancelledByBrowser(String failure) {
		String text = failure == null ? "" : failure.toLowerCase(Locale.ROOT);
		return text.contains("abort") || text.contains("cancel");
	}

	/**
	 * Answers a request with mocked JSON through route.fulfill(), so the
	 * browser shows data the server never sent. This is how a UI can be
	 * tested against edge cases (empty lists, errors) without changing the
	 * backend.
	 */
	@Test(description = "A mocked response replaces the server's")
	public void mockedResponseReplacesTheServers() {
		String mocked = "{\"firstname\":\"Mocked\",\"lastname\":\"By Playwright\",\"totalprice\":1}";
		Steps.log("Answer every /booking/1 request with mocked JSON");
		page().route("**/booking/1", route -> route.fulfill(new Route.FulfillOptions()
				.setStatus(200).setContentType("application/json").setBody(mocked)));

		JsonPage booking = new JsonPage(page()).open(Config.get().apiUrl() + "/booking/1");
		assertThat(booking.body()).containsText("Mocked");
		assertThat(booking.body()).containsText("By Playwright");
	}
}
