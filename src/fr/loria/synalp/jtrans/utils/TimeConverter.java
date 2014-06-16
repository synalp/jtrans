package fr.loria.synalp.jtrans.utils;

public abstract class TimeConverter {

	public static int FRAMES_PER_SECOND = 100;

	public static float frame2second(int f) {
		return f/FRAMES_PER_SECOND;
	}

	public static int second2frame(float s) {
		return (int)(s*FRAMES_PER_SECOND);
	}

}
