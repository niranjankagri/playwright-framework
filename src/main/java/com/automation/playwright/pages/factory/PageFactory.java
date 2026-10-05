package com.automation.playwright.pages.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Playwright's counterpart of Selenium's {@code PageFactory.initElements}:
 * fills every {@code Locator} field of a page object that carries a locator
 * annotation ({@link TestId}, {@link Role}, {@link Placeholder}, {@link Text},
 * {@link Css}, {@link XPath}).
 * <p>
 * Selenium needs its PageFactory to create lazy proxies, so elements are found
 * when used and do not go stale. A Playwright {@code Locator} is already lazy
 * (it is resolved again on every action), so this factory only gives the page
 * objects a declarative style: each locator is written once, as an annotation.
 * Locators that depend on a value (e.g. the card of a named product) stay
 * methods.
 * <p>
 * {@code BasePage}'s constructor calls {@link #initLocators(Object, Page)}, so
 * every page is filled before its own field initializers run; those may then
 * build on annotated fields, e.g. {@code images = items.locator("img")}.
 */
public final class PageFactory {

	// The annotations this factory understands
	private static final List<Class<? extends Annotation>> LOCATOR_ANNOTATIONS = List.of(
			TestId.class, Role.class, Placeholder.class, Text.class, Css.class, XPath.class);

	// Static helper only
	private PageFactory() {
	}

	/**
	 * Fills the annotated {@code Locator} fields of a page object and of all
	 * its parent classes.
	 *
	 * @param pageObject the page object to fill.
	 * @param page       the browser tab its locators search in.
	 * @return List      the names of the fields filled, e.g. "LoginPage.username".
	 * @throws IllegalStateException if an annotated field is not a non-final,
	 *                               non-static Locator, or has two locator annotations.
	 */
	public static List<String> initLocators(Object pageObject, Page page) {
		List<String> filled = new ArrayList<>();
		for (Class<?> type = pageObject.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				List<Annotation> annotations = locatorAnnotations(field);
				if (annotations.isEmpty()) {
					continue;
				}
				String name = type.getSimpleName() + "." + field.getName();
				check(field, name, annotations);
				Locator locator = create(page, annotations.get(0));
				try {
					field.setAccessible(true);
					field.set(pageObject, locator);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException("Cannot set " + name, e);
				}
				filled.add(name);
			}
		}
		return filled;
	}

	/**
	 * @param field   a field of a page class.
	 * @return List   the locator annotations on it (normally none or one).
	 */
	private static List<Annotation> locatorAnnotations(Field field) {
		List<Annotation> found = new ArrayList<>();
		for (Class<? extends Annotation> type : LOCATOR_ANNOTATIONS) {
			Annotation annotation = field.getAnnotation(type);
			if (annotation != null) {
				found.add(annotation);
			}
		}
		return found;
	}

	/**
	 * Fails fast, while the page is created, on a field the factory cannot fill.
	 *
	 * @param field        the annotated field.
	 * @param name         Class.field, for the message.
	 * @param annotations  its locator annotations.
	 */
	private static void check(Field field, String name, List<Annotation> annotations) {
		if (annotations.size() > 1) {
			throw new IllegalStateException(name + " has more than one locator annotation: " + annotations);
		}
		if (field.getType() != Locator.class) {
			throw new IllegalStateException(name + " must be a Locator to carry a locator annotation, not " + field.getType().getSimpleName());
		}
		int modifiers = field.getModifiers();
		if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
			throw new IllegalStateException(name + " must not be static or final, so the factory can fill it");
		}
	}

	/**
	 * @param page        the browser tab.
	 * @param annotation  one locator annotation.
	 * @return Locator    the matching Playwright locator.
	 */
	private static Locator create(Page page, Annotation annotation) {
		// instanceof patterns, not a pattern switch: the project compiles for Java 17
		if (annotation instanceof TestId a) {
			return page.getByTestId(a.value());
		}
		if (annotation instanceof Role a) {
			return a.name().isEmpty()
					? page.getByRole(a.value())
					: page.getByRole(a.value(), new Page.GetByRoleOptions().setName(a.name()).setExact(a.exact()));
		}
		if (annotation instanceof Placeholder a) {
			return page.getByPlaceholder(a.value());
		}
		if (annotation instanceof Text a) {
			return page.getByText(a.value(), new Page.GetByTextOptions().setExact(a.exact()));
		}
		if (annotation instanceof Css a) {
			return page.locator(a.value());
		}
		if (annotation instanceof XPath a) {
			return page.locator("xpath=" + a.value());
		}
		throw new IllegalArgumentException("Not a locator annotation: " + annotation);
	}
}
