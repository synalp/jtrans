package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.markup.MarkupPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Interface for saving a project to various text markup formats.
 */
public interface MarkupSaver extends MarkupPlugin {

	public void save(Project project, File file) throws IOException;

}
