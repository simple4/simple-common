package net.simpleframework.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacAddress {
	static private Pattern macPattern = Pattern.compile(".*((:?[0-9a-f]{2}[-:]){5}[0-9a-f]{2}).*",
			Pattern.CASE_INSENSITIVE);

	static final String[] windowsCommand = { "ipconfig", "/all" };

	static final String[] linuxCommand = { "/sbin/ifconfig", "-a" };

	public final static List<String> getMacAddresses() throws IOException {
		final List<String> macAddressList = new ArrayList<String>();

		final BufferedReader reader = getMacAddressesReader();
		for (String line = null; (line = reader.readLine()) != null;) {
			final Matcher matcher = macPattern.matcher(line);
			if (matcher.matches()) {
				macAddressList.add(matcher.group(1).replaceAll("[-:]", ""));
			}
		}
		reader.close();
		return macAddressList;
	}

	public final static String getMacAddress() throws IOException {
		return getMacAddress(0);
	}

	public final static String getMacAddress(final int nicIndex) throws IOException {
		final BufferedReader reader = getMacAddressesReader();
		int nicCount = 0;
		for (String line = null; (line = reader.readLine()) != null;) {
			final Matcher matcher = macPattern.matcher(line);
			if (matcher.matches()) {
				if (nicCount == nicIndex) {
					reader.close();
					return matcher.group(1).replaceAll("[-:]", "");
				}
				nicCount++;
			}
		}
		reader.close();
		return null;
	}

	private static BufferedReader getMacAddressesReader() throws IOException {
		final String[] command;
		final String os = System.getProperty("os.name");

		if (os.startsWith("Windows")) {
			command = windowsCommand;
		} else if (os.startsWith("Linux")) {
			command = linuxCommand;
		} else {
			throw new IOException("Unknown operating system: " + os);
		}
		final Process process = Runtime.getRuntime().exec(command);
		new Thread() {

			@Override
			public void run() {
				try {
					final InputStream errorStream = process.getErrorStream();
					while (errorStream.read() != -1) {
						;
					}
					errorStream.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

		}.start();

		return new BufferedReader(new InputStreamReader(process.getInputStream()));
	}
}
