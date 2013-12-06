package jtrans.markup;

import jtrans.facade.Project;

import java.io.*;


/**
 * Interface for loaders of various text markup formats.
 */
public interface MarkupLoader {
	/**
	 * Builds a project from the text markup contained in the input stream.
	 *
	 * Must create a synthetic anchor at the end of the last speaker turn.
	 */
	public Project parse(File file) throws ParsingException, IOException;

	/**
	 * Returns the name of the format handled by this MarkupLoader.
	 */
	public String getFormat();
}
