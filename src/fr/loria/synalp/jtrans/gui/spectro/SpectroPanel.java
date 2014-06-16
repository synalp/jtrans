
package fr.loria.synalp.jtrans.gui.spectro;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ReplicateScaleFilter;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.swing.JPanel;

import edu.cmu.sphinx.frontend.FloatData;
import fr.loria.synalp.jtrans.gui.JTransGUI;
import fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer;

public class SpectroPanel extends JPanel {

    protected BufferedImage spectrogram;
    /** A scaled version of the spectrogram image. */
    protected Image scaledSpectrogram;
    /** Offset factor - what will be subtracted from the image to adjust for noise level. */
    protected double offsetFactor;
    /** The zooming factor. */
    protected float zoom = 1.0f;

	List<FloatData> buf;
	
    /**
     * Updates the offset factor used to calculate the greyscale values from the intensities.  This also calculates and
     * populates all the greyscale values in the image.
     *
     * @param offsetFactor the offset factor used to calculate the greyscale values from the intensities; this is used
     *                     to adjust the level of background noise that shows up in the image
     */
	public void setOffsetF(double v) {offsetFactor=v;}
	public double getOffsetF() {return offsetFactor;}
	
	public void setZoom(float v) {zoom=v;}
	public float getZoom() {return zoom;}

	public SpectroPanel(final JTransGUI gui) {
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int x=e.getX();
				if (e.getButton()==MouseEvent.BUTTON1) {
					gui.clicOnSpectro((float) x / zoom);
					System.out.println("clic at "+x);
				}
			}
		});
	}

	public void setAudioInputStream(AudioInputStream audio) {
		if (null == audio) {
			buf = null;
		} else {
			buf = S4mfccBuffer.getAllData(audio, false);
		}
	}

    /** Actually creates the Spectrogram image. */
    protected void computeSpectrogram(int startFrame, int endFrame) {
		if (null == buf) {
			return;
		}

		// Check bounds if asking for more frames than available
		endFrame = Math.min(buf.size(), endFrame);

		// Run through all the spectra one at a time and convert
		// them to a log intensity value.
		double maxIntensity = Double.MIN_VALUE;
		ArrayList<double[]> intensitiesList = new ArrayList<>();

		for (int f = startFrame; f < endFrame; f++) {
			float[] spectrumData = buf.get(f).getValues();
			if (spectrumData==null) {
				System.err.println("null spectrum data at frame " + f);
				break;
			}
			double[] intensities = new double[spectrumData.length];
			for (int i = 0; i < intensities.length; i++) {
				// A very small intensity is, for all intents
				// and purposes, the same as 0.
				intensities[i] = Math.max(Math.log(spectrumData[i]),0.0);
				if (intensities[i] > maxIntensity) {
					maxIntensity = intensities[i];
				}
			}
			intensitiesList.add(intensities);
		}

		int width = intensitiesList.size();
		if (width<=0) return;
		int height = (intensitiesList.get(0)).length;
		int maxYIndex = height - 1;
		Dimension d = new Dimension((int)(zoom*width), height);

		setMinimumSize(d);
		setMaximumSize(d);
		setPreferredSize(d);

		// Create the image for displaying the data.
		spectrogram = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);

		// Set scaleFactor so that the maximum value, after removing
		// the offset, will be 0xff.
		double scaleFactor = ((0xff + offsetFactor) / maxIntensity);

		for (int i = 0; i < width; i++) {
			double[] intensities = intensitiesList.get(i);
			for (int j = maxYIndex; j >= 0; j--) {

				int grey = (int) (intensities[j] * scaleFactor
						- offsetFactor);
				grey = Math.max(grey, 0); // 0 = black
				grey = 0xff - grey;       // 0xff = white

				// Turn the grey into a pixel value.
				int pixel = ((grey << 16) & 0xff0000)
						| ((grey << 8) & 0xff00)
						| (grey & 0xff);

				spectrogram.setRGB(i, maxYIndex - j, pixel);
			}
		}
		ImageFilter scaleFilter =
				new ReplicateScaleFilter((int) (zoom * width), height);
		scaledSpectrogram = createImage(
				new FilteredImageSource(spectrogram.getSource(), scaleFilter));
		repaint(0, 0, 0, getWidth() - 1, getHeight() - 1);
    }

    @Override
    public void paint(Graphics g) {
        Dimension sz = getSize();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sz.width - 1, sz.height - 1);

        if (spectrogram != null) {
            g.drawImage(scaledSpectrogram, 0, 0, null);
        }
    }

}
