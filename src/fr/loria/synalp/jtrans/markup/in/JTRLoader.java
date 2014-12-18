package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.TrackProject;
import fr.loria.synalp.jtrans.utils.FileUtils;

import static fr.loria.synalp.jtrans.markup.jtr.JTR.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * Parser for JTrans's JSON format.
 */
public class JTRLoader implements MarkupLoader {

	@Override
	public Project parse(File file) throws ParsingException, IOException {
		Reader r = FileUtils.getUTF8Reader(file);
		ProjectWrapper pw = newGson().fromJson(r, ProjectWrapper.class);
		r.close();
		
		if (pw.project instanceof TrackProject) {
			System.out.println("track project detected");
			TrackProject p = (TrackProject)pw.project;
			p.fixTracks();
		}
		
		return pw.project;
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
