/**
 * Copyright (C) 2009 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.utils4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Common utility methods for use in Java applications and libraries.
 */
public final class Utils4J {

	private static final String USER_HOME_KEY = "user.home";

	private static final String TEMP_DIR_KEY = "java.io.tmpdir";

	/**
	 * The difference between the Windows epoch (1601-01-01 00:00:00) and the
	 * Unix epoch (1970-01-01 00:00:00) in milliseconds: 11644473600000L.
	 */
	private static final long EPOCH_DIFF = 11644473600000L;

	/**
	 * Used building output as Hex.
	 */
	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Private default constructor.
	 */
	private Utils4J() {
		throw new UnsupportedOperationException(
				"This utility class is not intended to be instanciated!");
	}

	/**
	 * Returns the package path of a class.
	 * 
	 * @param clasz
	 *            Class to determine the path for - Cannot be <code>null</code>.
	 * 
	 * @return Package path for the class.
	 */
	public static String getPackagePath(final Class clasz) {
		checkNotNull("clasz", clasz);
		return clasz.getPackage().getName().replace('.', '/');
	}

	/**
	 * Get the path to a resource located in the same package as a given class.
	 * 
	 * @param clasz
	 *            Class with the same package where the resource is located -
	 *            Cannot be <code>null</code>.
	 * @param name
	 *            Filename of the resource - Cannot be <code>null</code>.
	 * 
	 * @return Resource URL.
	 */
	public static URL getResource(final Class clasz, final String name) {
		checkNotNull("clasz", clasz);
		checkNotNull("name", name);
		final String nameAndPath = "/" + getPackagePath(clasz) + "/" + name;
		return clasz.getResource(nameAndPath);
	}

	/**
	 * Load properties from classpath.
	 * 
	 * @param clasz
	 *            Class in the same package as the properties file - Cannot be
	 *            <code>null</code>.
	 * @param filename
	 *            Name of the properties file (without path) - Cannot be
	 *            <code>null</code>.
	 * 
	 * @return Properties.
	 */
	public static Properties loadProperties(final Class clasz,
			final String filename) {
		checkNotNull("clasz", clasz);
		checkNotNull("filename", filename);

		final String path = getPackagePath(clasz);
		final String resPath = "/" + path + "/" + filename;
		try {
			final Properties props = new Properties();
			final InputStream inStream = clasz.getResourceAsStream(resPath);
			if (inStream == null) {
				throw new IllegalArgumentException("Resource '" + resPath
						+ "' was not found!");
			}
			try {
				props.load(inStream);
			} finally {
				inStream.close();
			}
			return props;
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Load properties from a file.
	 * 
	 * @param file
	 *            Properties file - Cannot be <code>null</code> and must be a
	 *            valid file.
	 * 
	 * @return Properties.
	 */
	public static Properties loadProperties(final File file) {
		checkNotNull("file", file);
		checkValidFile(file);
		try {
			final Properties props = new Properties();
			final InputStream inStream = new FileInputStream(file);
			try {
				props.load(inStream);
			} finally {
				inStream.close();
			}
			return props;
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Check if the argument is an existing file. If the check fails an
	 * <code>IllegalArgumentException</code> is thrown.
	 * 
	 * @param file
	 *            File to check - Cannot be <code>null</code>.
	 */
	public static void checkValidFile(final File file) {
		checkNotNull("file", file);
		if (!file.exists()) {
			throw new IllegalArgumentException("The file '" + file
					+ "' does not exist!");
		}
		if (!file.isFile()) {
			throw new IllegalArgumentException("The name '" + file
					+ "' is not a file!");
		}
	}

	/**
	 * Check if the argument is an existing directory. If the check fails an
	 * <code>IllegalArgumentException</code> is thrown.
	 * 
	 * @param dir
	 *            Directory to check - Cannot be <code>null</code>.
	 */
	public static void checkValidDir(final File dir) {
		checkNotNull("dir", dir);
		if (!dir.exists()) {
			throw new IllegalArgumentException("The directory '" + dir
					+ "' does not exist!");
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("The name '" + dir
					+ "' is not a directory!");
		}
	}

	/**
	 * Save properties to a file.
	 * 
	 * @param file
	 *            Destination file - Cannot be <code>null</code> and parent
	 *            directory must exist.
	 * @param props
	 *            Properties to save - Cannot be <code>null</code>.
	 * @param comment
	 *            Comment for the file.
	 */
	public static void saveProperties(final File file, final Properties props,
			final String comment) {
		checkNotNull("file", file);
		checkNotNull("props", props);

		if (!file.getParentFile().exists()) {
			throw new IllegalArgumentException("The parent directory '"
					+ file.getParentFile() + "' does not exist [file='" + file
					+ "']!");
		}
		try {
			final OutputStream outStream = new FileOutputStream(file);
			try {
				props.store(outStream, comment);
			} finally {
				outStream.close();
			}
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Create an instance with Class.forName(..) and wrap all exceptions into
	 * RuntimeExceptions.
	 * 
	 * @param className
	 *            Full qualified class name - Cannot be <code>null</code>.
	 * 
	 * @return New instance of the class.
	 */
	public static Object createInstance(final String className) {
		checkNotNull("className", className);
		try {
			final Class clasz = Class.forName(className);
			return clasz.newInstance();
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException("Unknown class!", e);
		} catch (final InstantiationException e) {
			throw new RuntimeException("Error instanciating class!", e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException("Error accessing class!", e);
		}
	}

	/**
	 * Adds an URL to the classpath.
	 * 
	 * @param url
	 *            URL to add - Cannot be <code>null</code>.
	 */
	public static void addToClasspath(final String url) {
		checkNotNull("url", url);
		try {
			addToClasspath(new URL(url));
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Checks if the array or URLs contains the given URL.
	 * 
	 * @param urls
	 *            Array of URLs - Cannot be <code>null</code>.
	 * @param url
	 *            URL to find - Cannot be <code>null</code>.
	 * 
	 * @return If the URL is in the array TRUE else FALSE.
	 */
	public static boolean containsURL(final URL[] urls, final URL url) {
		checkNotNull("urls", urls);
		checkNotNull("url", url);
		for (int i = 0; i < urls.length; i++) {
			final URL element = urls[i];
			final String elementStr = element.toExternalForm();
			final String urlStr = url.toExternalForm();
			if (elementStr.equals(urlStr)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates an MD5 hash from a file.
	 * 
	 * @param file
	 *            File to create an hash for - Cannot be <code>null</code>.
	 * 
	 * @return Hash as text.
	 * 
	 * @deprecated Use <code>createHashMD5</code> instead.
	 */
	public static String createHash(final File file) {
		return createHashMD5(file);
	}

	/**
	 * Creates an MD5 hash from a file.
	 * 
	 * @param file
	 *            File to create an hash for - Cannot be <code>null</code>.
	 * 
	 * @return Hash as text.
	 */
	public static String createHashMD5(final File file) {
		return createHash(file, "MD5");
	}

	/**
	 * Creates a HEX encoded hash from a file.
	 * 
	 * @param file
	 *            File to create a hash for - Cannot be <code>null</code>.
	 * @param algorithm
	 *            Hash algorithm like "MD5" or "SHA" - Cannot be
	 *            <code>null</code>.
	 * 
	 * @return HEX encoded hash.
	 */
	public static String createHash(final File file, final String algorithm) {
		checkNotNull("file", file);
		checkNotNull("algorithm", algorithm);
		try {
			final FileInputStream in = new FileInputStream(file);
			try {
				return createHash(in, algorithm);
			} finally {
				in.close();
			}
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Creates a HEX encoded hash from a stream.
	 * 
	 * @param inputStream
	 *            Stream to create a hash for - Cannot be <code>null</code>.
	 * @param algorithm
	 *            Hash algorithm like "MD5" or "SHA" - Cannot be
	 *            <code>null</code>.
	 * 
	 * @return HEX encoded hash.
	 */
	public static String createHash(final InputStream inputStream,
			final String algorithm) {
		checkNotNull("inputStream", inputStream);
		checkNotNull("algorithm", algorithm);
		try {
			final MessageDigest messageDigest = MessageDigest
					.getInstance(algorithm);
			final BufferedInputStream in = new BufferedInputStream(inputStream);
			try {
				final byte[] buf = new byte[1024];
				int count = 0;
				while ((count = in.read(buf)) > -1) {
					messageDigest.update(buf, 0, count);
				}
			} finally {
				in.close();
			}
			return encodeHex(messageDigest.digest());
		} catch (final NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Creates a cipher for encryption or decryption.
	 * 
	 * @param algorithm
	 *            PBE algorithm like "PBEWithMD5AndDES" or
	 *            "PBEWithMD5AndTripleDES".
	 * @param mode
	 *            Encyrption or decyrption.
	 * @param password
	 *            Password.
	 * @param salt
	 *            Salt usable with algorithm.
	 * @param count
	 *            Iterations.
	 * 
	 * @return Ready initialized cipher.
	 * 
	 * @throws GeneralSecurityException
	 *             Error creating the cipher.
	 */
	private static Cipher createCipher(final String algorithm, final int mode,
			final char[] password, final byte[] salt, final int count)
			throws GeneralSecurityException {

		final SecretKeyFactory keyFactory = SecretKeyFactory
				.getInstance(algorithm);
		final PBEKeySpec keySpec = new PBEKeySpec(password);
		final SecretKey key = keyFactory.generateSecret(keySpec);
		final Cipher cipher = Cipher.getInstance(algorithm);
		final PBEParameterSpec params = new PBEParameterSpec(salt, count);
		cipher.init(mode, key, params);
		return cipher;

	}

	/**
	 * Encrypts some data based on a password.
	 * 
	 * @param algorithm
	 *            PBE algorithm like "PBEWithMD5AndDES" or
	 *            "PBEWithMD5AndTripleDES" - Cannot be <code>null</code>.
	 * @param data
	 *            Data to encrypt - Cannot be <code>null</code>.
	 * @param password
	 *            Password - Cannot be <code>null</code>.
	 * @param salt
	 *            Salt usable with algorithm - Cannot be <code>null</code>.
	 * @param count
	 *            Iterations.
	 * 
	 * @return Encrypted data.
	 */
	public static byte[] encryptPasswordBased(final String algorithm,
			final byte[] data, final char[] password, final byte[] salt,
			final int count) {

		checkNotNull("algorithm", algorithm);
		checkNotNull("data", data);
		checkNotNull("password", password);
		checkNotNull("salt", salt);

		try {
			final Cipher cipher = createCipher(algorithm, Cipher.ENCRYPT_MODE,
					password, salt, count);
			return cipher.doFinal(data);
		} catch (final Exception ex) {
			throw new RuntimeException(
					"Error encrypting the password!", ex);
		}
	}

	/**
	 * Decrypts some data based on a password.
	 * 
	 * @param algorithm
	 *            PBE algorithm like "PBEWithMD5AndDES" or
	 *            "PBEWithMD5AndTripleDES" - Cannot be <code>null</code>.
	 * @param encryptedData
	 *            Data to decrypt - Cannot be <code>null</code>.
	 * @param password
	 *            Password - Cannot be <code>null</code>.
	 * @param salt
	 *            Salt usable with algorithm - Cannot be <code>null</code>.
	 * @param count
	 *            Iterations.
	 * 
	 * @return Encrypted data.
	 */
	public static byte[] decryptPasswordBased(final String algorithm,
			final byte[] encryptedData, final char[] password,
			final byte[] salt, final int count) {

		checkNotNull("algorithm", algorithm);
		checkNotNull("encryptedData", encryptedData);
		checkNotNull("password", password);
		checkNotNull("salt", salt);

		try {
			final Cipher cipher = createCipher(algorithm, Cipher.DECRYPT_MODE,
					password, salt, count);
			return cipher.doFinal(encryptedData);
		} catch (final Exception ex) {
			throw new RuntimeException(
					"Error decrypting the password!", ex);
		}
	}

	/**
	 * Creates an URL based on a directory a relative path and a filename.
	 * 
	 * @param baseUrl
	 *            Directory URL with or without slash ("/") at the end of the
	 *            string - Cannot be <code>null</code>.
	 * @param path
	 *            Relative path inside the base URL (with or without slash ("/")
	 *            at the end of the string) - Can be <code>null</code> or an
	 *            empty string.
	 * @param filename
	 *            Filename without path - Cannot be <code>null</code>.
	 * 
	 * @return URL.
	 */
	public static URL createUrl(final URL baseUrl, final String path,
			final String filename) {
		checkNotNull("baseUrl", baseUrl);
		checkNotNull("filename", filename);
		try {
			String baseUrlStr = baseUrl.toString();
			if (!baseUrlStr.endsWith("/")) {
				baseUrlStr = baseUrlStr + "/";
			}
			final String pathStr;
			if ((path == null) || (path.length() == 0)) {
				pathStr = "";
			} else {
				if (path.endsWith("/")) {
					pathStr = path;
				} else {
					pathStr = path + "/";
				}
			}
			return new URL(baseUrlStr + pathStr + filename);
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Returns a relative path based on a base directory. If the
	 * <code>dir</code> is not inside <code>baseDir</code> an
	 * <code>IllegalArgumentException</code> is thrown.
	 * 
	 * @param baseDir
	 *            Base directory the path is relative to - Cannot be
	 *            <code>null</code>.
	 * @param dir
	 *            Directory inside the base directory - Cannot be
	 *            <code>null</code>.
	 * 
	 * @return Path of <code>dir</code> relative to <code>baseDir</code>. If
	 *         both are equal an empty string is returned.
	 */
	public static String getRelativePath(final File baseDir, final File dir) {
		checkNotNull("baseDir", baseDir);
		checkNotNull("dir", dir);
		try {
			final String base = baseDir.getCanonicalPath();
			final String path = dir.getCanonicalPath();
			if (!path.startsWith(base)) {
				throw new IllegalArgumentException("The path '" + path
						+ "' is not inside the base directory '" + base + "'!");
			}
			if (base.equals(path)) {
				return "";
			}
			return path.substring(base.length() + 1);
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Load a file from an directory.
	 * 
	 * @param baseUrl
	 *            Directory URL - Cannot be <code>null</code>.
	 * @param filename
	 *            Filename without path - Cannot be <code>null</code>.
	 * 
	 * @return Properties.
	 */
	public static Properties loadProperties(final URL baseUrl,
			final String filename) {
		return loadProperties(createUrl(baseUrl, "", filename));
	}

	/**
	 * Load a file from an URL.
	 * 
	 * @param fileURL
	 *            Property file URL - Cannot be <code>null</code>.
	 * 
	 * @return Properties.
	 */
	public static Properties loadProperties(final URL fileURL) {
		checkNotNull("fileURL", fileURL);
		try {
			final Properties props = new Properties();
			final InputStream inStream = fileURL.openStream();
			try {
				props.load(inStream);
			} finally {
				inStream.close();
			}
			return props;
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Load a file from an directory. Wraps a possible
	 * <code>MalformedURLException</code> exception into a
	 * <code>RuntimeException</code>.
	 * 
	 * @param baseUrl
	 *            Directory URL as <code>String</code> - Cannot be
	 *            <code>null</code>.
	 * @param filename
	 *            Filename without path - Cannot be <code>null</code>.
	 * 
	 * @return Properties.
	 */
	public static Properties loadProperties(final String baseUrl,
			final String filename) {
		checkNotNull("baseUrl", baseUrl);
		checkNotNull("filename", filename);

		try {
			final URL url = new URL(baseUrl);
			return loadProperties(url, filename);
		} catch (final MalformedURLException ex) {
			// Should be an IllegalArgumentException but 1.4 has no "String,
			// Throwable" constructor...
			throw new RuntimeException(
					"The argument 'srcUrl' is not a valid URL [" + baseUrl
							+ "]!", ex);
		}

	}

	/**
	 * Adds an URL to the classpath.
	 * 
	 * @param url
	 *            URL to add - Cannot be <code>null</code>.
	 */
	public static void addToClasspath(final URL url) {
		checkNotNull("url", url);
		final ClassLoader classLoader = Utils4J.class.getClassLoader();
		if (!(classLoader instanceof URLClassLoader)) {
			throw new IllegalArgumentException("Cannot add '" + url
					+ "' to classloader because it's not an URL classloader");
		}
		final URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
		if (!containsURL(urlClassLoader.getURLs(), url)) {
			try {

				final Method addURL = URLClassLoader.class.getDeclaredMethod(
						"addURL", new Class[] { URL.class });
				addURL.setAccessible(true);
				addURL.invoke(urlClassLoader, new Object[] { url });
			} catch (final NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (final IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (final InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Checks if a variable is not <code>null</code> and throws an
	 * <code>IllegalNullArgumentException</code> if this rule is violated.
	 * 
	 * @param name
	 *            Name of the variable to be displayed in an error message.
	 * @param value
	 *            Value to check for <code>null</code>.
	 */
	public static void checkNotNull(final String name, final Object value) {
		if (value == null) {
			throw new IllegalNullArgumentException(name);
		}
	}

	/**
	 * Checks if a variable is not <code>empty</code> and throws an
	 * <code>IllegalNullArgumentException</code> if this rule is violated. A
	 * String with spaces is NOT considered empty!
	 * 
	 * @param name
	 *            Name of the variable to be displayed in an error message.
	 * @param value
	 *            Value to check for an empty String - Cannot be
	 *            <code>null</code>.
	 */
	public static void checkNotEmpty(final String name, final String value) {
		if (value.length() == 0) {
			throw new IllegalArgumentException("The argument '" + name
					+ "' cannot be empty!!");
		}
	}

	/**
	 * Creates a textual representation of the method.
	 * 
	 * @param returnType
	 *            Return type of the method - Can be <code>null</code>.
	 * @param methodName
	 *            Name of the method - Cannot be <code>null</code>.
	 * @param argTypes
	 *            The list of parameters - Can be <code>null</code>.
	 * 
	 * @return Textual signature of the method.
	 */
	private static String getMethodSignature(final String returnType,
			final String methodName, final Class[] argTypes) {
		final StringBuffer sb = new StringBuffer();
		if (returnType != null) {
			sb.append(returnType);
			sb.append(" ");
		}
		sb.append(methodName);
		sb.append("(");
		if (argTypes != null) {
			for (int i = 0; i < argTypes.length; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(argTypes[i].getName());
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Calls a method with reflection and maps all errors into one exception.
	 * 
	 * @param obj
	 *            The object the underlying method is invoked from - Cannot be
	 *            <code>null</code>.
	 * @param methodName
	 *            Name of the Method - Cannot be <code>null</code>.
	 * @param argTypes
	 *            The list of parameters - May be <code>null</code>.
	 * @param args
	 *            Arguments the arguments used for the method call - May be
	 *            <code>null</code> if "argTypes" is also <code>null</code>.
	 * 
	 * @return The result of dispatching the method represented by this object
	 *         on <code>obj</code> with parameters <code>args</code>.
	 * 
	 * @throws InvokeMethodFailedException
	 *             Invoking the method failed for some reason.
	 */
	public static Object invoke(final Object obj, final String methodName,
			final Class[] argTypes, final Object[] args)
			throws InvokeMethodFailedException {

		checkNotNull("obj", obj);
		checkNotNull("methodName", methodName);

		final Class[] argTypesIntern;
		final Object[] argsIntern;
		if (argTypes == null) {
			argTypesIntern = new Class[] {};
			if (args != null) {
				throw new IllegalArgumentException(
						"The argument 'argTypes' is null but "
								+ "'args' containes values!");
			}
			argsIntern = new Object[] {};
		} else {
			argTypesIntern = argTypes;
			if (args == null) {
				throw new IllegalArgumentException(
						"The argument 'argTypes' contains classes "
								+ "but 'args' is null!");
			}
			argsIntern = args;
		}
		checkSameLength(argTypesIntern, argsIntern);

		String returnType = null;
		try {
			final Method method = obj.getClass().getMethod(methodName,
					argTypesIntern);
			if (method.getReturnType() == null) {
				returnType = "void";
			} else {
				returnType = method.getReturnType().getName();
			}
			return method.invoke(obj, argsIntern);
		} catch (final SecurityException ex) {
			throw new InvokeMethodFailedException(
					"Security problem with '"
							+ getMethodSignature(returnType, methodName,
									argTypesIntern) + "'! ["
							+ obj.getClass().getName() + "]", ex);
		} catch (final NoSuchMethodException ex) {
			throw new InvokeMethodFailedException(
					"Method '"
							+ getMethodSignature(returnType, methodName,
									argTypesIntern) + "' not found! ["
							+ obj.getClass().getName() + "]", ex);
		} catch (final IllegalArgumentException ex) {
			throw new InvokeMethodFailedException(
					"Argument problem with '"
							+ getMethodSignature(returnType, methodName,
									argTypesIntern) + "'! ["
							+ obj.getClass().getName() + "]", ex);
		} catch (final IllegalAccessException ex) {
			throw new InvokeMethodFailedException(
					"Access problem with '"
							+ getMethodSignature(returnType, methodName,
									argTypesIntern) + "'! ["
							+ obj.getClass().getName() + "]", ex);
		} catch (final InvocationTargetException ex) {
			throw new InvokeMethodFailedException(
					"Got an exception when calling '"
							+ getMethodSignature(returnType, methodName,
									argTypesIntern) + "'! ["
							+ obj.getClass().getName() + "]", ex);
		}

	}

	private static void checkSameLength(final Class[] argTypes,
			final Object[] args) {
		if (argTypes.length != args.length) {
			throw new IllegalArgumentException(
					"The argument 'argTypes' contains " + argTypes.length
							+ " classes " + "but 'args' only contains "
							+ args.length + " arguments!");
		}
	}

	/**
	 * Unzips a file into a given directory. WARNING: Only relative path entries
	 * are allowed inside the archive!
	 * 
	 * @param zipFile
	 *            Source ZIP file - Cannot be <code>null</code> and must be a
	 *            valid ZIP file.
	 * @param destDir
	 *            Destination directory - Cannot be <code>null</code> and must
	 *            exist.
	 * 
	 * @throws IOException
	 *             Error unzipping the file.
	 */
	public static void unzip(final File zipFile, final File destDir)
			throws IOException {
		unzip(zipFile, destDir, null, null);
	}

	/**
	 * Unzips a file into a given directory. WARNING: Only relative path entries
	 * are allowed inside the archive!
	 * 
	 * @param zipFile
	 *            Source ZIP file - Cannot be <code>null</code> and must be a
	 *            valid ZIP file.
	 * @param destDir
	 *            Destination directory - Cannot be <code>null</code> and must
	 *            exist.
	 * @param wrapper
	 *            Callback interface to give the caller the chance to wrap the
	 *            ZIP input stream into another one. This is useful for example
	 *            to display a progress bar - Can be <code>null</code> if no
	 *            wrapping is required.
	 * @param cancelable
	 *            Signals if the unzip should be canceled - Can be
	 *            <code>null</code> if no cancel option is required.
	 * 
	 * @throws IOException
	 *             Error unzipping the file.
	 */
	public static void unzip(final File zipFile, final File destDir,
			final UnzipInputStreamWrapper wrapper, final Cancelable cancelable)
			throws IOException {

		checkNotNull("zipFile", zipFile);
		checkValidFile(zipFile);
		checkNotNull("destDir", destDir);
		checkValidDir(destDir);

		final ZipFile zip = new ZipFile(zipFile);
		try {
			final Enumeration enu = zip.entries();
			while (enu.hasMoreElements()
					&& ((cancelable == null) || !cancelable.isCanceled())) {
				final ZipEntry entry = (ZipEntry) enu.nextElement();
				final File file = new File(entry.getName());
				if (file.isAbsolute()) {
					throw new IllegalArgumentException(
							"Only relative path entries are allowed! ["
									+ entry.getName() + "]");
				}
				if (entry.isDirectory()) {
					final File dir = new File(destDir, entry.getName());
					createIfNecessary(dir);
				} else {
					final File outFile = new File(destDir, entry.getName());
					createIfNecessary(outFile.getParentFile());
					final InputStream in;
					if (wrapper == null) {
						in = new BufferedInputStream(zip.getInputStream(entry));
					} else {
						in = new BufferedInputStream(wrapper.wrapInputStream(
								zip.getInputStream(entry), entry, outFile));
					}
					try {
						final OutputStream out = new BufferedOutputStream(
								new FileOutputStream(outFile));
						try {
							final byte[] buf = new byte[4096];
							int len;
							while ((len = in.read(buf)) > 0) {
								out.write(buf, 0, len);
							}
						} finally {
							out.close();
						}
					} finally {
						in.close();
					}
				}
			}
		} finally {
			zip.close();
		}
	}

	private static void createIfNecessary(final File dir) throws IOException {
		if (dir.exists()) {
			return;
		}
		if (!dir.mkdirs()) {
			throw new IOException("Error creating directory '" + dir + "'!");
		}
	}

	/**
	 * Returns the user home directory and checks if it is valid and exists. If
	 * not a <code>IllegalStateException</code> is thrown.
	 * 
	 * @return Directory.
	 */
	public static File getUserHomeDir() {
		final String str = System.getProperty(USER_HOME_KEY);
		if (str == null) {
			throw new IllegalStateException("System property '" + USER_HOME_KEY
					+ "' not found!");
		}
		final String userHome = str.trim();
		if (userHome.length() == 0) {
			throw new IllegalStateException("System property '" + USER_HOME_KEY
					+ "' is empty!");
		}
		final File dir = new File(userHome);
		try {
			checkValidDir(dir);
		} catch (final IllegalArgumentException ex) {
			throw new IllegalStateException("System property '" + USER_HOME_KEY
					+ "' is not valid! [" + ex.getMessage() + "]");
		}
		return dir;
	}

	/**
	 * Returns the temporary directory and checks if it is valid and exists. If
	 * not a <code>IllegalStateException</code> is thrown.
	 * 
	 * @return Directory.
	 */
	public static File getTempDir() {
		final String str = System.getProperty(TEMP_DIR_KEY);
		if (str == null) {
			throw new IllegalStateException("System property '" + TEMP_DIR_KEY
					+ "' not found!");
		}
		final String tempDirStr = str.trim();
		if (tempDirStr.length() == 0) {
			throw new IllegalStateException("System property '" + TEMP_DIR_KEY
					+ "' is empty!");
		}
		final File dir = new File(tempDirStr);
		try {
			checkValidDir(dir);
		} catch (final IllegalArgumentException ex) {
			throw new IllegalStateException("System property '" + TEMP_DIR_KEY
					+ "' is not valid! [" + ex.getMessage() + "]");
		}
		return dir;
	}

	/**
	 * Replaces all variables inside a string with values from a map.
	 * 
	 * @param str
	 *            Text with variables (Format: ${key} ) - May be
	 *            <code>null</code> or empty.
	 * @param vars
	 *            Map with key/values (both of type <code>String</code> - Cannot
	 *            be <code>null</code>.
	 * 
	 * @return String with replaced variables. Unknown variables will remain
	 *         unchanged.
	 */
	public static String replaceVars(final String str, final Map vars) {

		if (str == null) {
			return null;
		}
		if (str.length() == 0) {
			return str;
		}

		checkNotNull("vars", vars);

		final StringBuffer sb = new StringBuffer();

		int end = -1;
		int from = 0;
		int start = -1;
		while ((start = str.indexOf("${", from)) > -1) {
			sb.append(str.substring(end + 1, start));
			end = str.indexOf('}', start + 1);
			if (end == -1) {
				// No closing bracket found...
				sb.append(str.substring(start));
				from = str.length();
			} else {
				final String key = str.substring(start + 2, end);
				final String value = (String) vars.get(key);
				if (value == null) {
					sb.append("${");
					sb.append(key);
					sb.append("}");
				} else {
					sb.append(value);
				}
				from = end + 1;
			}
		}

		sb.append(str.substring(from));

		return sb.toString();
	}

	/**
	 * Converts Date into a Windows FILETIME. The Windows FILETIME structure
	 * holds a date and time associated with a file. The structure identifies a
	 * 64-bit integer specifying the number of 100-nanosecond intervals which
	 * have passed since January 1, 1601. This code is copied from the
	 * <code>org.apache.poi.hpsf.Util</code> class.
	 * 
	 * @param date
	 *            The date to be converted - Cannot be <code>null</code>.
	 * @return The file time
	 */
	public static long dateToFileTime(final Date date) {
		checkNotNull("date", date);
		final long msSince19700101 = date.getTime();
		final long msSince16010101 = msSince19700101 + EPOCH_DIFF;
		return msSince16010101 * (1000 * 10);
	}

	/**
	 * Creates an URL Link on the Windows Desktop. This is done by creating a
	 * file (URL File Format) with an ".url" extension. For a description see
	 * http://www.cyanwerks.com/file-format-url.html .
	 * 
	 * @param baseUrl
	 *            Base URL for the link - Cannot be <code>null</code> or empty.
	 * @param url
	 *            Target URL - Cannot be <code>null</code> or empty.
	 * @param workingDir
	 *            It's the "working folder" that your URL file uses. The working
	 *            folder is possibly the folder to be set as the current folder
	 *            for the application that would open the file. However Internet
	 *            Explorer does not seem to be affected by this field - Can be
	 *            <code>null</code>.
	 * @param showCommand
	 *            Normal=<code>null</code>, Minimized=7, Maximized=3
	 * @param iconIndex
	 *            The Icon Index within the icon library specified by IconFile.
	 *            In an icon library, which can be generally be either a ICO,
	 *            DLL or EXE file, the icons are indexed with numbers. The first
	 *            icon index starts at 0 - Can be <code>null</code> if the file
	 *            is not indexed.
	 * @param iconFile
	 *            Specifies the path of the icon library file. Generally the
	 *            icon library can be an ICO, DLL or EXE file. The default icon
	 *            library used tends to be the URL.DLL library on the system's
	 *            Windows\System directory - Can be <code>null</code> if no icon
	 *            is required.
	 * @param hotKey
	 *            The HotKey field specifies what is the shortcut key used to
	 *            automatically launch the Internet shortcut. The field uses a
	 *            number to specify what hotkey is used. To get the appropriate
	 *            code simply create a shortcut with MSIE and examine the file's
	 *            content.
	 * @param linkFilenameWithoutExtension
	 *            Name for the link file (displayed as text) - Cannot be
	 *            <code>null</code> or empty.
	 * @param overwrite
	 *            Overwrite an existing ".url" file.
	 * @param modified
	 *            Timestamp.
	 * 
	 * @throws IOException
	 *             Error writing the file.
	 */
	// CHECKSTYLE:OFF Maximum Parameters
	public static void createWindowsDesktopUrlLink(final String baseUrl,
			final String url, final File workingDir, final Integer showCommand,
			final Integer iconIndex, final File iconFile, final Integer hotKey,
			final String linkFilenameWithoutExtension, final boolean overwrite,
			final Date modified) throws IOException {
		// CHECKSTYLE:ON

		checkNotNull("baseUrl", baseUrl);
		checkNotEmpty("baseUrl", baseUrl);
		checkNotNull("url", url);
		checkNotEmpty("url", url);
		checkNotNull("linkFilenameWithoutExtension",
				linkFilenameWithoutExtension);
		checkNotEmpty("linkFilenameWithoutExtension",
				linkFilenameWithoutExtension);

		final File userHomeDir = new File(System.getProperty("user.home"));
		final File desktopDir = new File(userHomeDir, "Desktop");
		final File linkFile = new File(desktopDir, linkFilenameWithoutExtension
				+ ".url");
		if (linkFile.exists() && !overwrite) {
			// Do nothing
			return;
		}
		final String content = createWindowsDesktopUrlLinkContent(baseUrl, url,
				workingDir, showCommand, iconIndex, iconFile, hotKey, modified);
		final Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(linkFile), "Cp1252"));
		try {
			writer.write(content);
		} finally {
			writer.close();
		}

	}

	/**
	 * Creates the content of an URL Link file (.url) on the Windows Desktop.
	 * For a description see http://www.cyanwerks.com/file-format-url.html .
	 * 
	 * @param baseUrl
	 *            Base URL for the link - Cannot be <code>null</code> or empty.
	 * @param url
	 *            Target URL - Cannot be <code>null</code> or empty.
	 * @param workingDir
	 *            It's the "working folder" that your URL file uses. The working
	 *            folder is possibly the folder to be set as the current folder
	 *            for the application that would open the file. However Internet
	 *            Explorer does not seem to be affected by this field - Can be
	 *            <code>null</code>.
	 * @param showCommand
	 *            Normal=<code>null</code>, Minimized=7, Maximized=3
	 * @param iconIndex
	 *            The Icon Index within the icon library specified by IconFile.
	 *            In an icon library, which can be generally be either a ICO,
	 *            DLL or EXE file, the icons are indexed with numbers. The first
	 *            icon index starts at 0 - Can be <code>null</code> if the file
	 *            is not indexed.
	 * @param iconFile
	 *            Specifies the path of the icon library file. Generally the
	 *            icon library can be an ICO, DLL or EXE file. The default icon
	 *            library used tends to be the URL.DLL library on the system's
	 *            Windows\System directory - Can be <code>null</code> if no icon
	 *            is required.
	 * @param hotKey
	 *            The HotKey field specifies what is the shortcut key used to
	 *            automatically launch the Internet shortcut. The field uses a
	 *            number to specify what hotkey is used. To get the appropriate
	 *            code simply create a shortcut with MSIE and examine the file's
	 *            content.
	 * @param modified
	 *            Timestamp.
	 * 
	 * @return INI file text.
	 */
	// CHECKSTYLE:OFF
	public static String createWindowsDesktopUrlLinkContent(
			final String baseUrl, final String url, final File workingDir,
			final Integer showCommand, final Integer iconIndex,
			final File iconFile, final Integer hotKey, final Date modified) {
		// CHECKSTYLE:ON

		checkNotNull("baseUrl", baseUrl);
		checkNotEmpty("baseUrl", baseUrl);
		checkNotNull("url", url);
		checkNotEmpty("url", url);

		final StringBuffer sb = new StringBuffer();
		sb.append("[DEFAULT]\r\n");
		sb.append("BASEURL=" + baseUrl + "\r\n");
		sb.append("\r\n");
		sb.append("[InternetShortcut]\r\n");
		sb.append("URL=" + url + "\r\n");
		if (workingDir != null) {
			sb.append("WorkingDirectory=" + workingDir + "\r\n");
		}
		if (showCommand != null) {
			sb.append("ShowCommand=" + showCommand);
		}
		if ((iconFile != null) && (iconFile.exists())) {
			if (iconIndex == null) {
				sb.append("IconIndex=0\r\n");
			} else {
				sb.append("IconIndex=" + iconIndex);
			}
			sb.append("IconFile=" + iconFile + "\r\n");
		}
		sb.append("Modified=" + dateToFileTime(new Date()) + "\r\n");
		if (hotKey != null) {
			sb.append("HotKey=" + hotKey + "\r\n");
		}
		return sb.toString();
	}

	/**
	 * Concatenate a path and a filename taking <code>null</code> and empty
	 * string values into account.
	 * 
	 * @param path
	 *            Path - Can be <code>null</code> or an empty string.
	 * @param filename
	 *            Filename - Cannot be <code>null</code>.
	 * @param separator
	 *            Separator for directories - Can be <code>null</code> or an
	 *            empty string.
	 * 
	 * @return Path and filename divided by the separator.
	 */
	public static String concatPathAndFilename(final String path,
			final String filename, final String separator) {

		checkNotNull("filename", filename);
		checkNotNull("separator", separator);
		checkNotEmpty("separator", separator);

		if (path == null) {
			return filename;
		}
		final String trimmedPath = path.trim();
		if (trimmedPath.length() == 0) {
			return filename;
		}
		final String trimmedFilename = filename.trim();
		if (trimmedPath.endsWith(separator)) {
			return trimmedPath + trimmedFilename;
		}
		return trimmedPath + separator + trimmedFilename;

	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexidecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * Author: Apache Software Foundation
	 * See: org.apache.commons.codec.binary.Hex
	 * 
	 * @param data
	 *            A byte[] to convert to Hex characters - Cannot be
	 *            <code>null</code>.
	 * 
	 * @return A string containing hexidecimal characters
	 */
	public static String encodeHex(final byte[] data) {

		checkNotNull("data", data);

		final int l = data.length;
		final char[] out = new char[l << 1];
		// two characters form the hex value.
		int j = 0;
		for (int i = 0; i < l; i++) {
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}
		return String.copyValueOf(out);
	}

	/**
	 * Converts an array of characters representing hexidecimal values into an
	 * array of bytes of those same values. The returned array will be half the
	 * length of the passed array, as it takes two characters to represent any
	 * given byte. An exception is thrown if the passed char array has an odd
	 * number of elements.
	 * 
	 * @param data
	 *            An array of characters containing hexidecimal digits - Cannot
	 *            be <code>null</code>.
	 * 
	 * @return A byte array containing binary data decoded from the supplied
	 *         char array.
	 * 
	 * Author: Apache Software Foundation
	 * See: org.apache.commons.codec.binary.Hex
	 */
	public static byte[] decodeHex(final String data) {

		checkNotNull("data", data);

		final int len = data.length();

		if ((len & 0x01) != 0) {
			throw new RuntimeException("Odd number of characters.");
		}

		final byte[] out = new byte[len >> 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = toDigit(data.charAt(j), j) << 4;
			j++;
			f = f | toDigit(data.charAt(j), j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}

		return out;
	}

	/**
	 * Converts a hexadecimal character to an integer.
	 * 
	 * @param ch
	 *            A character to convert to an integer digit
	 * @param index
	 *            The index of the character in the source
	 * @return An integer
	 * 
	 * @author Apache Software Foundation
	 * @see org.apache.commons.codec.binary.Hex
	 */
	private static int toDigit(final char ch, final int index) {
		final int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new RuntimeException("Illegal hexadecimal charcter " + ch
					+ " at index " + index);
		}
		return digit;
	}

	/**
	 * Lock the file.
	 * 
	 * @param file
	 *            File to lock - Cannot be <code>null</code>.
	 * @param tryLockMax
	 *            Number of tries to lock before throwing an exception.
	 * @param tryWaitMillis
	 *            Milliseconds to sleep between retries.
	 * 
	 * @return FileLock.
	 * 
	 * @throws LockingFailedException
	 *             Locking the file failed.
	 */
	public static FileLock lockRandomAccessFile(final RandomAccessFile file,
			final int tryLockMax, final long tryWaitMillis)
			throws LockingFailedException {

		checkNotNull("file", file);

		final FileChannel channel = file.getChannel();

		int tryCount = 0;
		while (tryCount < tryLockMax) {
			tryCount++;
			try {
				final FileLock lock = channel.tryLock();
				if (lock != null) {
					return lock;
				}
			} catch (final IOException ex) {
				throw new LockingFailedException("Unexpected I/O-Exception!",
						ex);
			} catch (final OverlappingFileLockException ex) {
				ignore();
			}
			try {
				Thread.sleep(tryWaitMillis);
			} catch (final InterruptedException ex) {
				throw new LockingFailedException("Unexpected interrupt!", ex);
			}
		}
		throw new LockingFailedException("Number of max tries (" + tryLockMax
				+ ") exceeded!");

	}

	private static void ignore() {
		// Do nothing - Just to document rare empty catches.
	}

	/**
	 * Adds a file to a ZIP output stream.
	 * 
	 * @param srcFile
	 *            File to add - Cannot be <code>null</code>.
	 * @param destPath
	 *            Path to use for the file - May be <code>null</code> or empty.
	 * @param out
	 *            Destination stream - Cannot be <code>null</code>.
	 * 
	 * @throws IOException
	 *             Error writing to the output stream.
	 */
	private static void zipFile(final File srcFile, final String destPath,
			final ZipOutputStream out) throws IOException {

		final byte[] buf = new byte[1024];
		final InputStream in = new BufferedInputStream(new FileInputStream(
				srcFile));
		try {
			out.putNextEntry(new ZipEntry(concatPathAndFilename(destPath,
					srcFile.getName(), File.separator)));
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
		} finally {
			in.close();
		}
	}

	/**
	 * List all files for a directory.
	 * 
	 * @param srcDir
	 *            Directory to list the files for - Cannot be <code>null</code>
	 *            and must be a valid directory.
	 * @param filter
	 *            Filter or <code>null</code> for all files.
	 * 
	 * @return List of child entries of the directory.
	 */
	private static File[] listFiles(final File srcDir, final FileFilter filter) {

		final File[] files;
		if (filter == null) {
			files = srcDir.listFiles();
		} else {
			files = srcDir.listFiles(filter);
		}
		return files;

	}

	/**
	 * Add a directory to a ZIP output stream.
	 * 
	 * @param srcDir
	 *            Directory to add - Cannot be <code>null</code> and must be a
	 *            valid directory.
	 * @param filter
	 *            Filter or <code>null</code> for all files.
	 * @param destPath
	 *            Path to use for the ZIP archive - May be <code>null</code> or
	 *            an empyt string.
	 * @param out
	 *            Destination stream - Cannot be <code>null</code>.
	 * 
	 * @throws IOException
	 *             Error writing to the output stream.
	 */
	private static void zipDir(final File srcDir, final FileFilter filter,
			final String destPath, final ZipOutputStream out)
			throws IOException {

		final File[] files = listFiles(srcDir, filter);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				zipDir(files[i], filter, concatPathAndFilename(destPath,
						files[i].getName(), File.separator), out);
			} else {
				zipFile(files[i], destPath, out);
			}
		}

	}

	/**
	 * Creates a ZIP file and adds all files in a directory and all it's sub
	 * directories to the archive. Only entries are added that comply to the
	 * file filter.
	 * 
	 * @param srcDir
	 *            Directory to add - Cannot be <code>null</code> and must be a
	 *            valid directory.
	 * @param filter
	 *            Filter or <code>null</code> for all files/directories.
	 * @param destPath
	 *            Path to use for the ZIP archive - May be <code>null</code> or
	 *            an empyt string.
	 * @param destFile
	 *            Target ZIP file - Cannot be <code>null</code>.
	 * 
	 * @throws IOException
	 *             Error writing to the output stream.
	 */
	public static void zipDir(final File srcDir, final FileFilter filter,
			final String destPath, final File destFile) throws IOException {

		Utils4J.checkNotNull("srcDir", srcDir);
		Utils4J.checkValidDir(srcDir);
		Utils4J.checkNotNull("destFile", destFile);

		final ZipOutputStream out = new ZipOutputStream(
				new BufferedOutputStream(new FileOutputStream(destFile)));
		try {
			zipDir(srcDir, filter, destPath, out);
		} finally {
			out.close();
		}

	}

	/**
	 * Creates a ZIP file and adds all files in a directory and all it's sub
	 * directories to the archive.
	 * 
	 * @param srcDir
	 *            Directory to add - Cannot be <code>null</code> and must be a
	 *            valid directory.
	 * @param destPath
	 *            Path to use for the ZIP archive - May be <code>null</code> or
	 *            an empyt string.
	 * @param destFile
	 *            Target ZIP file - Cannot be <code>null</code>.
	 * 
	 * @throws IOException
	 *             Error writing to the output stream.
	 */
	public static void zipDir(final File srcDir, final String destPath,
			final File destFile) throws IOException {

		zipDir(srcDir, null, destPath, destFile);

	}

	/**
	 * Wraps a given input stream into another one an returns it.
	 */
	public static interface UnzipInputStreamWrapper {

		/**
		 * Wraps the input stream into another one.
		 * 
		 * @param in
		 *            Stream to create a wrapping stream for.
		 * @param entry
		 *            Zip entry the input stream is reading.
		 * @param destFile
		 *            Destination file.
		 * 
		 * @return Wrapped input stream.
		 */
		public InputStream wrapInputStream(InputStream in, ZipEntry entry,
				File destFile);

	}

}
