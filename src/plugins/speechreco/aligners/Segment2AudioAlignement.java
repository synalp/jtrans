package plugins.speechreco.aligners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * contient des segments, représentés par des strings, alignés avec de l'audio (en frames)
 * 
 * @author xtof
 *
 */
public class Segment2AudioAlignement implements Serializable {
	private int firstSegmentModified=-1;
	
	public int getFirstSegmentAltered() {return firstSegmentModified;}
	public void setFirstSegmentAltered(int seg) {firstSegmentModified=seg;}
	
//	public String[] getSegments() {
//		return segments.toArray(new String[segments.size()]);
//	}
	public int getNbSegments() {
		return segments.size();
	}
	public String getSegmentLabel(int segidx) {
		assert segidx>=0;
		assert segidx<segments.size();
		return segments.get(segidx);
	}
	public void setSegmentLabel(int segidx, String newlab) {
		assert segidx<segments.size();
		segments.set(segidx, newlab);
	}
	/**
	 * 
	 * Attention avec cette fonction: elle ne doit etre utilisee QUE lorsqu'on decale l'alignement
	 * DANS LE PASSE !!
	 * 
	 * @param deltat
	 */
	public void addTimeOffset(int deltat) {
		for (int i=0;i<segments.size();i++) {
			int t = segmentsDeb.get(i);
			segmentsDeb.set(i, t+deltat);
			t = segmentsFin.get(i);
			segmentsFin.set(i, t+deltat);
		}
	}
	public void setSegmentFinFrame(int segidx, int fr) {
		segmentsFin.set(segidx, fr);
	}
	public void setSegmentDebFrame(int segidx, int fr) {
		segmentsDeb.set(segidx, fr);
	}
	public int getSegmentFinFrame(int segidx) {
		return segmentsFin.get(segidx);
	}
	public int getSegmentDebFrame(int segidx) {
		return segmentsDeb.get(segidx);
	}
	
	/**
	 * 
	 * @param label
	 * @param deb
	 * @param fin
	 * @return l'ID du segment
	 */
	public int addSegment(String label, int deb, int fin) {

		for (int i=0;i<segments.size();i++) {
			if (deb<segmentsFin.get(i)) {
				if (fin<segmentsDeb.get(i)) {
					segments.add(i, label);
					segmentsDeb.add(i,deb);
					segmentsFin.add(i,fin);
					segmentsSource.add(i,(byte)0);
					if (firstSegmentModified<0) firstSegmentModified=i;
					return i;
				} else {
					System.out.println("ERREUR ADD SEGMENT "+deb+" "+fin+" prev: "+i+" "+segmentsDeb.get(i)+"--"+segmentsFin.get(i));
					String h = null;
					h.charAt(0);
					return -1;
				}
			}
		}
		if (firstSegmentModified<0) firstSegmentModified=segments.size();
		segments.add(label);
		segmentsDeb.add(deb);
		segmentsFin.add(fin);
		segmentsSource.add((byte)0);
		return segments.size()-1;
	}
	public String segToString(int segidx) {
		return segmentsDeb.get(segidx)+"-"+segmentsFin.get(segidx)+":"+segments.get(segidx);
	}
	public String toString(int offset) {
		String s = "";
		for (int i=0;i<segments.size();i++)
			s+=i+" : "+(segmentsDeb.get(i)+offset)+"-"+(segmentsFin.get(i)+offset)+":"+segments.get(i)+"\n";
		return s;
	}
	
	public void cutAfterFrame(int lastFrameToKeep) {
		for (int i=0;i<segments.size();i++) {
			if (segmentsFin.get(i)>lastFrameToKeep) {
				if (firstSegmentModified<0) firstSegmentModified=i;
				segments = segments.subList(0, i);
				segmentsDeb = segmentsDeb.subList(0, i);
				segmentsFin = segmentsFin.subList(0, i);
				segmentsSource =  segmentsSource.subList(0, i);
				break;
			}
		}
	}
	public void cutBeforeFrame(int fr) {
		for (int i=segments.size()-1;i>=0;i--) {
			if (segmentsDeb.get(i)<fr) {
				i++;
				int z = segments.size();
				segments = segments.subList(i,z);
				segmentsDeb = segmentsDeb.subList(i,z);
				segmentsFin = segmentsFin.subList(i,z);
				segmentsSource =  segmentsSource.subList(i, z);
				break;
			}
		}
	}
	
	public void load(BufferedReader f) {
		try {
			String s = f.readLine();
			int n = Integer.parseInt(s);
			for (int i=0;i<n;i++) {
				s = f.readLine();
				if (firstSegmentModified<0) firstSegmentModified=segments.size();
				segments.add(s);
				s = f.readLine();
				String[] ss = s.split(" ");
				segmentsDeb.add(Integer.parseInt(ss[0]));
				segmentsFin.add(Integer.parseInt(ss[1]));
				byte src=0;
				if (ss.length>2) src=Byte.parseByte(ss[2]);
				segmentsSource.add(src);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void save(PrintWriter f) {
		f.println(segments.size());
		for (int i=0;i<segments.size();i++) {
			f.println(segments.get(i));
			f.println(segmentsDeb.get(i)+" "+segmentsFin.get(i)+" "+segmentsSource.get(i));
		}
	}
	
	public void delSeg(int i) {
		segments.remove(i);
		segmentsDeb.remove(i);
		segmentsFin.remove(i);
		segmentsSource.remove(i);
	}
	
	public void setSegmentSourceManu(int segidx) {segmentsSource.set(segidx, (byte)1);}
	public void setSegmentSourceEqui(int segidx) {segmentsSource.set(segidx, (byte)2);}
	
	private List<String> segments = new ArrayList<String>();
	private List<Integer> segmentsDeb = new ArrayList<Integer>();
	private List<Integer> segmentsFin = new ArrayList<Integer>();
	// contient la source de l'alignement = Auto/Manu/Equi
	// 0=Auto 1=Manu 2=Equi
	private List<Byte> segmentsSource = new ArrayList<Byte>();
}
