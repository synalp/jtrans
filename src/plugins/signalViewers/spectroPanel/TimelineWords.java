package plugins.signalViewers.spectroPanel;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import plugins.speechreco.aligners.sphiinx4.AlignementEtat;

public class TimelineWords extends JPanel {
	private AlignementEtat align;
	// on peut indiquer soit une trame de debut, soit directement un segment a afficher
	private int firstFrame;
	private int firstSeg=-1;
	private float zoom=1f;

//    public Dimension getPreferredSize() {
//    	final Dimension dim = new Dimension(-1, 40);
//    	return dim;
//    }
    
	public void setAlign(AlignementEtat al) {align=al;}
	public void setFirstSeg(int seg) {
		firstSeg=seg;
		repaint();
	}
	public void setFirstFrame(int fr) {
		firstFrame=fr;
		repaint();
	}
	public void setZoom(float z) {
		zoom=z;
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int w = getWidth();
		int h = getHeight();
		g.drawLine(0, 4, w, 4);
		int seg;
		if (firstSeg<0) {
			int fr = firstFrame;
			if (align==null) return;
			seg = align.getSegmentAtFrame(fr);
			if (seg<0) return;
		} else {
			seg=firstSeg;
			firstFrame=align.getSegmentDebFrame(seg);
		}
		int lastx=0;
		FontMetrics fontMetric = getFontMetrics(getFont());
		for (;seg<align.getNbSegments();seg++) {
			int frlimit = align.getSegmentEndFrame(seg);
			if (frlimit<0) return;
			frlimit-=firstFrame;
			// 1 fr = 
			int xlimit = (int)((float)frlimit*zoom);
			if (xlimit>w) return;
			g.drawLine(xlimit, 4, xlimit, h-30);
			String word = align.getSegmentLabel(seg);
			if (!word.equals("SIL")) {
				int motWidth = SwingUtilities.computeStringWidth(fontMetric, word);
				int motx = (lastx+xlimit-motWidth)/2;
				g.drawString(word, motx, h-10);
			}
			lastx=xlimit;
		}
	}
}
