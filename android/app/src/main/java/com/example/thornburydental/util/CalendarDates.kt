package com.example.thornburydental.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Small helpers for working with appointment dates, stored as ISO "yyyy-MM-dd"
 * strings (matching the convention already used for patient DOB elsewhere in
 * this app). Backs the Schedule tab's week-strip calendar.
 */
private const val ISO_DATE_PATTERN = "yyyy-MM-dd"

private fun isoDateFormat(): SimpleDateFormat =
    SimpleDateFormat(ISO_DATE_PATTERN, Locale.getDefault()).apply { isLenient = false }

/** Today's date as an ISO "yyyy-MM-dd" string. */
fun todayIsoDate(): String = isoDateFormat().format(Calendar.getInstance().time)

/** Parses an ISO date string into a Calendar, falling back to "now" if unparseable/blank. */
private fun isoDateToCalendar(date: String): Calendar {
    val cal = Calendar.getInstance()
    if (date.isNotBlank()) {
        try {
            isoDateFormat().parse(date)?.let { cal.time = it }
        } catch (e: Exception) {
            // fall through with cal left at "now"
        }
    }
    return cal
}

/** Adds (or subtracts, for negative [days]) whole days to an ISO date string. */
fun addDaysToIsoDate(date: String, days: Int): String {
    val cal = isoDateToCalendar(date)
    cal.add(Calendar.DAY_OF_MONTH, days)
    return isoDateFormat().format(cal.time)
}

/** The 7 ISO dates (Monday through Sunday) of the week containing [date]. */
fun weekDatesContaining(date: String): List<String> {
    val cal = isoDateToCalendar(date)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 ... SATURDAY=7
    val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
    cal.add(Calendar.DAY_OF_MONTH, diffToMonday)
    return (0..6).map { offset ->
        val weekCal = cal.clone() as Calendar
        weekCal.add(Calendar.DAY_OF_MONTH, offset)
        isoDateFormat().format(weekCal.time)
    }
}

/** Day-of-month number, e.g. 11. */
fun isoDateDayOfMonth(date: String): Int = isoDateToCalendar(date).get(Calendar.DAY_OF_MONTH)

/** 3-letter uppercase weekday label, e.g. "MON". */
fun isoDateWeekdayShortLabel(date: String): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(isoDateToCalendar(date).time).uppercase(Locale.getDefault())

/** Full display label, e.g. "Thursday, 11 September". */
fun isoDateDisplayLabel(date: String): String =
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(isoDateToCalendar(date).time)

/** Short display label for a card/list header, e.g. "Thu, 11 Sep". */
fun isoDateShortDisplayLabel(date: String): String =
    SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(isoDateToCalendar(date).time)

/** Label for the month/year shown above the week strip, e.g. "September 2026". */
fun isoDateMonthYearLabel(date: String): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(isoDateToCalendar(date).time)

fun isIsoDateToday(date: String): Boolean = date == todayIsoDate()
