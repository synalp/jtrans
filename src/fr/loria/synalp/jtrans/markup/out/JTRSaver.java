package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.project.Project;
import static fr.loria.synalp.jtrans.markup.jtr.JTR.*;
import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;


public class JTRSaver implements MarkupSaver {

	@Override
	public void save(Project project, File file) throws IOException {
		Writer w = getUTF8Writer(file);
		newGson().toJson(project, w);
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
