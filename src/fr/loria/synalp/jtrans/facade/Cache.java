package fr.loria.synalp.jtrans.facade;

import java.io.*;

/**
 * Static class for caching the result of expensive operations.
 *
 * Each cached object belongs to a unique combination of "identifier" objects.
 * Identifier objects must be hashable objects. Any modification in the
 * identifiers must triggers a change in the hash of the combination, which, in
 * turn, invalidates the cache.
 *
 * Cached objects are stored as files on disk; each cache filename matches a
 * unique combination of identifiers.
 */
public class Cache {

	/**
	 * Factory invoked by cachedObject() when the cached object needs to be
	 * created or re-created.
	 */
	public interface ObjectFactory {
		public Object make();
	}


	/**
	 * Factory invoked by cachedFile() when the cached file needs to be
	 * created or re-created.
	 */
	public interface FileFactory {
		public void write(File f) throws IOException;
	}


	/**
	 * Flag indicating whether to try to read objects from the cache.
	 * If false, objects are recreated everytime they are requested.
	 */
	public static final boolean READ_FROM_CACHE = true;


	/** Cache files are stored there. */
	public static final File CACHE_DIR = new File("cache");


	/**
	 * Generates a unique path from the hash codes of a combination of several
	 * objects.
	 *
	 * If any of the objects is a file, its modification date is also
	 * represented in the hash.
	 *
	 * @param cacheGroup name of the cache subdirectory
	 * @param extension filename extension (without the period)
	 * @param identifiers hashable objects to be used in generating
	 *                    the unique hash
	 */
	public static File getCacheFile(String cacheGroup,
									 String extension,
									 Object... identifiers)
	{
		StringBuilder sb = new StringBuilder();
		for (Object c: identifiers) {
			int hash = c.hashCode();
			if (c instanceof File)
				hash = (int)(hash * 31 + ((File)c).lastModified());
			sb.append(String.format("%08x", hash)).append('.');
		}
		sb.append(extension);
		File f = new File(new File(CACHE_DIR, cacheGroup), sb.toString());
		if (!f.exists())
			f.getParentFile().mkdirs();
		return f;
	}


	/**
	 * Returns a cached object. If the requested object hasn't been cached yet,
	 * it is created, written to the cache, and returned.
	 *
	 * @param cacheGroup name of the cache subdirectory
	 * @param extension filename extension (without the period)
	 * @param factory factory to create a new object if the cache is invalid
	 * @param identifiers hashable objects to be used in generating
	 *                    the unique hash
	 */
	public static Object cachedObject(String cacheGroup,
									  String extension,
									  ObjectFactory factory,
									  Object... identifiers)
	{
		File cacheFile = getCacheFile(cacheGroup, extension, identifiers);

		// Try to read object from cache
		if (READ_FROM_CACHE && cacheFile.exists()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile));
				Object object = ois.readObject();
				ois.close();
				return object;
			} catch (Exception ex) {
				System.err.println("Couldn't deserialize cached object!");
				ex.printStackTrace();
			}
		}

		// Create object
		Object object = factory.make();

		// Dump computed object to cache
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile));
			oos.writeObject(object);
			oos.flush();
			oos.close();
		} catch (IOException ex) {
			System.err.println("Couldn't dump object to cache!");
			ex.printStackTrace();
		}

		return object;
	}


	/**
	 * Returns a cached file. If the requested file hasn't been cached yet,
	 * it is created, written to the cache, and returned.
	 *
	 * @param cacheGroup name of the cache subdirectory
	 * @param extension filename extension (without the period)
	 * @param factory factory to create a new file if the cache is invalid
	 * @param identifiers hashable objects to be used in generating
	 *                    the unique hash
	 */
	public static File cachedFile(String cacheGroup,
								  String extension,
								  FileFactory factory,
								  Object... identifiers)
	{
		File cacheFile = getCacheFile(cacheGroup, extension, identifiers);

		if (!READ_FROM_CACHE || !cacheFile.exists()) {
			try {
				factory.write(cacheFile);
			} catch (IOException ex) {
				System.err.println("Couldn't write cache file!");
				ex.printStackTrace();
			}
		}

		return cacheFile;
	}
}
