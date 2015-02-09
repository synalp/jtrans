package fr.loria.synalp.jtrans.markup;

import fr.loria.synalp.jtrans.utils.ClassEnumerator;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Repository of markup plugins (savers or loaders).
 * Markup plugins are paired with a unique string key.
 */
public class MarkupPluginPool<HandlerType extends MarkupPlugin> {

	// Maps lowercase class names to classes
	protected Map<String, Class<HandlerType>> map;


	protected MarkupPluginPool(
			Class<HandlerType> mother,
			String optSuffix)
	{
		map = new HashMap<String, Class<HandlerType>>();

		ArrayList<Class<?>> loaders = ClassEnumerator.getClassesForPackage(
				mother.getPackage());

		for (final Class clazz: loaders) {
			int mod = clazz.getModifiers();
			if (Modifier.isInterface(mod) || Modifier.isAbstract(mod)) {
				continue;
			}

			if (!mother.isAssignableFrom(clazz)) {
				continue;
			}

			String key = clazz.getSimpleName();
			
			if (key.endsWith(optSuffix)) {
				key = key.substring(0, key.length() - optSuffix.length());
			}
			key = key.toLowerCase();

			@SuppressWarnings("unchecked")
			Class<HandlerType> htClazz = (Class<HandlerType>)clazz;

			map.put(key, htClazz);
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
		Class<HandlerType> clazz = map.get(name.toLowerCase());
		if (null == clazz) {
			throw new IllegalArgumentException("No such plugin: " + name);
		} else {
			return clazz.getConstructor().newInstance();
		}
	}

}
