package fr.loria.synalp.jtrans.markup.jtr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.loria.synalp.jtrans.elements.Element;

import java.io.File;


/**
 * Common utilities for the JTR file format.
 */
public final class JTR {

	private JTR() {}


	/**
	 * Returns a Gson object suitable for serializing and deserializing JTrans
	 * projects to/from JSON.
	 */
	public static Gson newGson() {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(Element.class, new InterfaceAdapter<Element>("$TYPE$"));
		gb.registerTypeAdapter(File.class, new FileAdapter());
		gb.setPrettyPrinting();
		return gb.create();
	}

}
