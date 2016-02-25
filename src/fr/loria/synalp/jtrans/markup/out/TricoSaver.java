package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.TurnProject;
import fr.loria.synalp.jtrans.project.TurnProject.Turn;
import fr.loria.synalp.jtrans.project.Anchor;
import fr.loria.synalp.jtrans.project.Token;

import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;
import static fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer.frame2second;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/** Save the project as a TRICO file.
    From a TRICO file, rewrite the original XML content with updated timing.
    Otherwise, write a list of empty turns with their timing.
*/
public class TricoSaver implements MarkupSaver {

    @Override
    public void save(Project project, File file) throws IOException {
	if (project instanceof TurnProject) {
	    TurnProject p = (TurnProject) project;

	    updateTiming(p);

	    if (p.document == null) {
		printTurnTimings(p, file);
	    } else {
		updateDocumentTiming(p);
		printAlignedTrico(p, file);
	    }
	} else {
	    System.err.println("This is not a Turn-Based Project. Can't be saved yet. Sorry.");
	}
    }

    /** Update Turn Anchors on the basis of aligned tokens */
    protected void updateTiming(TurnProject p) {
	int nSpeakers = p.speakerCount();

	// STAGE 1: update timing for non empty turns
	for (int i=0; i< p.turns.size(); i++){
	    Turn t = p.turns.get(i);
	    float earliest = Float.MAX_VALUE;
	    float latest = Float.MIN_VALUE;
		
	    for (int s = 0; s < nSpeakers; s++) {		    
		for (Token tok: t.spkTokens.get(s)) {
		    if (tok.isAligned()) {
			Token.Segment seg = tok.getSegment();
			earliest = Math.min(earliest, seg.getStartSecond());
			latest   = Math.max(latest  , seg.getEndSecond());
		    }
		}
	    }
	    t.start=new Anchor(earliest);
	    t.end=new Anchor(latest);		       
	}

	// STAGE 2: update timings for empty turns
	// Count how many empty turns there are between two valid anchors and divide
	float minMin = 0f;
	float maxMax = frame2second((int)p.audioSourceTotalFrames);
	float prev = minMin;
	for (int i=0; i< p.turns.size(); i++) {
	    Turn t = p.turns.get(i);
	    float start = t.start.seconds;
	    if (start == Float.MAX_VALUE) {
		int nEmpty = 1;
		float next = maxMax;
		for (int j=i+1; j<p.turns.size(); j++) {
		    Turn t2 = p.turns.get(j);
		    float start2 = t2.start.seconds;
		    if (start2 == Float.MAX_VALUE) nEmpty++;
		    else {
			next = start2;
			break;
		    }
		    if (j == p.turns.size() -1) {
			next = maxMax;
		    }
		}
		t.start = new Anchor( prev );
		t.end = new Anchor ( prev + (next - prev) / nEmpty );

		prev = prev + (next - prev) / nEmpty;
	    }
	}
    }

    /** print a raw XML list of Turns, with starting and ending time stamps
	No content. Not really a Trico file
    */
    protected void printTurnTimings(TurnProject p, File file) throws IOException {
	Writer w = getUTF8Writer(file);
	Anchor.showMinutes = false;

	w.append("<?xml  version=\"1.0\" encoding=\"UTF-8\"?>\n");
	w.append("<Turns>\n");

	for (int i=0; i< p.turns.size(); i++) {
	    Turn t = p.turns.get(i);
	    w.append("  <Turn startTime=\""+t.start.toString()+"\" endTime=\""+t.end.toString()+"\"/>\n");
	}
	w.append("</Turns>");

	w.close();
    }

    /** update original document timing */
    protected void updateDocumentTiming(TurnProject p) {
	Anchor.showMinutes = false;
	String first ="";
	String last = "";

	NodeList turnList = p.document.getElementsByTagName("Turn");
	NodeList syncList = p.document.getElementsByTagName("Sync");

	String start;
	String end;
	for (int i=0; i< p.turns.size(); i++) {
	    Turn turn = p.turns.get(i);	
	    start = turn.start.toString();
	    end = turn.end.toString();
	    if (i==0) first = start;
	    if (i == p.turns.size() -1) last = end;

	    if (turnList.getLength() > i) {
		Element originalTurn = (Element) turnList.item(i);
		originalTurn.setAttribute( "startTime", start);
		originalTurn.setAttribute( "endTime", end);
		NodeList children =  originalTurn.getElementsByTagName("Sync");
		if (children.getLength() > 0) {
		    Element sync = (Element)children.item(0);		
		    sync.setAttribute("time", start);		 
		}
	    }
	}

	Element section = (Element) p.document.getElementsByTagName("Section").item(0);
	section.setAttribute( "startTime", first);
	section.setAttribute( "endTime", last);
    }

    /** print the updated document */
    protected void printAlignedTrico(TurnProject p, File file) throws IOException {	    
	Source source = new DOMSource(p.document);
	Result result = new StreamResult(file);
	try {
	    Transformer xformer = TransformerFactory.newInstance().newTransformer();
	    xformer.transform(source, result);
	    System.err.println("Trico file written.");
	} catch (TransformerException e) {
	    System.err.println("\n\nTransformer ERROR");
	    e.printStackTrace();
	}	
    }


    @Override
    public String getFormat() {
	return "TransICOR";
    }


    public String getExt() {
	return ".trico";
    }

}
