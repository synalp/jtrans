package fr.xtof54.jtransapp;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.graphics.Color;

import java.io.File;
import java.util.List;

public class JTransapp extends Activity {
	public Mike mike=null;
	public static JTransapp main = null;
	public File fdir=null;
	private TextView txt = null;

	@Override
	public void onCreate(Bundle s) {
		super.onCreate(s);
		main = this;
		// fdir = getFilesDir();
		fdir = getExternalFilesDir(null);
		System.out.println("detjtrapp fdir "+fdir.getAbsolutePath());
		mike = new Mike();
		setContentView(R.layout.main);

		txt = (TextView) findViewById(R.id.textid);
		final Button recb = (Button) findViewById(R.id.recb);
		recb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					recb.setBackgroundColor(Color.RED);
					recb.invalidate();
					System.out.println("detjtrapp startrecord");
					mike.startRecord();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mike.stopRecord();
					System.out.println("detjtrapp stoprecord");
					refreshText();
				}

				return true;
			}
		});
		refreshText();
	}

	public void mikeEnded() {
		// called from the mike
		try {
			runOnUiThread(new Runnable() {
				public void run() {
					Button recb = (Button)findViewById(R.id.recb);
					recb.setBackgroundColor(Color.GRAY);
					System.out.println("detjtrapp mikeended");
					recb.invalidate();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clear(View v) {
		if (fdir!=null) {
			File[] fs = fdir.listFiles();
			for (File f: fs) {
				if (f.isFile() && !f.delete()) System.out.println("detjtrapp cannot delete "+f.getAbsolutePath());
			}
		}
		refreshText();
	}
	public void quitte(View v) {
		System.exit(1);
	}
	public void mfcc(View v) {
		mike.resetAudioSource();
		List frames = MFCC.getMFCC(mike);
		System.out.println("detjtrapp "+frames.size());
	}

	public void refreshText() {
		if (fdir!=null) {
			File[] fs = fdir.listFiles();
			if (fs!=null) {
				String nfs = "n="+Integer.toString(fs.length);
				if (txt!=null) {
					txt.setText(nfs);
					txt.invalidate();
				}
			}
		}
	}
}
