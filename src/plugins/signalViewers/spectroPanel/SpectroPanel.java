
package plugins.signalViewers.spectroPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ReplicateScaleFilter;
import java.io.File;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import plugins.applis.SimpleAligneur.Aligneur;

import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

public class SpectroPanel extends JPanel {
	AudioInputStream audioIn;
	FrontEnd fft=null;
    protected BufferedImage spectrogram;
    /** A scaled version of the spectrogram image. */
    protected Image scaledSpectrogram;
    /** Offset factor - what will be subtracted from the image to adjust for noise level. */
    protected double offsetFactor;
    /** The zooming factor. */
    protected float zoom = 1.0f;

    public Aligneur aligneur;
	AudioStreamSource audiosource;
	MFCCbuffer buf;
	
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

	public SpectroPanel() {
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int x=e.getX();
				if (e.getButton()==MouseEvent.BUTTON1) {
					aligneur.clicOnSpectro((float)x/zoom);
					System.out.println("clic at "+x);
				}
			}
		});
	}
	
	private void initFronEnd(AudioInputStream a) {
		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		audiosource = new AudioStreamSource(a);
		frontEndList.add(audiosource);
		frontEndList.add(new DetDataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));
//		frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));

		fft = new FrontEnd(frontEndList);
		
/*
		try {
		JOptionPane.showMessageDialog(null, "bef getdata");
		fft.getData();
		fft.getData();
		fft.getData();
		fft.getData();
		fft.getData();
		fft.getData();
		JOptionPane.showMessageDialog(null, "aft getdata");
		} catch (Exception e ) {
			JOptionPane.showMessageDialog(null, "exc "+e);
		}
		*/
		buf = new MFCCbuffer(fft);
	}

	public void setAudioInputStream(AudioInputStream audio) {
//		AudioFormat f = new AudioFormat(16000, 16, 1, true, true);
//		audioIn = AudioSystem.getAudioInputStream(f, audio);
		try {
			initFronEnd(audio);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	int frBufDeb=-1, frBufEnd=-1;

    private int getFrameForTime(float tsec) {
    	return (int)(tsec*100);
    }

    // relativement au debut de l'audio stream
    private double[] getSpectrumAtFrame(int fr) {
    	if (buf==null) return null;
    	return buf.getMFCCAtFrame(fr);
    }
    
    /** Actually creates the Spectrogram image. */
    protected void computeSpectrogram(int frdeb, int frfin) {
        try {
            /* Run through all the spectra one at a time and convert
             * them to an log intensity value.
             */
            double maxIntensity = Double.MIN_VALUE;
            ArrayList<double[]> intensitiesList = new ArrayList<double[]>();
            
            for (int fr=frdeb;fr<frfin;fr++) {
            	double[] spectrumData = getSpectrumAtFrame(fr);
            	if (spectrumData==null) break;
            	double[] intensities = new double[spectrumData.length];
            	for (int i = 0; i < intensities.length; i++) {
            		/*
            		 * A very small intensity is, for all intents
            		 * and purposes, the same as 0.
            		 */
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

            /* Create the image for displaying the data.
             */
            spectrogram = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);

            /* Set scaleFactor so that the maximum value, after removing
            * the offset, will be 0xff.
            */
            double scaleFactor = ((0xff + offsetFactor) / maxIntensity);

            for (int i = 0; i < width; i++) {
                double[] intensities = intensitiesList.get(i);
                for (int j = maxYIndex; j >= 0; j--) {

                    /* Adjust the grey value to make a value of 0 to mean
                    * white and a value of 0xff to mean black.
                    */
                    int grey = (int) (intensities[j] * scaleFactor
                            - offsetFactor);
                    grey = Math.max(grey, 0);
                    grey = 0xff - grey;

                    /* Turn the grey into a pixel value.
                    */
                    int pixel = ((grey << 16) & 0xff0000)
                            | ((grey << 8) & 0xff00)
                            | (grey & 0xff);

                    spectrogram.setRGB(i, maxYIndex - j, pixel);
                }
            }
            ImageFilter scaleFilter =
                    new ReplicateScaleFilter((int) (zoom * width), height);
            scaledSpectrogram =
                    createImage(new FilteredImageSource(spectrogram.getSource(),
                            scaleFilter));
            Dimension sz = getSize();
            repaint(0, 0, 0, sz.width - 1, sz.height - 1);
        } catch (Exception e) {
            e.printStackTrace();
    		JOptionPane.showMessageDialog(null, "PBBBB "+e);

        }
    }

    public int getScaledWidth() {
    	if (scaledSpectrogram==null) return -1;
    	return scaledSpectrogram.getWidth(null);
    }
    
//    public Dimension getPreferredSize() {
//    	final Dimension dim = new Dimension(getScaledWidth(), 150);
//    	return dim;
//    }
    
    /**
     * Paint the component.  This will be called by AWT/Swing.
     *
     * @param g The <code>Graphics</code> to draw on.
     */
    @Override
    public void paint(Graphics g) {
        /**
         * Fill in the whole image with white.
         */
        Dimension sz = getSize();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sz.width - 1, sz.height - 1);

        if (spectrogram != null) {
            g.drawImage(scaledSpectrogram, 0, 0, null);
        }
    }

    public static void main(String[] args) throws Exception {
    	SpectroPanel m = new SpectroPanel();
    	File f = new File("C:/xx.wav");
    	System.out.println("wav found "+f.exists());
    	AudioInputStream ais = AudioSystem.getAudioInputStream(f);
    	m.setAudioInputStream(ais);
    	m.computeSpectrogram(0, 300);
    	JFrame jf = new JFrame();
    	jf.setSize(200,200);
    	jf.getContentPane().add(m);
    	jf.setVisible(true);
    }
}
