package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Checkout step one: the buyer's name and postal code ({@code /checkout-step-one.html}).
 */
public class CheckoutInfoPage extends ShopPage {

	// Form fields and the Continue button
	private final Locator firstName = byTestId("firstName");
	private final Locator lastName = byTestId("lastName");
	private final Locator postalCode = byTestId("postalCode");
	private final Locator continueButton = byTestId("continue");
	// Cancel goes back to the cart
	private final Locator cancel = byTestId("cancel");
	// Red banner, e.g. "Error: First Name is required"
	private final Locator error = byTestId("error");

	/**
	 * @param page the browser tab this page works on.
	 */
	public CheckoutInfoPage(Page page) {
		super(page);
	}

	/**
	 * Waits for the first-name field of the buyer form.
	 */
	public void waitUntilLoaded() {
		assertThat(firstName).isVisible();
	}

	/**
	 * Fills in the form and clicks Continue, without checking the outcome.
	 *
	 * @param first             the first name; empty leaves it empty.
	 * @param last              the last name; empty leaves it empty.
	 * @param zip               the postal code; empty leaves it empty.
	 * @return CheckoutInfoPage this page, for checking the error banner.
	 */
	public CheckoutInfoPage submit(String first, String last, String zip) {
		Steps.log("Enter \"" + first + "\", \"" + last + "\", \"" + zip + "\" and continue");
		firstName.fill(first);
		lastName.fill(last);
		postalCode.fill(zip);
		continueButton.click();
		return this;
	}

	/**
	 * Fills in the form and continues to the order overview.
	 *
	 * @param first                 the first name.
	 * @param last                  the last name.
	 * @param zip                   the postal code.
	 * @return CheckoutOverviewPage the overview.
	 */
	public CheckoutOverviewPage continueWith(String first, String last, String zip) {
		submit(first, last, zip);
		CheckoutOverviewPage overview = new CheckoutOverviewPage(page);
		overview.waitUntilLoaded();
		return overview;
	}

	/**
	 * @return CartPage the cart, after clicking Cancel.
	 */
	public CartPage cancel() {
		Steps.log("Cancel the checkout");
		cancel.click();
		CartPage cart = new CartPage(page);
		cart.waitUntilLoaded();
		return cart;
	}

	/**
	 * @return Locator the error banner.
	 */
	public Locator error() {
		return error;
	}
}
