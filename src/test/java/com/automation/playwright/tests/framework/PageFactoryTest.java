package com.automation.playwright.tests.framework;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.lang.reflect.Field;
import java.util.List;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.pages.BasePage;
import com.automation.playwright.pages.CartPage;
import com.automation.playwright.pages.CheckoutCompletePage;
import com.automation.playwright.pages.CheckoutInfoPage;
import com.automation.playwright.pages.CheckoutOverviewPage;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.pages.JsonPage;
import com.automation.playwright.pages.LoginPage;
import com.automation.playwright.pages.ProductPage;
import com.automation.playwright.pages.factory.Css;
import com.automation.playwright.pages.factory.PageFactory;
import com.automation.playwright.pages.factory.TestId;
import com.automation.playwright.pages.factory.Text;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Checks the framework's own PageFactory: every annotated locator of every
 * page is filled, an annotation finds the real element, and a field the
 * factory cannot fill fails at once with a clear message (not later with a
 * NullPointerException in a test).
 */
@Test(groups = "framework")
public class PageFactoryTest extends UiTest {

	/**
	 * Creates every page object and checks that each annotated Locator field
	 * was filled, including the ones inherited from ShopPage.
	 *
	 * @throws IllegalAccessException never; the fields are made accessible first.
	 */
	@Test(description = "Every page gets all its annotated locators")
	public void everyPageGetsAllItsLocators() throws IllegalAccessException {
		List<BasePage> pages = List.of(new LoginPage(page()), new InventoryPage(page()), new ProductPage(page()),
				new CartPage(page()), new CheckoutInfoPage(page()), new CheckoutOverviewPage(page()),
				new CheckoutCompletePage(page()), new JsonPage(page()));
		int total = 0;
		for (BasePage pageObject : pages) {
			// Running the factory again returns the names of the fields it fills
			List<String> fields = PageFactory.initLocators(pageObject, page());
			Steps.log(pageObject.getClass().getSimpleName() + " locators filled: " + fields.size());
			assertFalse(fields.isEmpty(), pageObject.getClass().getSimpleName() + " should have annotated locators");
			for (Class<?> type = pageObject.getClass(); type != BasePage.class; type = type.getSuperclass()) {
				for (Field field : type.getDeclaredFields()) {
					if (field.getType() == Locator.class) {
						field.setAccessible(true);
						assertNotNull(field.get(pageObject), type.getSimpleName() + "." + field.getName() + " was not filled");
					}
				}
			}
			total += fields.size();
		}
		Steps.log("Locators filled in all pages: " + total);
	}

	/**
	 * A page declared in the test with @Text and @Css: the factory fills
	 * fields of any BasePage subclass, and the locators find the real elements.
	 */
	@Test(description = "Annotated locators find the real elements")
	public void annotatedLocatorsFindTheRealElements() {
		openLoginPage();

		LoginHeader header = new LoginHeader(page());
		assertThat(header.logo).isVisible();
		assertThat(header.inputs).hasCount(2);
	}

	/**
	 * Fields the factory cannot fill are reported when the page is created:
	 * a final field, a field with two annotations, and a field that is not a Locator.
	 */
	@Test(description = "Wrong locator fields fail when the page is created")
	public void wrongLocatorFieldsFailWhenThePageIsCreated() {
		IllegalStateException finalField = expectThrows(IllegalStateException.class, () -> new FinalField(page()));
		IllegalStateException twoAnnotations = expectThrows(IllegalStateException.class, () -> new TwoAnnotations(page()));
		IllegalStateException notALocator = expectThrows(IllegalStateException.class, () -> new NotALocator(page()));

		Steps.log(finalField.getMessage());
		Steps.log(twoAnnotations.getMessage());
		Steps.log(notALocator.getMessage());
		assertTrue(finalField.getMessage().contains("must not be static or final"), finalField.getMessage());
		assertTrue(twoAnnotations.getMessage().contains("more than one locator annotation"), twoAnnotations.getMessage());
		assertEquals(notALocator.getMessage(), "NotALocator.title must be a Locator to carry a locator annotation, not String");
	}

	/**
	 * The login page's logo and inputs, declared only for this test.
	 */
	static class LoginHeader extends BasePage {

		// The "Swag Labs" logo above the form
		@Text(value = "Swag Labs", exact = true)
		Locator logo;
		// The user name and password inputs
		@Css("form input[type=text], form input[type=password]")
		Locator inputs;

		/**
		 * @param page the browser tab.
		 */
		LoginHeader(Page page) {
			super(page);
		}
	}

	/**
	 * Wrong: a final field cannot be filled.
	 */
	static class FinalField extends BasePage {

		// final fields are rejected
		@TestId("title")
		final Locator title = null;

		/**
		 * @param page the browser tab.
		 */
		FinalField(Page page) {
			super(page);
		}
	}

	/**
	 * Wrong: one field, two locator annotations.
	 */
	static class TwoAnnotations extends BasePage {

		// Which one would it be? Rejected
		@TestId("title")
		@Css(".title")
		Locator title;

		/**
		 * @param page the browser tab.
		 */
		TwoAnnotations(Page page) {
			super(page);
		}
	}

	/**
	 * Wrong: an annotated field that is not a Locator.
	 */
	static class NotALocator extends BasePage {

		// Only Locator fields can be filled
		@TestId("title")
		String title;

		/**
		 * @param page the browser tab.
		 */
		NotALocator(Page page) {
			super(page);
		}
	}
}
