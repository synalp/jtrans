package fr.loria.synalp.jtrans.markup;

import pro.ddopson.ClassEnumerator;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Pool of markup loader plugins.
 * Includes both "vanilla" loaders (one per supported format) and preprocessors.
 */
public class MarkupLoaderPool {

	/** Maps lowercase class names to classes */
	private static Map<String, Class<? extends MarkupLoader>> nameMap;


	static {
		nameMap = new HashMap<String, Class<? extends MarkupLoader>>();

		ArrayList<Class<?>> loaders = ClassEnumerator.getClassesForPackage(
				MarkupLoader.class.getPackage());

		for (final Class clazz: loaders) {
			int mod = clazz.getModifiers();
			if (Modifier.isInterface(mod) || Modifier.isAbstract(mod)) {
				continue;
			}

			final Class<? extends MarkupLoader> mlClass;
			try {
				mlClass = clazz.asSubclass(MarkupLoader.class);
			} catch (ClassCastException ex) {
				continue;
			}

			nameMap.put(mlClass.getSimpleName().toLowerCase(), mlClass);
		}
	}


	private static boolean isVanillaLoader(Class clazz) {
		return clazz.getPackage().equals(MarkupLoader.class.getPackage());
	}


	/**
	 * Returns true if a loader is a vanilla loader (as opposed to a
	 * preprocessor).
	 */
	public static boolean isVanillaLoader(String name) {
		return isVanillaLoader(nameMap.get(name.toLowerCase()));
	}


	/**
	 * Returns a set of the names of all available loaders.
	 */
	public static Set<String> getLoaderNames() {
		return nameMap.keySet();
	}


	/**
	 * Creates an instance of a loader from a name.
	 * @throws IllegalArgumentException the requested markup loader cannot be
	 * found
	 * @throws ReflectiveOperationException an error occurred during class
	 * discovery
	 */
	public static MarkupLoader newLoader(String loaderName)
			throws IllegalArgumentException, ReflectiveOperationException
	{
		Class clazz = nameMap.get(loaderName.toLowerCase());
		if (null == clazz) {
			throw new IllegalArgumentException("No such markup loader: "
					+ loaderName);
		} else {
			return (MarkupLoader)clazz.getConstructor().newInstance();
		}
	}

}
