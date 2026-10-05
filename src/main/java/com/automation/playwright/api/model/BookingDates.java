package com.automation.playwright.api.model;

/**
 * Check-in and check-out of a booking, as ISO dates ("2026-11-01").
 *
 * @param checkin  the arrival date.
 * @param checkout the departure date.
 */
public record BookingDates(String checkin, String checkout) {
}
