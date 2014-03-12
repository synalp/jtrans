package fr.loria.synalp.jtrans.elements;

import fr.loria.synalp.jtrans.utils.TimeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Text element that must be aligned with the recording.
 */
public class Word implements Element {

	private final String word;
	private Segment segment;
	private List<Phone> phones = new ArrayList<Phone>();


	public class Segment {
		private final int start;
		private final int end;

		public Segment(int start, int end) {
			this.start = start;
			this.end = end;
			assert start <= end;
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
	}


	public class Phone {
		private String phone;
		private Segment segment;

		private Phone(String p, Segment s) {
			phone = p;
			segment = s;
		}

		public String toString() {
			return phone;
		}

		public Segment getSegment() {
			return segment;
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


	public List<Phone> getPhones() {
		return phones;
	}

}
