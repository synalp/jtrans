package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.project.Project;
import static fr.loria.synalp.jtrans.markup.jtr.JTR.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Parser for JTrans's JSON format.
 */
public class JTRLoader implements MarkupLoader {

	@Override
	public Project parse(File file) throws ParsingException, IOException {
		FileReader r = new FileReader(file);
		Project project = newGson().fromJson(r, Project.class);
		r.close();
		return project;
	}


	@Override
	public String getFormat() {
		return "JTrans/JSON (\"JTR\")";
	}


	@Override
	public String getExt() {
		return ".jtr";
	}

}
