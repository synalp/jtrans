package fr.xtof54.jtransapp;

import java.io.InputStream;
import java.util.Calendar;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.MediaExtractor;

public class Mike extends InputStream {
	private AudioRecord ar = null;
	private int minSize;
	private byte[] buf=null;
	private int curinbuf=0, maxinbuf=0;
	private MediaRecorder mrec=null;
	private long startRecordTime = 0;

	public static final int SAMPLE_RATE = 16000;

	public Mike() {
	}

	public void getRawAudio() {
		if (startRecordTime<=0) return;
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
		try {
			MediaExtractor wavio = new MediaExtractor();
			wavio.setDataSource(new File(PATH_NAME));
			wavio.selectTrack(0);
			ByteBuffer bytebuf = ByteBuffer.allocate();
			while (wavio.readSampleData(bytebuf, 0) >=0) {
				// TODO compute and add in list MFCC frame
				wavio.advance();
			}
			wavio.release();
			wavio=null;
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public void startRecord() {
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

	public void stopRecord() {
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

	@Override
	public int read() {
		if (buf==null) return -1;
		if (curinbuf>=maxinbuf) {
			maxinbuf=ar.read(buf,0,minSize);
			curinbuf=0;
			if (maxinbuf<=0) return -1;
		}
		return buf[curinbuf++];
	}

}

