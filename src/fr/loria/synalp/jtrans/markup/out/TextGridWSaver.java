package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.facade.Project;

import java.io.File;
import java.io.IOException;

public class TextGridWSaver implements MarkupSaver {

	public void save(Project project, File file) throws IOException {
		TextGridSaverHelper.savePraat(project, file, true, false);
	}

	public String getFormat() {
		return "Praat TextGrid (words only)";
	}

	public String getExt() {
		return ".w.textgrid";
	}
}
