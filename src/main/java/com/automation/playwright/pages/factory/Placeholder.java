package com.automation.playwright.pages.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fills a {@code Locator} field with {@code page.getByPlaceholder(value)}.
 * <p>
 * Example: {@code @Placeholder("Username") private Locator username;}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Placeholder {

	/**
	 * @return String the placeholder text of the input.
	 */
	String value();
}
