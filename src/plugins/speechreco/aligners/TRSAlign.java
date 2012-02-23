/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.aligners;

import java.io.BufferedReader;
import java.io.IOException;
import plugins.applis.SimpleAligneur.Aligneur;
import plugins.utils.FileUtils;

public class TRSAlign {

	static Aligneur aligneur;
	
	public static int seconds2frames(float sec) {
		int f = (int)(sec*100f);
		return f;
	}

	public static String importText(String trsfile) {
		try {
			BufferedReader f = FileUtils.openFileISO(trsfile);
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				if (s.startsWith("<Sync time=")) {
				} else {
					if (s.length()==0||s.charAt(0)=='<' || s.equals("#")) continue;
					s=s.trim();
					if (s.length()<=0) continue;
					String[] ss = s.split(" ");
					for (int i=0;i<ss.length;i++) {
						sb.append(ss[i]+" ");
					}
					sb.append('\n');
				}
			}
			f.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
