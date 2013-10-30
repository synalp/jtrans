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
	 * This abstract factory is invoked when no valid cached object was found.
	 */
	public interface Factory {
		public Object make();
	}

	/**
	 * Flag indicating whether to try to read objects from the cache.
	 * If false, objects are recreated everytime they are requested.
	 */
	public static final boolean READ_FROM_CACHE = true;


	/** Cache files are stored there. */
	public static final File CACHE_DIR = new File("cache");


	/**
	 * Generate a unique hash for a combination of a WAVE file and a text.
	 * @param wave WAVE file name
	 * @param text speech text
	 */
	public static final int comboHash(String wave, String text) {
		return (int)(text.hashCode() + wave.hashCode() + new File(wave).lastModified());
	}


	/**
	 * Return a cached object matching objectId or create a new object if the
	 * cache is invalid.
	 *
	 * When a new object is created, it is written to the cache.
	 *
	 * @param wave WAVE file name
	 * @param text speech text
	 * @param objectName custom identifier for the requested object
	 * @param factory factory to create a new object if the cache is invalid
	 */
	public static final Object cachedObject(String wave, String text, String objectName, Factory factory) {
		File cacheSubdir = new File(CACHE_DIR, String.format("combo_%08x", comboHash(wave, text)));
		File cacheFile = new File(cacheSubdir, objectName);

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

		// Compute object
		Object object = factory.make();

		// Dump computed object to cache
		try {
			cacheSubdir.mkdirs();
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
}
