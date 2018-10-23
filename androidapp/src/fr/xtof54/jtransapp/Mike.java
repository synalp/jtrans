package fr.xtof54.jtransapp;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.MediaExtractor;

public class Mike extends InputStream {
	private AudioRecord ar = null;
	private int minSize;
	private ArrayList<Short> buf = null;
	private int curinbuf=0, maxinbuf=0;
	// private MediaRecorder mrec=null;
	private long startRecordTime = 0;
	private boolean contrec = false;

	public static final int SAMPLE_RATE = 16000;

	public Mike() {
	}

	public void startRecord() {
		contrec=true;
		Thread recorder = new Thread(new Runnable() {
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
				int bufsize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				if (bufsize == AudioRecord.ERROR || bufsize == AudioRecord.ERROR_BAD_VALUE) bufsize=SAMPLE_RATE*2;
				short[] wav = new short[bufsize/2];

				// do not use Builder here to support api 9
				AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufsize);
				if (record.getState() != AudioRecord.STATE_INITIALIZED) {
					System.out.println("detjtrapp audio record cannot initialize");
					return;
				}
				record.startRecording();
				try {
					System.out.println("detjtrapp start recording "+wav.length);
					startRecordTime = Calendar.getInstance().getTimeInMillis();
					String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".raw";
					FileChannel fout = new FileOutputStream(PATH_NAME).getChannel();
					ByteBuffer myByteBuffer = ByteBuffer.allocate(wav.length*2);
					myByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
					ShortBuffer myShortBuffer = myByteBuffer.asShortBuffer();
					long shortsRead = 0;
					while (contrec) {
						int nbshorts = record.read(wav,0,wav.length);
						if (nbshorts<0) {
							System.out.println("detjtrapp audio recording error "+nbshorts);
						} else {
							shortsRead += nbshorts;
							myByteBuffer.clear();
							myShortBuffer.clear();
							myShortBuffer.put(wav,0,nbshorts);
							fout.write(myByteBuffer);
						}
						// TODO: conv to mfcc
					}
					record.stop();
					record.release();
					System.out.println("detjtrapp stopped recording "+shortsRead);
					fout.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				JTransapp.main.mikeEnded(); // callback to update the GUI (should use a listener here)
			}
		});
		recorder.start();
	}

	public void stopRecord() {
		contrec=false;
	}

	/*
	public void getRawAudio() {
		System.out.println("detjtrapp mfcc "+startRecordTime);
		if (startRecordTime<=0) return;
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
		MediaDecoder dec = new MediaDecoder(PATH_NAME);
		short[] wav = dec.readShortData();
		System.out.println("detjtrapp shortwav "+wav.length);
	}

	public void replay() {
		if (startRecordTime<=0) return;
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
		try {
			MediaPlayer mplayer = new MediaPlayer();
			mplayer.setDataSource(PATH_NAME);
			mplayer.prepare();
			mplayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void startRecordInFile() {
		if (mrec!=null) return;
		try {
			mrec = new MediaRecorder();
			mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
			mrec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			startRecordTime = Calendar.getInstance().getTimeInMillis();
			String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
			mrec.setOutputFile(PATH_NAME);
			mrec.prepare();
			Thread recorder = new Thread(new Runnable() {
				public void run() {
					mrec.start();   // Recording is now started
				}
			});
			recorder.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopRecordInFile() {
		if (mrec==null) {
			System.out.println("detjtrapp mrec=null in stoprecord");
			return;
		}
		long curtime = Calendar.getInstance().getTimeInMillis();
		if (curtime-startRecordTime<500) {
			// at least 0.5s, otherwise the mediarecorder may crash
			System.out.println("detjtrapp too short in stoprecord");
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mrec.stop();
		mrec.reset();   // You can reuse the object by going back to setAudioSource() step
		mrec.release(); // Now the object cannot be reused
		mrec=null;
		System.out.println("detjtrapp end in stoprecord");
		JTransapp.main.mikeEnded(); // callback to update the GUI (should use a listener here)
	}

	public void startStream() {
		minSize= AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		minSize*=8;
		buf = new byte[minSize];
		curinbuf=0;
		maxinbuf=0;
		ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,minSize);
		ar.startRecording();
	}

	public void stopStream() {
		if (ar != null) {
		    ar.stop();
		}
        }
	*/

	public void resetAudioSource() {
		buf=null;
	}
	@Override
	public int read() {
		if (buf==null) {
			File dir = JTransapp.main.fdir;
			File[] fs = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String nom) {
					return nom.startsWith("recwav_");
				}	
			});
			String[] fss = new String[fs.length];
			for (int i=0;i<fss.length;i++) fss[i]=fs[i].getAbsolutePath();
			java.util.Arrays.sort(fss);
			if (fss.length==0) return -1;
			String PATH_NAME=fss[fss.length-1];
			buf = new ArrayList<Short>();
			DataInputStream fin = null;
			try {
				fin = new DataInputStream(new FileInputStream(PATH_NAME));
				for (;;) buf.add(fin.readShort());
			} catch (Exception e) {}
			try {
				if (fin!=null) fin.close();
			} catch (Exception e) {}
			if (buf==null) return -1;
			System.out.println("detjtrapp readaudio "+buf.size()+" "+PATH_NAME);
			curinbuf=0;
		}
		if (curinbuf>=buf.size()) return -1;
		return buf.get(curinbuf++);
	}

}

