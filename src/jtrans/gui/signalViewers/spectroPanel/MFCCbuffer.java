package jtrans.gui.signalViewers.spectroPanel;

import javax.swing.JOptionPane;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;

// stocke un buffer MFCC avec un acces en lecture relatif depuis le debut du buffer
public class MFCCbuffer {
	FrontEnd fe;
	double[][] buf = new double[500][];
	int frBufEnd=0;

	public MFCCbuffer(FrontEnd input) {
		fe=input;
	}

	private boolean charge() {
		if (frBufEnd>=buf.length) return false;
		Data d=null;
		for (;;) {
			try {
				d = fe.getData();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "KKK "+e);
			}
			if (d==null || d instanceof DataEndSignal) {
				return false;
			}
			if (d instanceof DoubleData) break;
		}
		DoubleData dd = (DoubleData)d;
		buf[frBufEnd++]=dd.getValues();
		//		System.out.println("debug "+buf[frBufEnd-1][0]);
		return true;
	}

	/**
	 * 
	 * @param fr relative au debut du buffer !
	 * @return
	 */
	public double[] getMFCCAtFrame(int fr) {
		assert fr>=0;

		while (fr>=frBufEnd) {
			if (!charge()) break;
		}
		if (fr>=frBufEnd) {
			return null;
		}
		return buf[fr];
	}
}
