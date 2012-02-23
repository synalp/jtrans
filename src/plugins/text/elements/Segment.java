/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.text.elements;

public class Segment implements Comparable<Segment> {
	public int deb,fin,type;
	public Segment(int a, int b, int t) {deb=a;fin=b;type=t;}
	
	public int compareTo(Segment a) { 
		if (deb>a.deb) return 1;
		if (deb<a.deb) return -1;
		return 0;
	}
}//class Segment
