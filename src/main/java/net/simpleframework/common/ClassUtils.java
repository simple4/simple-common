package net.simpleframework.common;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ClassUtils {
	public static Class<?> forName(final String className) throws ClassNotFoundException {
		if (className.endsWith("[]")) {
			final String elementClassName = className.substring(0, className.length() - 2);
			return Array.newInstance(forName(elementClassName), 0).getClass();
		} else {
			Class<?> clazz;
			try {
				clazz = Class.forName(className);
			} catch (final ClassNotFoundException ex) {
				final int p = className.lastIndexOf('.');
				if (p > -1) {
					clazz = Class.forName(className.substring(0, p) + "$" + className.substring(p + 1));
				} else {
					throw ex;
				}
			}
			return clazz;
		}
	}

	public static Object invoke(final Method method, final Object obj, final Object... args)
			throws IllegalArgumentException {
		try {
			return method.invoke(obj, args);
		} catch (final IllegalAccessException e) {
			throw ClassException.of(e);
		} catch (final InvocationTargetException e) {
			throw ClassException.of(e);
		}
	}

	public static Object newInstance(final String className) {
		try {
			return forName(className).newInstance();
		} catch (final Exception e) {
			throw ClassException.of(e);
		}
	}

	public static <T> T newInstance(final Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (final Exception e) {
			throw ClassException.of(e);
		}
	}

	public static Object newInstance(final String className, final Class<?>[] parameterTypes,
			final Object[] objs) {
		try {
			return forName(className).getConstructor(parameterTypes).newInstance(objs);
		} catch (final Exception e) {
			throw ClassException.of(e);
		}
	}

	public static InputStream getResourceAsStream(final String resourcePath) {
		return getResourceAsStream(resourcePath, (Class<?>) null);
	}

	public static InputStream getResourceAsStream(final String resourcePath,
			final Class<?>... loaderClasses) {
		InputStream inputStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(resourcePath);
		if (inputStream == null) {
			inputStream = ClassUtils.class.getClassLoader().getResourceAsStream(resourcePath);
		}
		if (loaderClasses != null && inputStream == null) {
			ClassLoader cl;
			for (final Class<?> loaderClass : loaderClasses) {
				if (loaderClass != null && (cl = loaderClass.getClassLoader()) != null) {
					inputStream = cl.getResourceAsStream(resourcePath);
					if (inputStream != null) {
						break;
					}
				}
			}
		}
		return inputStream;
	}

	/**
	 * 缓存资源为null的路径
	 */
	private static Set<String> resourceNull = new HashSet<String>();

	public static InputStream getResourceAsStream(final Class<?> packageClass,
			final String resourceName) {
		if (packageClass == null) {
			return null;
		}
		final String packageName = packageClass.getPackage().getName();
		final String resourcePath = packageName + "." + resourceName;
		if (resourceNull.contains(resourcePath)) {
			return null;
		}
		final InputStream inputStream = getResourceAsStream(
				StringUtils.replace(packageName, ".", "/") + "/" + resourceName, packageClass);
		if (inputStream == null) {
			resourceNull.add(resourcePath);
		}
		return inputStream;
	}

	public static InputStream getResourceRecursively(final Class<?> packageClass,
			final String filename) {
		if (!StringUtils.hasText(filename) || packageClass == null) {
			return null;
		}
		final InputStream inputStream = getResourceAsStream(packageClass, filename);
		if (inputStream != null) {
			return inputStream;
		} else {
			return getResourceRecursively(packageClass.getSuperclass(), filename);
		}
	}

	public static Class<?>[] getAllInterfaces(final Class<?> clazz) {
		final HashSet<Class<?>> set = new HashSet<Class<?>>();
		Class<?> superClazz = clazz;
		while (superClazz != null) {
			for (final Class<?> interfaceClazz : superClazz.getInterfaces()) {
				set.add(interfaceClazz);
			}
			superClazz = superClazz.getSuperclass();
		}
		return set.toArray(new Class<?>[set.size()]);
	}

	public static Object getFieldValue(final String fieldname, final Object o)
			throws NoSuchFieldException {
		try {
			final Field field = o.getClass().getDeclaredField(fieldname);
			field.setAccessible(true);
			return field.get(o);
		} catch (final NoSuchFieldException e) {
			throw e;
		} catch (final Exception e) {
			throw ClassException.of(e);
		}
	}

	public static void scanResources(final String packageName,
			final IScanResourcesCallback... callbacks) throws IOException {
		scanResources(packageName, true, callbacks);
	}

	public static void scanResources(final String packageName, final boolean recursive,
			final IScanResourcesCallback... callbacks) throws IOException {
		final String packageDirName = packageName.replace('.', '/');
		final Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader()
				.getResources(packageDirName);
		while (dirs.hasMoreElements()) {
			final URL url = dirs.nextElement();
			final String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				final String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				findAndAddClassesInPackageByFile(packageDirName, filePath, recursive, callbacks);
			} else if ("jar".equals(protocol)) {
				final JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
				final Enumeration<JarEntry> entries = jar.entries();
				// 目录包缓存
				final Set<String> packs = new HashSet<String>();
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.charAt(0) == '/') {
						name = name.substring(1);
					}
					if (!name.startsWith(packageDirName)) {
						continue;
					}
					if (!recursive) {
						final int idx = name.lastIndexOf('/');
						if (idx == -1) {
							continue;
						}
						final String packageDirName2 = name.substring(0, idx);
						if (packs.contains(packageDirName2)) {
							continue;
						}
						if (entry.isDirectory() && !packageDirName2.equals(packageDirName)) {
							packs.add(packageDirName2);
							continue;
						}
					}
					if (callbacks != null) {
						for (final IScanResourcesCallback callback : callbacks) {
							callback.doResources(name, entry.isDirectory());
						}
					}
				}
			}
		}
	}

	static void findAndAddClassesInPackageByFile(final String packageName, final String packagePath,
			final boolean recursive, final IScanResourcesCallback... callbacks) throws IOException {
		final File dir = new File(packagePath);
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		final File[] dirfiles = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return (recursive && file.isDirectory()) || file.isFile();
			}
		});
		for (final File file : dirfiles) {
			final String packageName2 = packageName + "/" + file.getName();
			final boolean isDirectory = file.isDirectory();
			if (callbacks != null) {
				for (final IScanResourcesCallback callback : callbacks) {
					callback.doResources(packageName2, isDirectory);
				}
			}
			if (isDirectory) {
				findAndAddClassesInPackageByFile(packageName2, file.getAbsolutePath(), recursive,
						callbacks);
			}
		}
	}

	public static interface IScanResourcesCallback {

		/**
		 * 扫描处理回调
		 * 
		 * @param filename
		 */
		void doResources(String filepath, boolean isDirectory) throws IOException;
	}

	public static abstract class ScanClassResourcesCallback extends ObjectEx implements
			IScanResourcesCallback {

		private final ClassLoader cl = IScanResourcesCallback.class.getClassLoader();

		protected Class<?> loadClass(final String filepath) {
			if (!filepath.endsWith(".class")) {
				return null;
			}
			try {
				return cl.loadClass(filepath.substring(0, filepath.lastIndexOf(".")).replace('/', '.'));
			} catch (final ClassNotFoundException e) {
			} catch (final NoClassDefFoundError e) {
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		protected <T> T newInstance(final Class<?> tClass, final Class<T> iClass) {
			if (tClass == null || tClass.isInterface() || Modifier.isAbstract(tClass.getModifiers())) {
				return null;
			}
			if (!iClass.isAssignableFrom(tClass)) {
				return null;
			}
			T t = null;
			try {
				t = (T) tClass.getMethod("singleton", Class.class).invoke(null, tClass);
			} catch (final NoSuchMethodException e) {
				try {
					t = (T) tClass.newInstance();
				} catch (final Exception e1) {
					log.error(e1);
				}
			} catch (final Exception e) {
				log.error(e);
			}
			return t;
		}
	}
}
