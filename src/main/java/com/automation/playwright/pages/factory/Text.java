package com.automation.playwright.pages.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fills a {@code Locator} field with {@code page.getByText(value)}.
 * <p>
 * Example: {@code @Text(value = "Thank you for your order!", exact = true) private Locator thanks;}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Text {

	/**
	 * @return String the visible text.
	 */
	String value();

	/**
	 * @return boolean true to match the whole text exactly; false matches a part, ignoring case.
	 */
	boolean exact() default false;
}
