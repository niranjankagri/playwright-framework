package com.automation.playwright.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Test settings read from {@code config.properties} on the classpath.
 * A system property with the same key wins, so any setting can be changed
 * from the command line, e.g. {@code -Dbrowser=firefox -Dheadless=false}.
 */
public final class Config {

	// Name of the settings file on the test classpath
	private static final String FILE = "config.properties";
	// Loaded once; the settings do not change during a run
	private static final Config INSTANCE = new Config();

	// Values from the settings file
	private final Properties properties = new Properties();

	// Use get()
	private Config() {
		try (InputStream in = Config.class.getClassLoader().getResourceAsStream(FILE)) {
			if (in == null) {
				throw new IllegalStateException(FILE + " not found on the classpath");
			}
			properties.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + FILE, e);
		}
	}

	/**
	 * @return Config the shared settings.
	 */
	public static Config get() {
		return INSTANCE;
	}

	/**
	 * @param key       the setting name.
	 * @return String   the system property with that name, else the file value.
	 * @throws IllegalStateException if the setting is in neither place.
	 */
	public String value(String key) {
		String value = System.getProperty(key, properties.getProperty(key));
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing setting '" + key + "' in " + FILE);
		}
		return value.trim();
	}

	/**
	 * @return String the web shop URL without a trailing slash.
	 */
	public String baseUrl() {
		return value("baseUrl").replaceAll("/+$", "");
	}

	/**
	 * @return String the password of the Sauce Demo users.
	 */
	public String password() {
		return value("password");
	}

	/**
	 * @return String the REST API URL without a trailing slash.
	 */
	public String apiUrl() {
		return value("apiUrl").replaceAll("/+$", "");
	}

	/**
	 * @return String the API admin user name.
	 */
	public String apiUser() {
		return value("apiUser");
	}

	/**
	 * @return String the API admin password.
	 */
	public String apiPassword() {
		return value("apiPassword");
	}

	/**
	 * @return String chromium, firefox or webkit.
	 */
	public String browser() {
		return value("browser");
	}

	/**
	 * @return boolean true to run the browser without a window.
	 */
	public boolean headless() {
		return Boolean.parseBoolean(value("headless"));
	}

	/**
	 * @return double milliseconds Playwright pauses after each action (0 = no pause).
	 */
	public double slowMo() {
		return Double.parseDouble(value("slowMo"));
	}

	/**
	 * @return double the timeout for page actions and auto-waiting assertions, in milliseconds.
	 */
	public double timeoutMillis() {
		return Double.parseDouble(value("timeoutMillis"));
	}

	/**
	 * @return boolean true to record a trace for every UI test (kept for failures only).
	 */
	public boolean trace() {
		return Boolean.parseBoolean(value("trace"));
	}

	/**
	 * @return boolean true to record a video for every UI test (kept for failures only).
	 */
	public boolean video() {
		return Boolean.parseBoolean(value("video"));
	}
}
