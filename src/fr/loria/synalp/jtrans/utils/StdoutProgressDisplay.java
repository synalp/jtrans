package fr.loria.synalp.jtrans.utils;

public class StdoutProgressDisplay implements ProgressDisplay {
	@Override
	public void setIndeterminateProgress(String message) {
		System.out.println(message);
	}

	@Override
	public void setProgress(String message, float f) {
		System.out.println(String.format(
				"%s (%.2f%%)", message, f*100f));
	}

	@Override
	public void setProgressDone() {
		System.out.println("Done.");
	}
}
