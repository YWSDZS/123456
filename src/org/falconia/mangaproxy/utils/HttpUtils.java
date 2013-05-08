package org.falconia.mangaproxy.utils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.falconia.mangaproxy.AppUtils;

import android.text.TextUtils;

public final class HttpUtils {

	public static final String TAG = "HttpUtils";

	public static final String CHARSET_GBK = "GBK";
	public static final String CHARSET_UTF8 = "UTF-8";

	public static String getHost(String url) {
		String host = null;
		try {
			URL link = new URL(url);
			host = link.getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return host;
	}

	public static String joinUrl(String base, String spec) {
		if (TextUtils.isEmpty(base)) {
			return null;
		}
		try {
			URL url;
			if (TextUtils.isEmpty(spec)) {
				url = new URL(base);
			} else if (spec.matches("^http://.+")) {
				url = new URL(spec);
			} else if (base.endsWith("/")) {
				url = new URL(new URL(base), spec);
			} else {
				url = new URL(new URL(base + "/"), spec);
			}
			return url.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			AppUtils.logE(TAG, "Invalid URL.");
		}
		return null;
	}

	public static String urlencode(String url) {
		return urlencode(url, CHARSET_UTF8);
	}

	public static String urlencode(String url, String urlCharset) {
		try {

			Pattern p = Pattern.compile("^(?:https?|ftp)://|[^a-zA-Z0-9=?&/: ~`!@#$%^()+.*_-]+");
			Matcher m = p.matcher(url);

			String urlNew = "";
			int end = 0;

			m.find();
			while (m.find()) {
				urlNew += url.substring(end, m.start());
				urlNew += URLEncoder.encode(m.group(), CHARSET_UTF8);
				end = m.end();
			}
			urlNew += url.substring(end, url.length());
			url = urlNew.replace(" ", "%20");
			AppUtils.logV(TAG, "New URL: " + urlNew);
			AppUtils.logV(TAG, "Host IP: " + InetAddress.getByName(getHost(url)).getHostAddress());

		} catch (Exception e) {
		}
		return url;
	}

	public static void flushDns(String url) {
		try {
			String ttl = System.getProperty("networkaddress.cache.ttl");
			String oldIp = InetAddress.getByName(getHost(url)).getHostAddress();
			System.setProperty("networkaddress.cache.ttl", "0");
			String newIp = InetAddress.getByName(getHost(url)).getHostAddress();
			System.setProperty("networkaddress.cache.ttl", ttl);

			if (!oldIp.equals(newIp)) {
				AppUtils.logD(TAG, "Flush DNS: " + oldIp + " -> " + newIp);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
