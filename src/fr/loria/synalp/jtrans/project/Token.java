package fr.loria.synalp.jtrans.project;

import static fr.loria.synalp.jtrans.speechreco.s4.S4mfccBuffer.frame2second;
import static fr.loria.synalp.jtrans.graph.StatePool.SILENCE_PHONE;

import java.util.ArrayList;
import java.util.List;

public class Token {

	private final String text;
	private final Type type;
	private int speaker = -1;
	private Segment segment;
	private final List<Phone> phones;
	private boolean anonymize;


	public static enum Type {
		/** Alignable token */
		WORD,
		COMMENT,
		NOISE,
		PUNCTUATION,
		OVERLAP_START_MARK,
		OVERLAP_END_MARK,
		SPEAKER_MARK,
	}

	public Type getType() {
		return type;
	}

	public boolean isAlignable() {
		return type == Type.WORD;
	}


	public static class Segment {
		private int start;
		private int end;

		public Segment(int start, int end) {
			this.start = start;
			this.end = end;
			assert start <= end;
		}

		public void setStartFrame(int f) {
			start = f;
		}

		public void setEndFrame(int f) {
			end = f;
		}

		public float getStartSecond() {
			return frame2second(start);
		}

		public float getEndSecond() {
			return frame2second(end);
		}

		public int getStartFrame() {
			return start;
		}

		public int getEndFrame() {
			return end;
		}

		public int getLengthFrames() {
			return end-start+1;
		}

		public float getLengthSeconds() {
			return frame2second(getLengthFrames());
		}
	}


	public static class Phone {
		private String phone;
		private Segment segment;

		public Phone(String p, Segment s) {
			phone = p;
			segment = s;
		}

		public String toString() {
			return phone;
		}

		public Segment getSegment() {
			return segment;
		}

		public boolean isSilence() {
			return phone.equals(SILENCE_PHONE);
		}
	}


	/**
	 * Constructs an alignable token (i.e. a "word").
	 */
	public Token(String text) {
		this(text, Type.WORD);
	}


	public Token(String text, Type type) {
		this.text = text;
		this.type = type;

		if (type == Type.WORD) {
			phones = new ArrayList<>();
		} else {
			phones = null;
		}
	}


	public String toString() {
		return isAlignable()? text: "["+text+"]";
	}


	public void clearAlignment() {
		if (isAlignable()) {
			segment = null;
			phones.clear();
		}
		assert !isAligned();
	}


	public boolean isAligned() {
		return isAlignable() && segment != null;
	}


	public Segment getSegment() {
		return segment;
	}


	public void setSegment(int start, int end) {
		assert isAlignable();
		segment = new Segment(start, end);
	}


	public void addPhone(Phone phone) {
		assert isAlignable();
		phones.add(phone);
	}


	public List<Phone> getPhones() {
		return phones;
	}


	public void setAnonymize(boolean anonymize) {
		this.anonymize = anonymize;
	}


	public boolean shouldBeAnonymized() {
		return anonymize;
	}


	/**
	 * @return -1 if the entire word is silent
	 */
	public int getFirstNonSilenceFrame() {
		for (Phone p: phones) {
			if (!p.isSilence()) {
				return p.getSegment().getStartFrame();
			}
		}
		return -1;
	}


	/**
	 * @return -1 if the entire word is silent
	 */
	public int getLastNonSilenceFrame() {
		for (int i = phones.size()-1; i >= 0; i--) {
			if (!phones.get(i).isSilence()) {
				return phones.get(i).getSegment().getEndFrame();
			}
		}
		return -1;
	}


	public int getSpeaker() {
		return speaker;
	}


	public void setSpeaker(int id) {
		speaker = id;
	}

}
