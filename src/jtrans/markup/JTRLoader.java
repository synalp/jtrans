package jtrans.markup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jtrans.elements.Element;
import jtrans.facade.Project;
import jtrans.utils.InterfaceAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Parser for JTrans's JSON format.
 */
public class JTRLoader implements MarkupLoader {
	/**
	 * Returns a Gson object suitable for serializing and deserializing JTrans
	 * projects to/from JSON.
	 */
	public static Gson newGson() {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(Element.class, new InterfaceAdapter<Element>("$TYPE$"));
		gb.setPrettyPrinting();
		return gb.create();
	}

	@Override
	public Project parse(File file) throws ParsingException, IOException {
		FileReader r = new FileReader(file);
		Project project = newGson().fromJson(r, Project.class);
		r.close();
		project.refreshIndex();
		return project;
	}

	@Override
	public String getFormat() {
		return "JTrans/JSON (\"JTR\")";
	}
}
