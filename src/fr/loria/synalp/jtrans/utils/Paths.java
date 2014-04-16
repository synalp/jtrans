package fr.loria.synalp.jtrans.utils;

import java.io.File;

/**
 * Paths for various directories used by JTrans (config, resources, cache...).
 */
public class Paths {

	public static final File BASE_DIR =
			new File(System.getProperty("user.home"), ".jtrans");

	public static final File RES_DIR = new File(BASE_DIR, "res");

	public static final File CACHE_DIR = new File(BASE_DIR, "cache");

}
