/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package fr.loria.synalp.jtrans.test.s4;

import java.io.File;
import java.util.ArrayList;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import junit.framework.TestCase;

public class FETest extends TestCase {
	public void test() {
		String wavname = "/home/xtof/Bureau/culture.wav";
		AudioFileDataSource wavfile = new AudioFileDataSource(3200,null);
		wavfile.setAudioFile(new File(wavname), null);

		ArrayList<DataProcessor> frontEndList = new ArrayList<DataProcessor>();
		frontEndList.add(wavfile);
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f,25.625f,10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));
		frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
		frontEndList.add(new DiscreteCosineTransform(40,13));
		frontEndList.add(new LiveCMN(12,100,160));
		frontEndList.add(new DeltasFeatureExtractor(3));
		
		BaseDataProcessor mfcc = new FrontEnd(frontEndList);
		
		for (int i=0;;i++) {
			Data d = mfcc.getData();
			if (d==null) break;
			if (d instanceof DataEndSignal) {
				System.out.println("EOS");
				break;
			}
			System.out.println("data "+i+" "+d);
		}
	}
	public static void main(String args[]) {
		FETest m = new FETest();
		m.test();
	}
}
