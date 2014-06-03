package fr.loria.synalp.jtrans.gui.signalViewers.spectroPanel;

import fr.loria.synalp.jtrans.project.Token;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.util.List;

public class TimelineWords extends JPanel {
	private List<Token> words;
	private int firstFrame;
	private float zoom=1f;
	private FontMetrics metrics;

	public TimelineWords() {
		setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		metrics = getFontMetrics(getFont());
	}

	public void setTokens(List<Token> words) {
		this.words = words;
	}

	public void setFirstFrame(int fr) {
		firstFrame=fr;
		repaint();
	}

	public void setZoom(float z) {
		zoom=z;
		repaint();
	}

	private int transformX(int frame) {
		return (int)((float)(frame-firstFrame) * zoom);
	}

	private void drawSegment(Graphics g, String str, Token.Segment seg, int y, int tickHeight) {
		int x1 = transformX(seg.getStartFrame());
		int x2 = transformX(seg.getEndFrame());
		int segW = SwingUtilities.computeStringWidth(metrics, str);

		g.setColor(Color.GRAY);
		g.drawLine(x1, 0, x1, tickHeight);

		g.setColor(getForeground());
		g.drawString(str, (x1 + x2 - segW) / 2, y);
	}

	public void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D)g0;

		g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		super.paintComponent(g);

		if (words == null) {
			return;
		}

		int w = getWidth();
		int h = getHeight();

		final int lastFrame = firstFrame + (int)(w/zoom);

		for (Token word: words) {
			if (!word.isAligned() ||
					word.getSegment().getEndFrame() < firstFrame)
			{
				continue;
			} else if (word.getSegment().getStartFrame() > lastFrame) {
				return;
			}

			drawSegment(g, word.toString(), word.getSegment(), h-5, h);
			for (Token.Phone p: word.getPhones()) {
				drawSegment(g, p.toString(), p.getSegment(), h/2-5, 3);
			}
		}
	}
}
