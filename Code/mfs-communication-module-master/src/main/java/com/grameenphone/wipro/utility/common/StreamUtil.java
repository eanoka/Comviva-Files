package com.grameenphone.wipro.utility.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtil {
	public static String getContent(InputStream stream) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int data;
		while((data = stream.read()) != -1) {
			outputStream.write(data);
		}
		outputStream.close();
		return new String(outputStream.toByteArray());
	}
}