package com.automation.playwright.pages;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Parent of all page objects. Playwright waits for every element by itself
 * (actionability checks) and its assertions retry until they pass, so pages
 * need no explicit waits; this class only holds what every page shares:
 * browser navigation, JavaScript and dialogs.
 */
public abstract class BasePage {

	// The browser tab this page works on
	protected final Page page;

	/**
	 * The text of a JavaScript dialog and the answer the page got.
	 *
	 * @param message  the text the dialog showed.
	 * @param answer   what the page's script received, e.g. true for an accepted confirm().
	 */
	public record DialogResult(String message, Object answer) {
	}

	/**
	 * @param page the browser tab this page works on.
	 */
	protected BasePage(Page page) {
		this.page = page;
	}

	/**
	 * @return Page the browser tab, for checks with {@code assertThat(page)}.
	 */
	public Page page() {
		return page;
	}

	/**
	 * Presses the browser's Back button.
	 */
	public void goBack() {
		Steps.log("Press the browser's Back button");
		page.goBack();
	}

	/**
	 * Reloads the current page.
	 */
	public void reload() {
		Steps.log("Reload the page");
		page.reload();
	}

	/**
	 * @return String the page's document.readyState, read with JavaScript.
	 */
	public String readyState() {
		return (String) page.evaluate("() => document.readyState");
	}

	/**
	 * Opens a confirm() dialog from JavaScript and accepts it. Playwright
	 * dismisses dialogs by itself unless a handler is registered; this one
	 * handles only the next dialog.
	 *
	 * @param question      the dialog text.
	 * @return DialogResult the text shown and the value confirm() returned.
	 */
	public DialogResult confirmAndAccept(String question) {
		Steps.log("Open a confirm() dialog \"" + question + "\" and accept it");
		AtomicReference<String> message = new AtomicReference<>();
		page.onceDialog(dialog -> {
			message.set(dialog.message());
			dialog.accept();
		});
		Object answer = page.evaluate("question => confirm(question)", question);
		return new DialogResult(message.get(), answer);
	}

	/**
	 * @return int how many tabs are open in this page's browser context.
	 */
	public int openTabs() {
		return page.context().pages().size();
	}

	/**
	 * Closes this tab.
	 */
	public void close() {
		page.close();
	}

	/**
	 * @param testId  the element's data-test value, e.g. "login-button".
	 * @return Locator a lazy locator; nothing is searched until it is used.
	 */
	protected Locator byTestId(String testId) {
		return page.getByTestId(testId);
	}

	/**
	 * @param locator the elements to read.
	 * @return List   the trimmed text of every match, in page order.
	 */
	protected List<String> texts(Locator locator) {
		return locator.allInnerTexts().stream().map(String::trim).toList();
	}

	/**
	 * @param priceText a price as shown, e.g. "$29.99" or "Item total: $39.98".
	 * @return double   the number, e.g. 29.99.
	 */
	protected static double money(String priceText) {
		return Double.parseDouble(priceText.substring(priceText.indexOf('$') + 1).trim());
	}
}
