package com.automation.playwright.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON conversion for API requests and responses, with one shared Jackson
 * mapper. Java records map to and from JSON without annotations.
 */
public final class Json {

	// Thread-safe once configured; unknown fields in a response are ignored
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	// Static helper only
	private Json() {
	}

	/**
	 * @param value    any object, e.g. a Booking record or a Map.
	 * @return String  its JSON text.
	 */
	public static String write(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cannot convert to JSON: " + value, e);
		}
	}

	/**
	 * @param <T>   the target type.
	 * @param json  JSON text.
	 * @param type  the class to map it to, e.g. Booking.class.
	 * @return T    the mapped object.
	 * @throws IllegalArgumentException if the text is not valid JSON for that type.
	 */
	public static <T> T read(String json, Class<T> type) {
		try {
			return MAPPER.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Response is not a " + type.getSimpleName() + ": " + json, e);
		}
	}

	/**
	 * @param json      JSON text.
	 * @return JsonNode a tree to read single fields from, e.g. node.path("token").asText().
	 */
	public static JsonNode tree(String json) {
		try {
			return MAPPER.readTree(json);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Response is not JSON: " + json, e);
		}
	}
}
