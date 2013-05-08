package org.falconia.mangaproxy.utils;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.TimeZone;

public final class TimeUtils {

	public static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

	public static int year2to4(int year) {
		if (year > 100) {
			throw new InvalidParameterException("Invalid year digits.");
		}
		return year < 50 ? year + 2000 : year + 1900;
	}

	public static int getCalendarOffset(Calendar calendar1, Calendar calendar2, int field) {
		return calendar2.get(field) - calendar1.get(field);
	}

}
