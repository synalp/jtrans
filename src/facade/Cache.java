package facade;

import java.io.*;

/**
 * Static class for caching the result of expensive operations.
 *
 * Each cached object belongs to a unique combination of a WAVE file and a text,
 * which is identified by a unique hash.
 *
 * Cached objects are stored as files on disk in a subdirectory specific to the
 * combination.
 *
 * Any modification in the WAVE file or in the text triggers a change in the
 * hash of the combination, which, in turn, invalidates the cache.
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
	 * Generates a unique path for a combination of several objects.
	 *
	 * Each object in hashableComponents triggers the creation of a new cache
	 * subdirectory named after a hash of the object. If any of the objects is
	 * a file, its modification date is also accounted for in the hash.
	 */
	public static File getCacheDir(Object... hashableComponents) {
		File f = CACHE_DIR;
		for (Object c: hashableComponents) {
			int hash = c.hashCode();
			if (c instanceof File)
				hash = (int)(hash * 31 + ((File)c).lastModified());
			f = new File(f, String.format("%08x.cache", hash));
		}
		return f;
	}


	/**
	 * Returns a cached object. If the requested object hasn't been cached yet,
	 * it is created, written to the cache, and returned.
	 *
	 * @param objectName custom identifier for the requested object
	 * @param factory factory to create a new object if the cache is invalid
	 * @param hashableComponents objects that will be hashed to find the
	 *                           correct cache file
	 */
	public static Object cachedObject(String objectName, ObjectFactory factory, Object... hashableComponents) {
		File cacheFile = new File(getCacheDir(hashableComponents), objectName);

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
			cacheFile.getParentFile().mkdirs();
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
	 * @param fileName custom identifier for the requested file
	 * @param factory factory to create a new file if the cache is invalid
	 * @param hashableComponents objects that will be hashed to find the
	 *                           correct cache file
	 */
	public static File cachedFile(String fileName, FileFactory factory, Object... hashableComponents) {
		File cacheFile = new File(getCacheDir(hashableComponents), fileName);

		if (!READ_FROM_CACHE || !cacheFile.exists()) {
			cacheFile.getParentFile().mkdirs();
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
