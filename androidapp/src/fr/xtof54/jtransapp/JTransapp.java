package fr.xtof54.jtransapp;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;

import java.io.File;

public class JTransapp extends Activity {
	public Mike mike=null;
	public static JTransapp main = null;
	public File fdir=null;
	private TextView txt = null;

	@Override
	public void onCreate(Bundle s) {
		super.onCreate(s);
		main = this;
		fdir = getFilesDir();
		mike = new Mike();
		setContentView(R.layout.main);

		txt = (TextView) findViewById(R.id.textid);
		Button recb = (Button) findViewById(R.id.recb);
		recb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					mike.startRecord();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mike.stopRecord();
					refreshText();
				}

				return true;
			}
		});
		refreshText();
	}

	public void quitte(View v) {
		System.exit(1);
	}

	public void refreshText() {
		if (fdir!=null) {
			File[] fs = fdir.listFiles();
			if (fs!=null) {
				int nfs = fs.length;
				if (txt!=null) {
					txt.setText(nfs);
					txt.invalidate();
				}
			}
		}
	}
}
