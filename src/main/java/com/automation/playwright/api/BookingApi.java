package com.automation.playwright.api;

import java.util.Map;

import com.automation.playwright.api.model.Booking;
import com.automation.playwright.api.model.CreatedBooking;
import com.automation.playwright.utils.Steps;

/**
 * The endpoints of the Restful Booker API, one method each. Methods return
 * the raw {@link ApiResponse}, so tests can check failing statuses too;
 * {@link #createBooking(Booking)} and {@link #token(String, String)} are
 * shortcuts for test data setup that fail fast when the API does not work.
 */
public class BookingApi {

	// The HTTP client of the running test
	private final ApiClient client;

	/**
	 * @param client the HTTP client of the running test.
	 */
	public BookingApi(ApiClient client) {
		this.client = client;
	}

	/**
	 * @return ApiResponse the health check: 201 "Created" when the API is up.
	 */
	public ApiResponse ping() {
		return client.get("/ping");
	}

	/**
	 * Asks for an auth token. Note the API's quirk: wrong credentials still
	 * return 200, with {"reason":"Bad credentials"} instead of a token.
	 *
	 * @param user          the user name.
	 * @param password      the password.
	 * @return ApiResponse  the response.
	 */
	public ApiResponse auth(String user, String password) {
		return client.send("POST", "/auth", Map.of("username", user, "password", password), Map.of(), Map.of());
	}

	/**
	 * @param user      the user name.
	 * @param password  the password.
	 * @return String   a token for update and delete requests.
	 * @throws IllegalStateException if the API returns no token.
	 */
	public String token(String user, String password) {
		ApiResponse response = auth(user, password);
		String token = response.json().path("token").asText("");
		if (token.isEmpty()) {
			throw new IllegalStateException("No token from /auth: " + response.body());
		}
		return token;
	}

	/**
	 * @param booking       the booking to create.
	 * @return ApiResponse  200 with the new id and the stored booking.
	 */
	public ApiResponse create(Booking booking) {
		return client.send("POST", "/booking", booking, Map.of(), Map.of());
	}

	/**
	 * Creates a booking for test data setup.
	 *
	 * @param booking  the booking to create.
	 * @return int     the new booking id.
	 * @throws IllegalStateException if the API does not create it.
	 */
	public int createBooking(Booking booking) {
		ApiResponse response = create(booking);
		if (response.status() != 200) {
			throw new IllegalStateException("Could not create a booking: " + response.status() + " " + response.body());
		}
		int id = response.as(CreatedBooking.class).bookingid();
		Steps.log("Test data: booking " + id + " for \"" + booking.firstname() + " " + booking.lastname() + "\"");
		return id;
	}

	/**
	 * @param id            the booking id.
	 * @return ApiResponse  200 with the booking, or 404.
	 */
	public ApiResponse get(int id) {
		return client.get("/booking/" + id);
	}

	/**
	 * @param firstname     the first name to filter by.
	 * @param lastname      the last name to filter by.
	 * @return ApiResponse  200 with a list of {"bookingid": n}.
	 */
	public ApiResponse search(String firstname, String lastname) {
		return client.get("/booking", Map.of("firstname", firstname, "lastname", lastname));
	}

	/**
	 * Replaces a whole booking.
	 *
	 * @param id            the booking id.
	 * @param booking       the new booking.
	 * @param token         an auth token; null sends none.
	 * @return ApiResponse  200 with the stored booking, or 403 without a valid token.
	 */
	public ApiResponse update(int id, Booking booking, String token) {
		return client.send("PUT", "/booking/" + id, booking, tokenCookie(token), Map.of());
	}

	/**
	 * Changes only the given fields of a booking.
	 *
	 * @param id            the booking id.
	 * @param fields        the fields to change, e.g. {"firstname": "Jim"}.
	 * @param token         an auth token; null sends none.
	 * @return ApiResponse  200 with the stored booking, or 403 without a valid token.
	 */
	public ApiResponse patch(int id, Map<String, Object> fields, String token) {
		return client.send("PATCH", "/booking/" + id, fields, tokenCookie(token), Map.of());
	}

	/**
	 * Deletes a booking. Note the API's quirk: success is 201 "Created".
	 *
	 * @param id            the booking id.
	 * @param token         an auth token; null sends none.
	 * @return ApiResponse  201 when deleted, 403 without a valid token.
	 */
	public ApiResponse delete(int id, String token) {
		return client.send("DELETE", "/booking/" + id, null, tokenCookie(token), Map.of());
	}

	/**
	 * @param token  an auth token, or null.
	 * @return Map   the Cookie header Restful Booker expects, or no header.
	 */
	private static Map<String, String> tokenCookie(String token) {
		return token == null ? Map.of() : Map.of("Cookie", "token=" + token);
	}
}
