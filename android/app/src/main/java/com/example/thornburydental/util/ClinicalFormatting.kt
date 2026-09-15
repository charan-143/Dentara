package com.example.thornburydental.util

import java.util.Calendar

/**
 * Computes age in years from a "YYYY-MM-DD" date-of-birth string.
 * Falls back to 38 if the string can't be parsed (matches historical behavior
 * of the call sites this was extracted from — TodayQueueScreen/ScheduleScreen).
 */
fun calculateAge(dobStr: String): Int {
    return try {
        val parts = dobStr.split("-").map { it.toInt() }
        val birthYear = parts[0]
        val birthMonth = parts[1] - 1
        val birthDay = parts[2]

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthYear

        val currentMonth = today.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }
        if (age < 0) 0 else age
    } catch (_: Exception) {
        38
    }
}

/**
 * Parses a time string like "9:30 AM", "09:30 AM", or bare 24-hour "14:00"
 * into minutes-since-midnight, for chronological sorting/comparison.
 *
 * Appointment times are stored as free-form 12-hour strings (e.g. "02:00 PM"),
 * and a plain lexicographic sort of that text is NOT chronological — e.g.
 * "02:00 PM" sorts before "11:30 AM" as text even though 11:30 AM is earlier
 * in the day. Sort by this function's return value instead.
 *
 * Returns [Int.MAX_VALUE] for anything unparseable so malformed entries sort
 * last rather than throwing.
 */
fun parseTimeToMinutes(timeStr: String): Int {
    return try {
        val cleaned = timeStr.trim().uppercase()
        val isPm = cleaned.contains("PM")
        val isAm = cleaned.contains("AM")
        val digitsPart = cleaned.replace("AM", "").replace("PM", "").trim()
        val parts = digitsPart.split(":")
        var hour = parts[0].trim().toInt()
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        if (isAm || isPm) {
            if (hour == 12) hour = 0
            if (isPm) hour += 12
        }
        (hour * 60 + minute).coerceIn(0, 23 * 60 + 59)
    } catch (_: Exception) {
        Int.MAX_VALUE
    }
}

/**
 * Formats a time string as "hh:mm AM/PM". If [timeStr] already carries an AM/PM
 * suffix — the normal case, since appointment times are stored pre-formatted
 * (e.g. "09:30 AM") — it is returned unchanged rather than re-parsed as 24-hour
 * time, which would corrupt it (e.g. "09:30 AM" -> "09:30 AM AM").
 */
fun formatTimeWithAmPm(timeStr: String): String {
    return try {
        val cleanTime = timeStr.trim()
        if (cleanTime.uppercase().contains("AM") || cleanTime.uppercase().contains("PM")) {
            return cleanTime
        }
        val parts = cleanTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        String.format("%02d:%s %s", hour12, minute, amPm)
    } catch (_: Exception) {
        timeStr
    }
}

/**
 * Robust input validation and filtering utilities for Thornbury Dental.
 * Enforces strict phone number and email validation rules to prevent
 * arbitrary strings (e.g. "parrot") or invalid emails (missing "@", missing domain).
 */
object ValidationUtils {

    /**
     * Standard RFC-compliant email regex:
     * - Requires local part
     * - Requires '@'
     * - Requires domain with dot and minimum 2-letter TLD (e.g. .com, .org, .co.uk)
     * - No whitespace or invalid characters
     */
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Characters allowed in phone inputs: digits, +, -, (, ), and spaces.
     */
    private val PHONE_ALLOWED_CHARS_REGEX = Regex("^[0-9+\\-\\s()]+$")

    /**
     * Returns true if [email] is non-blank and strictly matches standard email format.
     */
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        return EMAIL_REGEX.matches(trimmed)
    }

    /**
     * Returns true if [phone] contains valid phone characters and between 7 and 15 actual digits
     * (matching ITU-T E.164 international recommendations and standard national formats).
     * Rejects words (like "parrot"), alphabets, and numbers with fewer than 7 or more than 15 digits.
     */
    fun isValidPhone(phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return false
        if (!PHONE_ALLOWED_CHARS_REGEX.matches(trimmed)) return false
        val digitCount = trimmed.count { it.isDigit() }
        return digitCount in 7..15
    }

    /**
     * Filters live user input to only allow valid phone characters (digits, +, -, (, ), and space),
     * preventing alphabetic characters like 'p', 'a', 'r', 'r', 'o', 't' from being typed.
     */
    fun filterPhoneInput(input: String): String {
        return input.filter { it.isDigit() || it in "+-() " }
    }
}
