package com.automation.playwright.tests.api;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.automation.playwright.api.ApiResponse;
import com.automation.playwright.base.ApiTest;
import com.automation.playwright.config.Config;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Restful Booker: the health check and authentication.
 */
@Test(groups = "api")
public class AuthApiTest extends ApiTest {

	/**
	 * The health check answers 201 "Created" (the API's own convention).
	 */
	@Test(groups = "smoke", description = "[TC-201] API health check answers")
	public void tc201_healthCheckAnswers() {
		ApiResponse response = bookings.ping();

		assertEquals(response.status(), 201, "Status of /ping");
	}

	/**
	 * The admin from config.properties gets a token.
	 */
	@Test(description = "[TC-202] Admin gets an auth token")
	public void tc202_adminGetsAToken() {
		ApiResponse response = bookings.auth(Config.get().apiUser(), Config.get().apiPassword());

		assertEquals(response.status(), 200, "Status of /auth");
		assertFalse(response.json().path("token").asText("").isEmpty(), "A token should be returned: " + response.body());
	}

	/**
	 * A wrong password gets no token. The API's quirk, checked on purpose so a
	 * change would be noticed: the status is still 200, and the body says
	 * "Bad credentials".
	 */
	@Test(description = "[TC-203] A wrong password gets no token")
	public void tc203_wrongPasswordGetsNoToken() {
		ApiResponse response = bookings.auth(Config.get().apiUser(), "wrong-password");

		JsonNode body = response.json();
		assertEquals(response.status(), 200, "Status of /auth (the API does not use 401)");
		assertEquals(body.path("reason").asText(), "Bad credentials", "Reason");
		assertTrue(body.path("token").isMissingNode(), "No token should be returned: " + response.body());
	}
}
