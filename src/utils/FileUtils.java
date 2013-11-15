package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileUtils {
	
	public static void setEnv(Map<String, String> newenv)
	{
	  try
	    {
	        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
	        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
	        theEnvironmentField.setAccessible(true);
	        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
	        env.putAll(newenv);
	        Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
	        theCaseInsensitiveEnvironmentField.setAccessible(true);
	        Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
	        cienv.putAll(newenv);
	    }
	    catch (NoSuchFieldException e)
	    {
	      try {
	        Class[] classes = Collections.class.getDeclaredClasses();
	        Map<String, String> env = System.getenv();
	        for(Class cl : classes) {
	            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
	                Field field = cl.getDeclaredField("m");
	                field.setAccessible(true);
	                Object obj = field.get(env);
	                Map<String, String> map = (Map<String, String>) obj;
	                map.clear();
	                map.putAll(newenv);
	            }
	        }
	      } catch (Exception e2) {
	        e2.printStackTrace();
	      }
	    } catch (Exception e1) {
	        e1.printStackTrace();
	    } 
	}
	
	public static void recursiveDelete(String path) {
		File f = new File(path);
		File[] fs = f.listFiles(new FileFilter() {
			@Override
			public boolean accept(File x) {
				if (x.isFile()) return true;
				return false;
			}
		});
		if (fs==null) return;
		for (File y : fs) y.delete();
		File[] ds = f.listFiles(new FileFilter() {
			@Override
			public boolean accept(File x) {
				if (x.isDirectory()) return true;
				return false;
			}
		});
		for (File y : ds) recursiveDelete(y.getAbsolutePath());
		f.delete();
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

	public static String[] simpleTokenization(String s) {
		s=s.replace('=', ' ');
		s=s.replace("-", " - ");
		s=s.replace(";", " ;");
		s=s.replace(":", " :");
		s=s.replace("!", " !");
		s=s.replace("?", " ?");
		s=s.replace("``", " `` ");
		s=s.replace("''", " ''");
		s=s.replace("/", " / ");
		s=s.replace("\"", " \" ");
		s=s.replace("[", " [ ");
		s=s.replace("]", " ] ");
		s=s.replace("{", " { ");
		s=s.replace("}", " } ");
		s=s.replace("(", " ( ");
		s=s.replace(")", " ) ");

		// point
		{
			int x=0;
			for (;;) {
				x=s.indexOf('.',x);
				if (x<0) break;
				if (x>0) {
					char c = s.charAt(x-1);
					if (Character.isDigit(c)||Character.isUpperCase(c)) {
						// on le laisse dans le mot
					} else {
						s=s.substring(0,x)+' '+s.substring(x);
						++x;
					}
				}
				++x;
			}
		}

		// virgule
		{
			int x=0,y=0;
			for (;;) {
				x=s.indexOf(',',y);
				if (x<0) break;
				if (x>0) {
					char c = s.charAt(x-1);
					if (Character.isDigit(c)) {
						// on le laisse dans le mot
					} else {
						s=s.substring(0,x)+' '+s.substring(x);
						++x;
					}
				}
				y=x+1;
			}
		}

		// apostrophe
		{
			int x=0,y=0;
			for (;;) {
				x=s.indexOf('\'',y);
				if (x<0) break;
				if (x>0) {
					if (s.substring(x+1).toLowerCase().startsWith("hui")) {
						// on le laisse dans le mot
					} else {
						++x;
						s=s.substring(0,x)+' '+s.substring(x);
					}
				}
				y=x+1;
			}
		}

		s=s.replaceAll("  +", " ");
		s=s.trim();
		if (s.length()==0) return new String[0];
		String[] st = s.split(" ");
		return st;
	}

	public static String guessedEncoding = null;
	public static BufferedReader getReaderGuessEncoding(String nom) {
		final String[] encs = {"UTF-8","ISO-8859-1"};
		final int maxnchars = 1000;
		try {
			for (String enc : encs) {
				System.out.println("try encoding "+enc);
				BufferedReader f= new BufferedReader(new InputStreamReader(new FileInputStream(nom), Charset.forName(enc)));
				int ncharsread = 0;
				while (ncharsread<maxnchars) {
					String s = f.readLine();
					if (s==null) break;
					if (s.indexOf('é')>=0) {
						System.out.println("enc found "+enc);
						guessedEncoding=enc;
						f.close();
						f= new BufferedReader(new InputStreamReader(new FileInputStream(nom), Charset.forName(enc)));
						return f;
					}
					ncharsread+=s.length();
				}
				f.close();
			}
			System.out.println("no encoding found, return locale");
			BufferedReader f = new BufferedReader(new FileReader(nom));
			return f;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * enleve l'extension d'un nom de fichier
	 */
	public static String noExt(String fich) {
		int i=fich.lastIndexOf('.');
		if (i<0) return fich;
		return fich.substring(0,i);
	}

	public static String noExtNoDir(String fich) {
		String s = noExt(fich);
		int i=s.lastIndexOf('/');
		if (i>=0) return s.substring(i+1);
		else return s;
	}
	public static String noDir(String fich) {
		int i=fich.lastIndexOf('/');
		if (i>=0) return fich.substring(i+1);
		else return fich;
	}

	public static List<String> readLines(File f) {
		ArrayList<String> res = new ArrayList<String>();
		try {
			BufferedReader ff = openFileUTF(f.getAbsolutePath());
			for (;;) {
				String s = ff.readLine();
				if (s==null) break;
				res.add(s);
			}
			ff.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	public static BufferedReader openFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(nom),"UTF-8"));
		return f;
	}
	public static BufferedReader openFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(nom),"ISO-8859-1"));
		return f;
	}
	public static PrintWriter writeFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom),"ISO-8859-1"));
		return f;
	}
	public static PrintWriter appendFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom,true),"ISO-8859-1"));
		return f;
	}
	public static PrintWriter appendFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom,true),"UTF-8"));
		return f;
	}
	public static PrintWriter writeFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom),"UTF-8"));
		return f;
	}
	public static PrintWriter writeFileUTF(File ff) throws UnsupportedEncodingException, FileNotFoundException {
		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ff),"UTF-8"));
		return f;
	}

	public static File[] getAllFilesRecurs(String dir, final String ext) {
		File d = new File(dir);
		ArrayList<File> all = new ArrayList<File>();
		File[] lyf = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(ext)) return true;
				return false;
			}
		});
		all.addAll(Arrays.asList(lyf));
		File[] dd = d.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) return true;
				return false;
			}
		});
		for (File ddd : dd) {
			File[] pp = getAllFilesRecurs(ddd.getAbsolutePath(),ext);
			all.addAll(Arrays.asList(pp));
		}
		return all.toArray(new File[all.size()]);
	}


	// UNZIP
	public static final void copyInputStream(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
		int len;
		while((len = in.read(buffer)) >= 0)
			out.write(buffer, 0, len);
		in.close();
		out.close();
	}
	public static final void unzip(String zipfich) {
		Enumeration entries;
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(zipfich);
			entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry)entries.nextElement();
				if(entry.isDirectory()) {
					// Assume directories are stored parents first then children.
					System.err.println("Extracting directory: " + entry.getName());
					// This is not robust, just for demonstration purposes.
					(new File(entry.getName())).mkdir();
					continue;
				}
				System.err.println("Extracting file: " + entry.getName());
				copyInputStream(zipFile.getInputStream(entry),
						new BufferedOutputStream(new FileOutputStream(entry.getName())));
			}
			zipFile.close();
		} catch (IOException ioe) {
			System.err.println("Unhandled exception:");
			ioe.printStackTrace();
			return;
		}
	}

	public static void oldunzip (String zipfile) {
		final int BUFFER = 2048;
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new 
					FileInputStream(zipfile);
			ZipInputStream zis = new 
					ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " +entry);
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = new 
						FileOutputStream(entry.getName());
				dest = new 
						BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) 
						!= -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
