package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.markup.MarkupPluginPool;

/**
 * Pool of markup loader plugins.
 * Includes both "vanilla" loaders (one per supported format) and preprocessors.
 */
public class MarkupLoaderPool extends MarkupPluginPool<MarkupLoader> {

	private static final MarkupLoaderPool instance = new MarkupLoaderPool();


	private MarkupLoaderPool() {
		super(MarkupLoader.class, "Loader");
	}


	public static MarkupLoaderPool getInstance() {
		return instance;
	}


	private static boolean isVanillaLoader(Class clazz) {
		return clazz.getPackage().equals(MarkupLoader.class.getPackage());
	}


	/**
	 * Returns true if a loader is a vanilla loader (as opposed to a
	 * preprocessor).
	 */
	public boolean isVanillaLoader(String name) {
		return isVanillaLoader(map.get(name.toLowerCase()));
	}

}
