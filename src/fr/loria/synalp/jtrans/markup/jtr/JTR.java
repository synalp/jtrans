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
import fr.loria.synalp.jtrans.project.Token.Phone;
import fr.loria.synalp.jtrans.project.Token.Segment;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;

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
				int phrasedeb=Integer.MAX_VALUE, phraseend=-Integer.MAX_VALUE;
				for (int i=0;i<l.size();i++) {
					JsonObject o = l.get(i).getAsJsonObject(); // Token
					String txt = o.get("text").getAsString();
					String type = o.get("type").getAsString();
					int speaker = o.get("speaker").getAsInt();
					boolean anon = o.get("anonymize").getAsBoolean();
					Token tt = new Token(txt,fr.loria.synalp.jtrans.project.Token.Type.valueOf(type));
					tt.setSpeaker(speaker);
					tt.setAnonymize(anon);
					t.add(tt);
					// read segment + phones
					JsonElement s = o.get("segment");
					if (s!=null) {
						JsonObject so=s.getAsJsonObject();
						int start = so.get("start").getAsInt();
						if (start<phrasedeb) phrasedeb=start;
						int end   = so.get("end").getAsInt();
						if (end>phraseend) phraseend=end;
						tt.setSegment(start, end);
					}
					s = o.get("phones");
					if (s!=null) {
						JsonArray so=s.getAsJsonArray();
						for (int j=0;j<so.size();j++) {
							o=so.get(j).getAsJsonObject();
							String ph = o.get("phone").getAsString();
							s = o.get("segment");
							JsonObject soo=s.getAsJsonObject();
							int start = soo.get("start").getAsInt();
							if (start<phrasedeb) phrasedeb=start;
							int end   = soo.get("end").getAsInt();
							if (end>phraseend) phraseend=end;
							Segment seg = new Segment(start, end);
							Phone pp = new Phone(ph, seg);
							tt.addPhone(pp);
						}
					}
				}
				float pdeb,pfin;
				if (phrasedeb==Integer.MAX_VALUE || phraseend==-Integer.MAX_VALUE)
				{pdeb=-1; pfin=-1;} else {
					pdeb=S4mfccBuffer.frame2second(phrasedeb);
					pfin=S4mfccBuffer.frame2second(phraseend);
				}
				return new Phrase(new Anchor(pdeb), new Anchor(pfin), t);
			}
		}
		gb.registerTypeAdapter(Phrase.class, new PhraseDeserializer());
		gb.registerTypeAdapter(File.class, new FileAdapter());
		gb.setPrettyPrinting();
		return gb.create();
	}

}
