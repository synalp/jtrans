package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.facade.Project;

import java.io.File;
import java.io.IOException;

public class TextGridPSaver implements MarkupSaver {

	public void save(Project project, File file) throws IOException {
		TextGridSaverHelper.savePraat(project, file, false, true);
	}

	public String getFormat() {
		return "Praat TextGrid (phones only)";
	}

	public String getExt() {
		return ".p.textgrid";
	}
}
