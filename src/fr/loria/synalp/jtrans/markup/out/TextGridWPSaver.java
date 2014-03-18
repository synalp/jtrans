package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.facade.Project;

import java.io.File;
import java.io.IOException;

public class TextGridWPSaver implements MarkupSaver {

	public void save(Project project, File file) throws IOException {
		TextGridSaverHelper.savePraat(project, file, true, true);
	}

	public String getFormat() {
		return "Praat TextGrid (words + phones)";
	}

	public String getExt() {
		return ".w+p.textgrid";
	}
}
