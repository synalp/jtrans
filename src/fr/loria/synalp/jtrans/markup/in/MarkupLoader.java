package fr.loria.synalp.jtrans.markup.in;

import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.markup.MarkupPlugin;

import java.io.*;


/**
 * Interface for loaders of various text markup formats.
 */
public interface MarkupLoader extends MarkupPlugin {

	/**
	 * Builds a project from the text markup contained in the input stream.
	 *
	 * Must create a synthetic anchor at the end of the last speaker turn.
	 */
	public Project parse(File file) throws ParsingException, IOException;

}
