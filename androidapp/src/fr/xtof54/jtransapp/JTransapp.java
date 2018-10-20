package fr.xtof54.jtransapp;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;

public class JTransapp extends Activity {

	@Override
	public void onCreate(Bundle s) {
		super.onCreate(s);
		setContentView(R.layout.main);
	}

	public void quitte(View v) {
		System.exit(1);
	}
}
