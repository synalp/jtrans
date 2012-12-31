package utils;

import java.io.File;
import java.io.SequenceInputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class WavUtils {

	public static void main(String[] args) {
		String wavFile1 = "../emospeech/adaptCorpus/voc0.wav";
		String wavFile2 = "../emospeech/adaptCorpus/voc0a.wav";
		String wavFileo = "../emospeech/adaptCorpus/voc0b.wav";
		
		try {
			AudioInputStream clip1 = AudioSystem.getAudioInputStream(new File(wavFile1));
			AudioInputStream clip2 = AudioSystem.getAudioInputStream(new File(wavFile2));

			AudioInputStream appendedFiles = 
					new AudioInputStream(
							new SequenceInputStream(clip1, clip2),     
							clip1.getFormat(), 
							clip1.getFrameLength() + clip2.getFrameLength());

			AudioSystem.write(appendedFiles, 
					AudioFileFormat.Type.WAVE, 
					new File(wavFileo));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
