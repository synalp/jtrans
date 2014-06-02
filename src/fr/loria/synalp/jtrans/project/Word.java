package fr.loria.synalp.jtrans.project;

import fr.loria.synalp.jtrans.utils.TimeConverter;
import fr.loria.synalp.jtrans.graph.StatePool;

import java.util.ArrayList;
import java.util.List;

/**
 * Text element that must be aligned with the recording.
 */
public class Word implements Element {

	private final String word;
	private int speaker = -1;
	private Segment segment;
	private List<Phone> phones = new ArrayList<Phone>();
	private boolean anonymize;


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
			return TimeConverter.frame2sec(start);
		}

		public float getEndSecond() {
			return TimeConverter.frame2sec(end);
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
			return TimeConverter.frame2sec(getLengthFrames());
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
			return phone.equals(StatePool.SILENCE_PHONE);
		}
	}


	public Word(String word) {
		this.word = word;
	}


	public String toString() {
		return word;
	}


	public void clearAlignment() {
		segment = null;
		phones.clear();
	}


	public boolean isAligned() {
		return segment != null;
	}


	public Segment getSegment() {
		return segment;
	}


	public void setSegment(int start, int end) {
		segment = new Segment(start, end);
	}


	public void addPhone(String phone, int start, int end) {
		phones.add(new Phone(phone, new Segment(start, end)));
	}


	public void addPhone(Phone phone) {
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
