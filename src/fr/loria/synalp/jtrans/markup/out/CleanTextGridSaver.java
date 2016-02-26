package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.graph.StatePool;
import fr.loria.synalp.jtrans.project.*;
import fr.loria.synalp.jtrans.utils.FileUtils;

import java.io.*;
import java.util.Iterator;

import static fr.loria.synalp.jtrans.utils.FileUtils.getUTF8Writer;
import static fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer.frame2second;

/** CleanTextGridSaver : Save as TextGrid with words (no phonemes)
    No null interval
    Intervals start at 0
    @author : Matthieu Quignard
*/
public class CleanTextGridSaver implements MarkupSaver {


    public void save(Project project, File file) throws IOException {
	savePraat(false,project,file,true,false);
    }


    private static void savePraat(
				  boolean anonymous,
				  Project p,
				  File f,
				  boolean withWords,
				  boolean withPhons)
	throws IOException
    {
	Writer w = getUTF8Writer(f);

	final int frameCount = (int) p.audioSourceTotalFrames;

	int nSpk = p.speakerCount();
	if (p.getSpeakerId(TurnProject.hackNobody) > -1) nSpk--;
	
	praatFileHeader(w,
			frameCount,
			nSpk * ((withWords?1:0) + (withPhons?1:0)));

	int id = 1;
	for (int i = 0; i < p.speakerCount(); i++) {
	    if (p.getSpeakerName(i) == TurnProject.hackNobody) continue;

	    StringBuilder wordSB = new StringBuilder();

	    int[] wordCount = {0};

	    // frame onto which to tack 0-length elements
	    int lastFrame = 0;

	    Iterator<Phrase> itr = p.phraseIterator(i);
	    while (itr.hasNext()) {
		Phrase phrase = itr.next();

                // frame onto which to tack 0-length elements
		if (phrase.getInitialAnchor() != null) {
		    lastFrame = phrase.getInitialAnchor().getFrame();
		}

		for (Token token: phrase) {
		    if (token.isAlignable() && token.isAligned()) {

			String tok = token.toString();

                        int startWfr = token.getFirstNonSilenceFrame();
                        int endWfr = token.getLastNonSilenceFrame();
                        lastFrame = token.getSegment().getEndFrame();

			if ((wordCount[0]==0) && (startWfr>0)) {
			    firstPraatInterval(
					       wordSB,
					       wordCount,
					       startWfr);
			}
			
			// cas normal: mot prononcé et aligné
			praatInterval(
				      wordSB,
				      wordCount,
				      startWfr,
				      endWfr,
				      tok);

			lastFrame=endWfr;                        
		    } 
                }
            }

	    if (withWords) {
		praatTierHeader(w, id++, p.getSpeakerName(i) + " words", wordCount[0], frameCount);
		w.write(wordSB.toString());
	    }
	}

	w.close();
    }


    /** The first header with time and tier information */
    public static void praatFileHeader(Appendable w, int frameCount, int tierCount)
	throws IOException
    {
	w.append("File type = \"ooTextFile\"")
	    .append("\nObject class = \"TextGrid\"")
	    .append("\n")
	    .append("\nxmin = 0")
	    .append("\nxmax = ")
	    .append(Float.toString(frame2second(frameCount)))
	    .append("\ntiers? <exists>")
	    .append("\nsize = ")
	    .append(Integer.toString(tierCount))
	    .append("\nitem []:");
    }


    /**
     * Appends a Praat tier header.
     * @param w Append text to this writer
     * @param id Tier ID (Praat tier numbering starts at 1 and is contiguous!)
     * @param name Tier name
     * @param intervalCount Number of intervals in the tier
     * @param frameCount Duration of the tier (as number of frames)
     */
    public static void praatTierHeader(
				       Appendable w, int id, String name, int intervalCount, int frameCount)
	throws IOException
    {
	assert id > 0;
	w.append("\n\titem [").append(Integer.toString(id)).append("]:")
	    .append("\n\t\tclass = \"IntervalTier\"")
	    .append("\n\t\tname = \"").append(name).append('"') // TODO escape strings
	    .append("\n\t\txmin = 0")
	    .append("\n\t\txmax = ")
	    .append(Float.toString(frame2second(frameCount)))
	    .append("\n\t\tintervals: size = ")
	    .append(Integer.toString(intervalCount));
    }


    /**
     * Appends a Praat interval.
     * @param w Append text to this writer
     * @param id Interval ID (Interval numbering starts at 1 and is contiguous!)

     TODO: we use the other praatInterval functions when doing anonymisation... So there must be some mismatch/diff btw both; try and make a single fct !
    */
    public static float praatInterval(
				      Appendable w, int[] id, int xminFrame, int xmaxFrame, String content)
	throws IOException
    {
	//get the endTime of the previous segment
	float lastTime = 0f;
	String s=w.toString();
	int i=s.lastIndexOf("xmax = ");
	if (i>=0) {
	    int j=s.indexOf('\n',i);
	    lastTime = Float.parseFloat(s.substring(i+7,j));
	}

	float debSec=frame2second(xminFrame,false);
	float endSec=frame2second(xmaxFrame,false);	   
	
	//don't start too early
	if (debSec<lastTime) debSec = lastTime;

	//don't end too early
	if ( endSec < (debSec  + 0.02f) ) endSec = Math.round((debSec+0.02) * 100f)/100f;

	// At this point, we sure have lastTime < debSec < endSec

	//if starts too far, add an empty interval in between
	if (debSec > (lastTime+0.02f)) {
	    w.append("\n\t\tintervals [").append(Integer.toString(++id[0])).append("]:")
		.append("\n\t\t\txmin = ")
		.append(Float.toString(lastTime))
		.append("\n\t\t\txmax = ")
		.append(Float.toString(debSec))
		.append("\n\t\t\ttext = \"\"");
	} else {
	    debSec = lastTime;
	}
	
	w.append("\n\t\tintervals [").append(Integer.toString(++id[0])).append("]:")
	    .append("\n\t\t\txmin = ")
	    .append(Float.toString(debSec))
	    .append("\n\t\t\txmax = ")
	    .append(Float.toString(endSec))
	    .append("\n\t\t\ttext = \"").append(content).append('"'); // TODO escape strings
	return endSec;
    }

    public static float firstPraatInterval(Appendable w, int[] id, int xmaxFrame) throws IOException {
	float endSec = frame2second(xmaxFrame,false);
	String content = "";
	w.append("\n\t\tintervals [").append(Integer.toString(++id[0])).append("]:")
	    .append("\n\t\t\txmin = 0")
	    .append("\n\t\t\txmax = ")
	    .append(Float.toString(endSec))
	    .append("\n\t\t\ttext = \"").append(content).append('"'); // TODO escape strings
	return endSec;
    }


    public String getFormat() {
	return "Clean Praat TextGrid (words only)";
    }

    public String getExt() {
	return ".w.textgrid";
    }
}
