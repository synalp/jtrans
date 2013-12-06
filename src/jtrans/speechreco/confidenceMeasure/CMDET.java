/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.confidenceMeasure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import jtrans.utils.DET;

public class CMDET {
	public static void main(String args[]) throws IOException {
		BufferedReader f = new  BufferedReader(new FileReader(args[0]));
		DET det = new DET();
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			if (s.startsWith("ALIGN")) {
				String[] ss = s.split(" ");
				try {
					float cm = Float.parseFloat(ss[ss.length-1]);
					int i = s.indexOf("ALIGN PAS BON");
					if (i>=0) {
						// pas bon
						det.updateExample(false, cm);
					} else {
						// bon
						det.updateExample(true, cm);
					}
				} catch (NumberFormatException e) {
				}
			}
		}
		f.close();
		det.showDET();
	}
}
