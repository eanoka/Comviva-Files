package com.grameenphone.wipro.utility.common;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.sw.BufferingXmlWriter;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil {
	public static String escapeForXml(String source) throws IOException {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferingXmlWriter writer = new BufferingXmlWriter(stringWriter, WriterConfig.createFullDefaults(), "UTF-8", false, null, 16);
			writer.writeCharacters(source);
			writer.close(true);
			return stringWriter.toString();
		} catch (IOException h) {
			if(h.getMessage().startsWith("Invalid white space character")) {
				return escapeForXml(source.replaceAll("\\p{C}", ""));
			}
			throw h;
		}
	}

	public static boolean isNullOrEmpty(String toCheck) {
		return toCheck == null || toCheck.trim().length() == 0;
	}

	public static boolean hasText(String toCheck) {
		return !isNullOrEmpty(toCheck);
	}

	/**
	 * @param msisdn
	 * @return 10 digit msisdn if format is valid
	 */
	public static String sanitizeMsisdn(String msisdn) {
		if (msisdn == null) {
			return null;
		}
		if (!msisdn.matches("^(((\\+|00)?88)?0)?1[13-9](-*\\d){8}$")) {
			return null;
		}
		return msisdn.replace("-", "").substring(msisdn.length() - 10);
	}

	public static String escapeForLikeExpression(String exp) {
		if (exp == null) {
			return null;
		}
		return exp.replaceAll("([%\\\\_])", "\\\\$1");
	}

	public static int hmTos(String hm) {
		char lastChar = hm.charAt(hm.length() - 1);
		if(lastChar > 47 && lastChar < 58) {
			return Integer.parseInt(hm);
		}
		int refValue = Integer.parseInt(hm.substring(0, hm.length() - 1));
		if(lastChar == 'm') {
			return refValue * 60;
		}
		if(lastChar == 'h') {
			return refValue * 60 * 60;
		}
		return refValue;
	}

	public static long toMilli(String hourMinSec) {
		return toSecond(hourMinSec) * 1000;
	}

	public static long toSecond(String hourMinSec) {
		String[] parts = hourMinSec.trim().split("\\s+");
		long resultantMillis = 0;
		for(String a : parts) {
			if(a.length() == 0) {
				continue;
			}
			char last = a.charAt(a.length() - 1);
			int number = a.length() == 1 ? 0 : Integer.parseInt(a.substring(0, a.length() - 1));
			long factor = 1;
			switch(last) {
				case 'h':
					factor *= 60;
				case 'm':
					factor *= 60;
				case 's':
					factor *= 1;
					break;
				default:
					number = number * 10 + (last - 48);
			}
			resultantMillis += number * factor;
		}
		return resultantMillis;
	}

	public static String toGbMbKb(long bytes) {
		long remBytes = bytes % 1024;
		long kbs = (bytes - remBytes) / 1024;
		String returnText = remBytes == 0 ? "" : (remBytes + "B");
		if(kbs == 0) {
			return returnText;
		}
		long remKbs = kbs % 1024;
		long mbs = (kbs - remKbs) / 1024;
		returnText = (remKbs == 0 ? "" : (remKbs + "KB")) + (returnText.length() == 0 ? "" : (" " + returnText));
		if(mbs == 0) {
			return returnText;
		}
		long remMbs = mbs % 1024;
		long gbs = (mbs - remMbs) / 1024;
		returnText = (remMbs == 0 ? "" : (remMbs + "MB")) + (returnText.length() == 0 ? "" : (" " + returnText));
		if(gbs == 0) {
			return returnText;
		}
		return gbs + "GB" + (returnText.length() == 0 ? "" : (" " + returnText));
	}

	public static Long toLong(String longString) {
		if(longString == null) {
			return null;
		}
		try {
			return Long.parseLong(longString);
		} catch (Exception e) {
			return null;
		}
	}

	public static String generateUniqueReference(String billType) {
		StringBuilder sb = new StringBuilder();
		sb.append(billType);
		sb.append(String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
		sb.append(("" + System.currentTimeMillis()).substring(3));
		sb.append(String.format("%03d", System.nanoTime() % 1000));
		return sb.toString();
	}

	public static String milliTohms(long milli) {
		if (milli == 0) {
			return "0 ms";
		}
		long millis = milli % 1000;
		long remInSec = (milli - millis) / 1000;
		long seconds = remInSec % 60;
		long minutes = (remInSec - seconds) / 60;
		String result = "";
		if (minutes != 0) {
			result += " " + minutes + "min";
		}
		if (seconds != 0) {
			result += " " + seconds + "sec";
		}
		if (millis != 0) {
			result += " " + millis + "ms";
		}
		return result.substring(1);
	}

	public static Map deserializeUrlParamsToMap(String urlParam) throws UnsupportedEncodingException {
		Map toReturn = new LinkedHashMap();
		String regex = "([^=]+)=([^&]+)?&?";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(urlParam);
		while(matcher.find()) {
			String key = matcher.group(1);
			Object value = matcher.group(2);
			if(value == null) {
				continue;
			}
			value = URLDecoder.decode((String) value, "UTF-8");
			if(toReturn.containsKey(key)) {
				List listValue = new ArrayList();
				listValue.add(toReturn.get(key));
				listValue.add(value);
				value = listValue;
			}
			toReturn.put(key, value);
		}
		return toReturn;
	}

	public static String getNonEmpty(String... values) {
		for(String value : values) {
			if(hasText(value)) {
				return value;
			}
		}
		return null;
	}

	public static String joinNullSafe(String delimiter, String... values) {
		return Stream.of(values).map(value -> null == value ? "" : value).collect(Collectors.joining(delimiter));
	}
}