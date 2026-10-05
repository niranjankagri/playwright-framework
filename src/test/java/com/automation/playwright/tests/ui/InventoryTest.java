package com.automation.playwright.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.pages.ProductPage;
import com.automation.playwright.utils.Steps;

/**
 * The product list: sorting and the detail page.
 */
@Test(groups = "inventory")
public class InventoryTest extends UiTest {

	/**
	 * Sorts the products with each option of the drop-down and checks the
	 * order by sorting the shown values in Java: the page's order must equal
	 * the sorted copy.
	 *
	 * @param label   the option's meaning, shown in the report.
	 * @param option  the option value of the select element.
	 */
	@Test(dataProvider = "sortOptions", dataProviderClass = TestData.class, description = "Products are sorted")
	public void productsAreSorted(String label, String option) {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER).sortBy(option);

		if (option.equals("az") || option.equals("za")) {
			List<String> names = inventory.productNames();
			List<String> expected = new ArrayList<>(names);
			expected.sort(option.equals("az") ? Comparator.naturalOrder() : Comparator.reverseOrder());
			Steps.log("Names shown: " + names);
			assertEquals(names, expected, "Product names sorted by " + label);
		} else {
			List<Double> prices = inventory.productPrices();
			List<Double> expected = new ArrayList<>(prices);
			expected.sort(option.equals("lohi") ? Comparator.naturalOrder() : Comparator.reverseOrder());
			Steps.log("Prices shown: " + prices);
			assertEquals(prices, expected, "Product prices sorted by " + label);
		}
	}

	/**
	 * Every product has a name and a positive price.
	 */
	@Test(description = "Every product has a name and a price")
	public void everyProductHasANameAndAPrice() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);

		List<String> names = inventory.productNames();
		List<Double> prices = inventory.productPrices();
		assertEquals(names.size(), 6, "Products shown");
		assertTrue(names.stream().noneMatch(String::isBlank), "Every product should have a name");
		assertTrue(prices.stream().allMatch(price -> price > 0), "Every price should be positive: " + prices);
	}

	/**
	 * Opens a product from the list: the detail page must show the same name
	 * and price, and "Back to products" must return to the list.
	 */
	@Test(groups = "smoke", description = "Product details match the list")
	public void productDetailsMatchTheList() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);
		double listPrice = inventory.priceOf(TestData.BACKPACK);

		ProductPage product = inventory.openProduct(TestData.BACKPACK);
		assertThat(product.name()).hasText(TestData.BACKPACK);
		assertEquals(product.price(), listPrice, "Price on the detail page");

		InventoryPage back = product.backToProducts();
		assertThat(back.items()).hasCount(6);
	}
}
