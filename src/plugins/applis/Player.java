/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.applis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import plugins.buffer.RoundBuffer;
import plugins.player.PlayerListener;
import plugins.sourceSignals.TemporalSigFromWavFile;
import utils.PrintLogger;

public class Player {
	public static void main(String args[]) throws Exception {
		TemporalSigFromWavFile wav = new TemporalSigFromWavFile();
		wav.openWavFile(new File(args[0]));
		RoundBuffer buf = new RoundBuffer(new PrintLogger() {
			public void print(String msg) {
				System.out.println(msg);
			}
		},10000000);
		buf.setSource(wav);
		final plugins.player.Player player = new plugins.player.Player(buf,wav.getAudioFormat());
		player.setData(wav);
		player.play(-1,new PlayerListener() {
			@Override
			public void playerHasFinished() {
				player.stopPlaying();
			}
		}, 0);
		BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
		f.readLine();
//		PlayerKeys gui = new PlayerKeys();
//		gui.player=player;
	}
}
