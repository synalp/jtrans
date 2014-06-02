package fr.loria.synalp.jtrans.markup.jtr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.loria.synalp.jtrans.project.Element;
import fr.loria.synalp.jtrans.project.Project;

import java.io.File;


/**
 * Common utilities for the JTR file format.
 */
public final class JTR {

	private JTR() {}


	/**
	 * A wrapper class is necessary so that the InterfaceAdapter has a chance
	 * to specify the type of the project (TurnProject or TrackProject).
	 * This is because fromJson() only works on non-generic objects.
	 *
	 * We could eventually add some more metadata (e.g. JTrans version number)
	 * to this class.
	 */
	public static class ProjectWrapper {
		public final Project project;

		public ProjectWrapper(Project p) {
			project = p;
		}
	}


	/**
	 * Returns a Gson object suitable for serializing and deserializing JTrans
	 * projects to/from JSON.
	 */
	public static Gson newGson() {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(Project.class, new InterfaceAdapter<Project>("$TYPE$"));
		gb.registerTypeAdapter(Element.class, new InterfaceAdapter<Element>("$TYPE$"));
		gb.registerTypeAdapter(File.class, new FileAdapter());
		gb.setPrettyPrinting();
		return gb.create();
	}

}
