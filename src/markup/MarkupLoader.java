package markup;

import plugins.text.ListeElement;

import java.io.*;


/**
 * Interface for loaders of various text markup formats.
 */
public interface MarkupLoader {
	/**
	 * Builds an element list and text buffer from the text markup contained in
	 * the input stream.
	 *
	 * Must create a synthetic anchor at the end of the last speaker turn.
	 */
	public void parse(File file) throws ParsingException, IOException;

	public ListeElement getElements();
	public String getFormat();
}
