package fr.loria.synalp.jtrans.markup.in.preprocessors;

import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.markup.in.ParsingException;
import fr.loria.synalp.jtrans.markup.in.TRSLoader;
import fr.loria.synalp.jtrans.project.TrackProject;
import fr.loria.synalp.jtrans.project.TurnProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Transcriber preprocessory. They modify the structure of a
 * Transcriber document to make it readable by the vanilla parser.
 * @see TRSLoader
 */
abstract class TRSPreprocessor extends TRSLoader {

	/**
	 * Modifies a Transcriber document in-place.
	 */
	public abstract void preprocess(Document doc) throws ParsingException;


	@Override
	public final TurnProject parse(File file)
			throws ParsingException, IOException
	{
		Document doc = parseXML(file);
		preprocess(doc);
		TurnProject p = parse(doc);
		postprocess(p);
		return p;
	}


	protected void postprocess(TurnProject p) throws ParsingException {
		// no-op by default
	}


	protected static List<Node> scrap(Node node) {
		List<Node> scrapped = new ArrayList<Node>();
		while (null != node) {
			scrapped.add(node);
			Node n = node;
			node = n.getNextSibling();
			n.getParentNode().removeChild(n);
		}
		return scrapped;
	}


	/**
	 * Saves the modified Transcriber document to an output stream.
	 */
	public void transform(File file, OutputStream out) throws
			ParsingException, IOException, TransformerException
	{
		Document doc = parseXML(file);
		preprocess(doc);

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, doc.getXmlEncoding());
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doc.getDoctype().getSystemId());

		// doesn't actually indent (another property is needed),
		// but places linebreaks strategically
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		transformer.transform(new DOMSource(doc), new StreamResult(out));
	}


	protected void transform(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("USAGE:\n\t" + this.getClass().getSimpleName()
					+ " <INPUT.trs> <OUTPUT.trs>");
			System.exit(1);
		}

		transform(new File(args[0]), new FileOutputStream(args[1]));
	}
}
