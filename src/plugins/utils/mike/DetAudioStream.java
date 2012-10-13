/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils.mike;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class DetAudioStream {
	AudioFormat format;
	long framelen;
	public DetAudioStream(AudioInputStream audioIn) {
		format = audioIn.getFormat();
		framelen = audioIn.getFrameLength();
		new Thread(new Runnable() {
			@Override
			public void run() {
				
			}
		}).start();
	}
}
