package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.markup.in.JTRLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class JTRSaver implements MarkupSaver {

	@Override
	public void save(Project project, File file) throws IOException {
		FileWriter w = new FileWriter(file);
		JTRLoader.newGson().toJson(project, w);
		w.close();
	}


	@Override
	public String getFormat() {
		return "JTrans custom JSON";
	}


	public String getExt() {
		return ".jtr";
	}

}
