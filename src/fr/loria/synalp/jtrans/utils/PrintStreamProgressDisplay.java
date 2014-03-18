package fr.loria.synalp.jtrans.utils;

import java.io.PrintStream;

/**
 * Displays progress on a PrintStream (e.g. System.out, System.err).
 */
public class PrintStreamProgressDisplay implements ProgressDisplay {

	private final PrintStream sink;
	private final int notificationIntervalMS;
	private long lastNotification = 0;


	/**
	 * Constructs a PrintStreamProgressDisplay that outputs messages to
	 * System.out immediately.
	 */
	public PrintStreamProgressDisplay() {
		this(0, System.out);
	}


	/**
	 * Constructs a PrintStreamProgressDisplay that prints progress messages on
	 * the specified PrintStream no more frequently than the specified interval.
	 * @param interval notification interval, in milliseconds
	 * @param sink PrintStream to output messages on
	 */
	public PrintStreamProgressDisplay(int interval, PrintStream sink) {
		notificationIntervalMS = interval;
		this.sink = sink;
	}


	@Override
	public void setIndeterminateProgress(String message) {
		System.out.println(message);
	}


	@Override
	public void setProgress(String message, float f) {
		long now = System.currentTimeMillis();
		long elapsed = now - lastNotification;
		if (f >= 1 || elapsed >= notificationIntervalMS) {
			sink.println(String.format("%s (%.2f%%)", message, f * 100f));
			lastNotification = now;
		}
	}


	@Override
	public void setProgressDone() {
		sink.println("Done.");
	}

}
