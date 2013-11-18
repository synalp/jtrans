package markup;

import plugins.text.ListeElement;
import plugins.text.elements.Segment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Interface for loaders of various text markup formats.
 */
public interface MarkupLoader {
	public void parse(InputStream in) throws ParsingException, IOException;
	public String getText();
	public ListeElement getElements();
	public List<Segment> getNonTextSegments();
	public float getLastEnd();
	public String getFormat();
}
