/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package plugins.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JOptionPane;

public class FileUtils {

	/**
	 * @obsolete please prefer to use getRessource
	 * @param classname
	 * @param nom
	 * @return
	 */
	public static InputStream getRessourceAsStream(String classname, String nom) {
		try {
			// cherche d'abord dans le .jar
			InputStream res = Class.forName(classname).getResourceAsStream(nom);
			if (res!=null) return res;
			// sinon recupere le basedir
			URL url = Class.forName(classname).getResource("/jtrans.jar");
			int i=url.toString().indexOf("dist/jtrans.jar");
			if (i<0) {
				System.err.println("ERROR getResources "+classname+" "+nom+" "+url);
				return null;
			}
			String prefix = (new URL(url.toString().substring(0,i))).getPath();
			String fich = prefix+nom;
			File f = new File(fich);
			if (f.exists()) return new FileInputStream(f);
			InputStream is = Class.forName(classname).getResourceAsStream("/"+nom);
			return is;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String getRessource(String classname, String nom) {
		try {
			// cherche d'abord dans le .jar
			URL res = Class.forName(classname).getResource(nom);
			if (res!=null) return res.getPath();
			System.out.println("ressource not found in jar");
			// sinon cherche le fichier soit dans le repertoire du .jar...
			URL url = Class.forName(classname).getResource("/jtrans.jar");
			System.out.println("getRessource jar file: "+url);
			int i=url.toString().indexOf("jtrans.jar");
			if (i<0) {
				System.err.println("ERROR getResources no jar dir found: "+classname+" "+nom+" "+url);
				return null;
			}
			String prefix = (new URL(url.toString().substring(0,i))).getPath();
			String fich = prefix+nom;
			File f = new File(fich);
			System.out.println("looking for file "+f.getAbsolutePath());
			if (f.exists()) return f.getAbsolutePath();
			// ... soit un repertoire au-dessus du .jar
			int j=url.toString().lastIndexOf('/', i-2);
			if (j<0) {
				System.err.println("ERROR getResources no jar dir found: "+classname+" "+nom+" "+url);
				return null;
			} else j++;
			prefix = (new URL(url.toString().substring(0,j))).getPath();
			fich = prefix+nom;
			f = new File(fich);
			System.out.println("looking for file "+f.getAbsolutePath());
			if (f.exists()) return f.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static URL getRessourceAsURL(String classname, String nom) {
		try {
			// cherche d'abord dans le .jar
			URL res = Class.forName(classname).getResource(nom);
			if (res!=null) return res;
			// sinon recupere le basedir
			URL url = Class.forName(classname).getResource("/jtrans.jar");
			int i=url.toString().indexOf("dist/jtrans.jar");
			if (i<0) {
				System.err.println("ERROR getResources "+classname+" "+nom+" "+url);
				return null;
			}
			String prefix = (new URL(url.toString().substring(0,i))).getPath();
			String fich = prefix+nom;
			File f = new File(fich);
			if (f.exists()) return new URL(url.toString().substring(0,i)+nom);
			else return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static FileChannel fileChannel=null;
	public static BufferedReader openFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		FileInputStream fis = new FileInputStream(nom);
		fileChannel = fis.getChannel();
		BufferedReader f = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		return f;
	}

	public static BufferedReader openFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		FileInputStream fis = new FileInputStream(nom);
		fileChannel = fis.getChannel();
		BufferedReader f = new BufferedReader(new InputStreamReader(fis, "ISO-8859-1"));
		return f;
	}

	public static PrintWriter writeFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom), "ISO-8859-1"));
		return f;
	}

	public static PrintWriter writeFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom), "UTF-8"));
		return f;
	}

	public static void main(String args[]) throws Exception {
		String s = "http://rapsodis.loria.fr/jtrans/culture.jtr";
		BufferedReader f=openFileURL(s);
	}

	public static InputStream findFileOrUrl(String path) {
		if (path.startsWith("http://") || path.startsWith("file://")) {
			try {
				URL u = new URL(path);
				return u.openStream();
			} catch (Exception e1) {
				ErrorsReporting.report(e1);
			}
		} else {
			try {
				FileInputStream fis = new FileInputStream(path);
				return fis;
			} catch (Exception e) {
				try {
					InputStream is = Runtime.getRuntime().getClass().getResourceAsStream(path);
					if (is!=null) return is;
				} catch (Exception ee) {
					ErrorsReporting.report("NO RESSUORCE "+path);
					ErrorsReporting.report(ee);
				}
			}
		}
		return null;
	}
	public static AudioInputStream findAudioFileOrUrl(String path) {
		if (path.startsWith("http://") || path.startsWith("file://")) {
			try {
				URL u = new URL(path);
				return AudioSystem.getAudioInputStream(u);
			} catch (Exception e1) {
				ErrorsReporting.report(e1);
			}
		} else {
			try {
				File f = new File(path);
				try {
					if (f.exists()) {
						return AudioSystem.getAudioInputStream(f);
					}
				} catch (Exception e) {
				} finally {
					URL u = Runtime.getRuntime().getClass().getResource(path);
					if (u!=null) return AudioSystem.getAudioInputStream(u);
				}
			} catch (Exception ee) {
				ErrorsReporting.report("NO RESSUORCE "+path);
				ErrorsReporting.report(ee);
			}
		}
		return null;
	}

	public static BufferedReader openFileOrURL(String url) {
		if (url.startsWith("http://") || url.startsWith("file://")) {
			return openFileURL(url);
		} else {
			try {
				return openFileUTF(url);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "inpute stream failed "+e);
				e.printStackTrace();
				return null;
			}
		}
	}

	public static BufferedReader openFileURL(String url) {
		try {
			URL xurl = new URL(url);
			InputStream i = xurl.openStream();
			return new BufferedReader(new InputStreamReader(i,Charset.forName("UTF-8")));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "inpute stream failed "+e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * enleve l'extension d'un nom de fichier
	 */
	public static String noExt(String fich) {
		int i = fich.lastIndexOf('.');
		if (i < 0) {
			return fich;
		}
		return fich.substring(0, i);
	}

	public static String noExtNoDir(String fich) {
		String s = noExt(fich);
		int i=s.lastIndexOf('/');
		if (i>=0) return s.substring(i+1);
		else return s;
	}

	boolean utf;
	BufferedReader f;
	int nextChar = -1;
	public FileUtils(String textfile, boolean isUTF) {
		utf=isUTF;
		String enc = utf? "UTF-8": "ISO-8859-1";
		try {
			InputStream is = findFileOrUrl(textfile);
			f = new BufferedReader(new InputStreamReader(is, Charset.forName(enc)));
		} catch (Exception e) {
			ErrorsReporting.report(e);
		}
	}
	public String readLineWithEOL() {
		try {
			StringBuilder sb = new StringBuilder();
			if (nextChar>=0) sb.append(nextChar);
			nextChar=-1;
			for (;;) {
				int c = f.read();
				if (c<0) {
					if (sb.length()==0) return null;
					break;
				}
				sb.append((char)c);
				if (c=='\n') break;
				if (c=='\r') {
					int d = f.read();
					if (d!='\n') nextChar=d;
					else
						sb.append((char)d);
					break;
				}
			}
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public void close() {
		try {
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String removeEOL(String s) {
		if (s.length()==0) return s;
		int i=s.length()-1;
		char c = s.charAt(i);
		if (c=='\r') return s.substring(0,i);
		if (c=='\n'&&i>1) {
			c=s.charAt(i-1);
			if (c=='\r') return s.substring(0,i-1);
			else return s.substring(0,i);
		}
		return s;
	}
}
