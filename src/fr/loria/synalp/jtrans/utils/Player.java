package fr.loria.synalp.jtrans.utils;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Player {

	private static final int BUFFER_SIZE = 4096;

	/** Must be {@code null} when not playing back */
	private PlaybackThread thread = null;


	public void play(final AudioInputStream audioStream)
			throws LineUnavailableException
	{
		if (isPlaying()) {
			stop();
		}

		AudioFormat format = audioStream.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		final SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);

		audioLine.open(format);
		audioLine.start();

		assert null == thread;
		thread = new PlaybackThread(audioStream, audioLine);
		thread.start();
	}


	public void stop() {
		if (!isPlaying()) {
			return;
		}

		assert null != thread;
		thread.scheduleStop();

		try {
			thread.join();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		thread = null;
	}


	public boolean isPlaying() {
		return null != thread;
	}


	private class PlaybackThread extends Thread {

		private boolean stop = false;
		private AudioInputStream stream;
		private SourceDataLine line;

		public PlaybackThread(AudioInputStream ais, SourceDataLine sdl) {
			stream = ais;
			line = sdl;
		}

		public void scheduleStop() {
			stop = true;
		}

		@Override
		public void run() {
			byte[] buf = new byte[BUFFER_SIZE];
			int read;

			try {
				while (!stop && (read = stream.read(buf)) >= 0) {
					line.write(buf, 0, read);
				}

				line.drain();
				line.close();
				stream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

}