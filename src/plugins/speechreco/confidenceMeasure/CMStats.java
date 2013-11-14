package plugins.speechreco.confidenceMeasure;

import plugins.speechreco.aligners.sphiinx4.Alignment;

/**
 * cette classe sert à calculer des stats pendant l'alignement pour détecter les décalages 
 * 
 * @author xtof
 *
 */
public class CMStats {
	public static void newAlignedSegment(int motdeb, int motfin, Alignment alwords, int nmots) {
		int nsegs = alwords.getNbSegments();
		int firstfr = alwords.getSegmentDebFrame(0);
		int lastfr = alwords.getSegmentEndFrame(nsegs-1);
		int deltaf=lastfr-firstfr;
		float speakspeed = (float)deltaf/(float)nsegs;
		System.out.println("cmstat "+nsegs+" "+speakspeed+" "+motfin+" "+nmots);
	}
}
