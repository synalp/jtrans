package fr.xtof54.jtransapp;

import java.io.InputStream;
import java.util.Calendar;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;

public class Mike extends InputStream {
	private AudioRecord ar = null;
	private int minSize;
	private byte[] buf=null;
	private int curinbuf=0, maxinbuf=0;
	private MediaRecorder mrec=null;

	public Mike() {
}

	public void startRecord() {
		if (mrec!=null) return;
		try {
			mrec = new MediaRecorder();
			mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
			mrec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			long currentTime = Calendar.getInstance().getTimeInMillis();
			String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+currentTime+".3gp";
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
		if (mrec==null) return;
		mrec.stop();
		mrec.reset();   // You can reuse the object by going back to setAudioSource() step
		mrec.release(); // Now the object cannot be reused
		mrec=null;
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

