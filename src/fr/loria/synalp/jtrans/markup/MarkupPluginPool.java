package fr.loria.synalp.jtrans.markup;

import pro.ddopson.ClassEnumerator;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Repository of markup plugins (savers or loaders).
 * Markup plugins are paired with a unique string key.
 */
public class MarkupPluginPool<HandlerType extends MarkupPlugin> {

	// Maps lowercase class names to classes
	protected Map<String, Class> map;


	protected MarkupPluginPool(Class mother, String optSuffix) {
		map = new HashMap<String, Class>();

		ArrayList<Class<?>> loaders = ClassEnumerator.getClassesForPackage(
				mother.getPackage());

		for (final Class clazz: loaders) {
			int mod = clazz.getModifiers();
			if (Modifier.isInterface(mod) || Modifier.isAbstract(mod)) {
				continue;
			}

			try {
				clazz.asSubclass(mother);
			} catch (ClassCastException ex) {
				continue;
			}

			String key = clazz.getSimpleName();
			if (key.endsWith(optSuffix)) {
				key = key.substring(0, key.length() - optSuffix.length());
			}
			key = key.toLowerCase();

			map.put(key, clazz);
		}

	}


	/**
	 * Returns a set of the names of all available loaders.
	 */
	public Set<String> getNames() {
		return map.keySet();
	}


	public String getDescription(String name) {
		try {
			return make(name).getFormat();
		} catch (ReflectiveOperationException ex) {
			ex.printStackTrace();
			return name;
		}
	}


	/**
	 * Creates an instance of a loader from a name.
	 * @throws IllegalArgumentException the requested markup loader cannot be
	 * found
	 * @throws ReflectiveOperationException an error occurred during class
	 * discovery
	 */
	public HandlerType make(String name)
			throws IllegalArgumentException, ReflectiveOperationException
	{
		Class clazz = map.get(name.toLowerCase());
		if (null == clazz) {
			throw new IllegalArgumentException("No such plugin: " + name);
		} else {
			return (HandlerType)clazz.getConstructor().newInstance();
		}
	}

}
