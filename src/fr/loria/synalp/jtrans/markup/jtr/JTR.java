package fr.loria.synalp.jtrans.markup.jtr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.project.Phrase;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.Token;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;


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
		class PhraseDeserializer implements JsonDeserializer<Phrase> {
			@Override
			public Phrase deserialize(JsonElement json, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				JsonArray l = json.getAsJsonArray();
				ArrayList<Token> t = new ArrayList<Token>();
				for (int i=0;i<l.size();i++) {
					JsonObject o = l.get(i).getAsJsonObject(); // Token
					// TODO: read all fields of Token one by one !!!
					Token tt = new Token("oo");
					t.add(tt);
				}
				System.out.println("indes "+json);
				return new Phrase(new Anchor(0), new Anchor(10), t);
			}
		}
		gb.registerTypeAdapter(Phrase.class, new PhraseDeserializer());
		gb.registerTypeAdapter(File.class, new FileAdapter());
		gb.setPrettyPrinting();
		return gb.create();
	}

}
