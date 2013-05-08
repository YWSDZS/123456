package org.falconia.mangaproxy.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

public final class Regex {

	public static ArrayList<String> match(String pattern, String subject) {
		ArrayList<String> groups = new ArrayList<String>();

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(subject);

		if (m.find()) {
			int count = m.groupCount();
			for (int i = 0; i <= count; i++) {
				groups.add(m.group(i));
			}
		}

		return groups;
	}

	public static HashMap<String, String> matchGroup(String pattern, String[] groupnames, String subject) {
		HashMap<String, String> groups = new HashMap<String, String>();

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(subject);

		if (m.find()) {
			int count = m.groupCount();
			for (int i = 0; i < count; i++) {
				if (!TextUtils.isEmpty(groupnames[i])) {
					groups.put(groupnames[i], m.group(i + 1));
				}
			}
		}

		return groups;
	}

	public static HashMap<String, String> matchGroup(String pattern, String subject) {
		ArrayList<String> groups = Regex.match("^(.+)\\{(.+?)\\}$", pattern);
		pattern = groups.get(1);
		String[] groupnames = split("^\\s*'|'\\s*$|'\\s*,\\s*'", groups.get(2));

		return matchGroup(pattern, groupnames, subject);
	}

	public static String matchString(String pattern, String subject) {
		ArrayList<String> groups = match(pattern, subject);
		switch (groups.size()) {
		case 0:
			return null;
		case 1:
			return groups.get(0);
		default:
			return groups.get(1);
		}
	}

	public static ArrayList<ArrayList<String>> matchAll(String pattern, String subject) {
		ArrayList<ArrayList<String>> matches = new ArrayList<ArrayList<String>>();

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(subject);

		while (m.find()) {
			ArrayList<String> groups = new ArrayList<String>();
			int count = m.groupCount();
			for (int i = 0; i <= count; i++) {
				groups.add(m.group(i));
			}
			matches.add(groups);
		}

		return matches;
	}

	public static ArrayList<ArrayList<String>> matchAll(String pattern, String pattern2, String subject) {
		ArrayList<String> groups = match(pattern, subject);
		if (groups.size() == 0 || (groups.size() > 1 && TextUtils.isEmpty(groups.get(1)))) {
			return new ArrayList<ArrayList<String>>();
		} else if (groups.size() == 1) {
			return matchAll(pattern2, groups.get(0));
		} else {
			return matchAll(pattern2, groups.get(1));
		}
	}

	public static String[] split(String pattern, String subject) {
		String[] result = subject.split(pattern);
		if (result.length > 0 && TextUtils.isEmpty(result[0])) {
			String[] resultFix = new String[result.length - 1];
			System.arraycopy(result, 1, resultFix, 0, resultFix.length);
			return resultFix;
		}
		return result;
	}

	public static boolean isMatch(String pattern, String subject) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(subject);
		return m.find();
	}

}
