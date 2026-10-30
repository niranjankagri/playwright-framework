package com.automation.playwright.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.CartPage;
import com.automation.playwright.pages.CheckoutCompletePage;
import com.automation.playwright.pages.CheckoutInfoPage;
import com.automation.playwright.pages.CheckoutOverviewPage;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.utils.Steps;

/**
 * Checkout: a full purchase with the price calculation, and the required
 * fields of the buyer form.
 */
@Test(groups = "checkout")
public class CheckoutTest extends UiTest {

	// Money is compared to the cent
	private static final double CENT = 0.001;

	/**
	 * Buys two products and checks the overview maths: the item total is the
	 * sum of the list prices, and the total is item total plus tax. The order
	 * confirmation must follow and the cart must be empty afterwards.
	 */
	@Test(groups = "smoke", description = "[TC-112] A full purchase adds up and completes")
	public void tc112_fullPurchaseAddsUpAndCompletes() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);
		double expectedItemTotal = inventory.priceOf(TestData.BACKPACK) + inventory.priceOf(TestData.BOLT_SHIRT);
		inventory.addToCart(TestData.BACKPACK).addToCart(TestData.BOLT_SHIRT);

		CheckoutOverviewPage overview = inventory.openCart().checkout().continueWith("Play", "Wright", "10115");
		assertEquals(overview.productNames(), List.of(TestData.BACKPACK, TestData.BOLT_SHIRT), "Products in the order");
		Steps.log("Item total " + overview.itemTotal() + ", tax " + overview.tax() + ", total " + overview.total());
		assertEquals(overview.itemTotal(), expectedItemTotal, CENT, "Item total = sum of the list prices");
		assertEquals(overview.total(), overview.itemTotal() + overview.tax(), CENT, "Total = item total + tax");

		CheckoutCompletePage complete = overview.finish();
		assertThat(complete.header()).hasText("Thank you for your order!");
		assertThat(complete.cartBadge()).isHidden();
	}

	/**
	 * Leaves one field of the buyer form empty at a time; the form must name
	 * the missing field and stay on the same step.
	 *
	 * @param caseName  which field is missing, shown in the report.
	 * @param first     the first name to enter.
	 * @param last      the last name to enter.
	 * @param zip       the postal code to enter.
	 * @param expected  the error banner text.
	 */
	@Test(dataProvider = "incompleteCheckouts", dataProviderClass = TestData.class, description = "[TC-113] Checkout needs every buyer field")
	public void tc113_checkoutNeedsEveryBuyerField(String caseName, String first, String last, String zip, String expected) {
		CheckoutInfoPage form = loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK).openCart().checkout();

		form.submit(first, last, zip);
		assertThat(form.error()).hasText(expected);
		assertThat(form.title()).hasText("Checkout: Your Information");
	}

	/**
	 * Cancels the checkout at the buyer form: the shop goes back to the cart
	 * and the products are still in it.
	 */
	@Test(description = "[TC-114] Cancelling the checkout keeps the cart")
	public void tc114_cancellingTheCheckoutKeepsTheCart() {
		CheckoutInfoPage form = loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK).openCart().checkout();

		CartPage cart = form.cancel();
		assertThat(cart.title()).hasText("Your Cart");
		assertThat(cart.items()).hasText(new String[] { TestData.BACKPACK });
		assertThat(cart.cartBadge()).hasText("1");
	}
}
