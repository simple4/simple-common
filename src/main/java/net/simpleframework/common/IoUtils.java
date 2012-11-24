package net.simpleframework.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class IoUtils {
	static final int BUFFER = 8 * 1024;

	public static String getStringFromInputStream(final InputStream inputStream) throws IOException {
		return getStringFromInputStream(inputStream, "UTF-8");
	}

	public static String getStringFromInputStream(final InputStream inputStream,
			final String charsetName) throws IOException {
		return getStringFromReader(new InputStreamReader(inputStream, charsetName));
	}

	public static String getStringFromReader(final Reader reader) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(reader);
			final StringWriter sw = new StringWriter();
			final PrintWriter writer = new PrintWriter(sw);
			String s;
			while ((s = br.readLine()) != null) {
				writer.println(s);
			}
			writer.flush();
			return sw.toString();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	public static String[] getStringsFromReader(final Reader reader) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(reader);
			final List<String> l = new ArrayList<String>();
			String s;
			while ((s = br.readLine()) != null) {
				l.add(s);
			}
			return l.toArray(new String[l.size()]);
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	public static boolean createDirectoryRecursively(File directory) {
		if (directory == null) {
			return false;
		} else if (directory.exists()) {
			return !directory.isFile();
		} else if (!directory.isAbsolute()) {
			directory = new File(directory.getAbsolutePath());
		}
		final String parent = directory.getParent();
		if ((parent == null) || !createDirectoryRecursively(new File(parent))) {
			return false;
		}
		directory.mkdir();
		return directory.exists();
	}

	public static File createFile(final File file) throws IOException {
		if (!file.exists()) {
			createDirectoryRecursively(file.getParentFile());
			file.createNewFile();
		}
		return file;
	}

	public static void copyFile(final File from, final File to) throws IOException {
		final InputStream inputStream = new BufferedInputStream(new FileInputStream(from));
		copyFile(inputStream, to);
	}

	public static void copyFile(final InputStream inputStream, final File to) throws IOException {
		createFile(to);
		OutputStream outputStream = null;
		try {
			copyStream(inputStream, outputStream = new BufferedOutputStream(new FileOutputStream(to)));
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}

	private static long kb = 1024, mb = kb * 1024, gb = mb * 1024;

	public static String toFileSize(final long size) {
		if (size < 0) {
			return "";
		} else if (size > gb) {
			final double d = (double) size / (double) gb;
			return Convert.toString(NumberUtils.formatDouble(d)) + " GB";
		} else if (size > mb) {
			final double d = (double) size / (double) mb;
			return Convert.toString(NumberUtils.formatDouble(d)) + " MB";
		} else if (size > kb) {
			final double d = (double) size / (double) kb;
			return Convert.toString(NumberUtils.formatDouble(d)) + " KB";
		} else {
			return Convert.toString(size) + " B";
		}
	}

	public static long toFileSize(String size) {
		if (size != null) {
			size = size.toUpperCase();
			final StringBuilder sb = new StringBuilder();
			final StringBuilder sb2 = new StringBuilder();
			for (final char c : size.toCharArray()) {
				if (c == 'B' || c == 'K' || c == 'M' || c == 'G') {
					sb2.append(c);
					continue;
				}
				sb.append(c);
			}
			char c;
			if (sb2.length() == 0 || (c = sb2.charAt(0)) == 'B') {
				return Convert.toLong(sb.toString());
			} else {
				final double l = Convert.toDouble(sb.toString());
				if (c == 'G') {
					return (long) (gb * l);
				} else if (c == 'M') {
					return (long) (mb * l);
				} else if (c == 'K') {
					return (long) (kb * l);
				}
			}
		}
		return 0;
	}

	public static long sizeOfDirectory(final File directory) {
		if (!directory.exists()) {
			return 0l;
		}

		if (!directory.isDirectory()) {
			return directory.length();
		}

		long size = 0;
		final File[] files = directory.listFiles();
		if (files == null) {
			return 0;
		}
		for (int i = 0; i < files.length; i++) {
			final File file = files[i];
			if (file.isDirectory()) {
				size += sizeOfDirectory(file);
			} else {
				size += file.length();
			}
		}
		return size;
	}

	public static int copyStream(final InputStream inputStream, final OutputStream outputStream)
			throws IOException {
		if (inputStream == null || outputStream == null) {
			return 0;
		}
		int result = 0;
		final byte[] buf = new byte[BUFFER];
		for (;;) {
			final int numRead = inputStream.read(buf);
			if (numRead == -1) {
				break;
			}
			outputStream.write(buf, 0, numRead);
			result += numRead;
		}
		outputStream.flush();
		return result;
	}

	private static Method getHardwareAddressMethod;
	static {
		try {
			getHardwareAddressMethod = NetworkInterface.class.getMethod("getHardwareAddress");
		} catch (final NoSuchMethodException e) {
		}
	}

	public static byte[] getMacAddressBytes() throws IOException {
		final NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
		if (getHardwareAddressMethod != null) {
			return (byte[]) ClassUtils.invoke(getHardwareAddressMethod, ni);
		}
		return MacAddress.getMacAddress().getBytes();
	}

	public static void deleteAll(final File dir) throws IOException {
		deleteAll(dir, true);
	}

	public static void deleteAll(final File dir, final boolean self) throws IOException {
		if (!dir.exists()) {
			return;
		}
		if (!dir.isDirectory()) {
			dir.delete();
			return;
		}
		final String[] list = dir.list();
		if (list != null) {
			for (final String element : list) {
				final File child = new File(dir, element);
				deleteAll(child);
			}
		}
		if (self) {
			dir.delete();
		}
	}

	/*----------------------------- zip utils ----------------------------*/

	public static void unzip(final InputStream in, final String target) throws IOException {
		unzip(in, target, true);
	}

	public static void unzip(final InputStream in, final String target, final boolean rewrite)
			throws IOException {
		unzip(in, target, rewrite, new IUnZipHandle() {

			@Override
			public void doFile(final ZipInputStream is, final File destFile) throws IOException {
				final BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(
						destFile));
				try {
					copyStream(is, oStream);
				} finally {
					if (oStream != null) {
						oStream.close();
					}
				}
			}
		});
	}

	public static void unzip(final InputStream in, String target, final boolean rewrite,
			final IUnZipHandle unzipHandle) throws IOException {
		if (target.charAt(target.length() - 1) != File.separatorChar) {
			target += File.separatorChar;
		}
		ZipInputStream is;
		if (in instanceof ZipInputStream) {
			is = (ZipInputStream) in;
		} else {
			is = new ZipInputStream(new BufferedInputStream(in));
		}
		try {
			ZipEntry entry;
			while ((entry = is.getNextEntry()) != null) {
				final String entryName = entry.getName();
				final int index = entryName.lastIndexOf("/");
				if (index > 0) {
					createDirectoryRecursively(new File(target + entryName.substring(0, index)));
				}
				if (entry.isDirectory()) {
					continue;
				}

				final File destFile = new File(target + entryName);
				if (rewrite || !destFile.exists()) {
					unzipHandle.doFile(is, destFile);
				}
			}
		} finally {
			if (is != null) {
				in.close();
			}
		}
	}

	public static interface IUnZipHandle {

		void doFile(ZipInputStream is, File destFile) throws IOException;
	}
}
