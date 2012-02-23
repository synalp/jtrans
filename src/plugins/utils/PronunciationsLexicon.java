package plugins.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Classe permettant de stocker un dictionnaire de prononciation, type BDLex ou Morphalou
 * 
 * @author cerisara
 *
 */
public abstract class PronunciationsLexicon implements Serializable {

    String dicoperso[] = {
        "/users/parole/cerisara/cvsreps2/ESTER2/dicoLORIA",
        "C:/xtof/ESTER2/dicoLORIA"
    };
    /**
     * Les phonemes utilises en sortie sont les suivants:
     */
    final public static String[] phones = {"a", "an", "b", "bb", "d", "e", "E", "eh", "eu", "euf", "f", "g", "H", "hh", "i", "in", "j", "J", "k", "l", "m", "n", "o", "O", "oh", "on", "p", "R", "s", "S", "sil", "swa", "t", "u", "v", "w", "xx", "y", "z", "Z"};
    /**
     * associe un mot a ses prononciations possibles
     */
    HashMap<String, Entree> dico;
    String curmot;

    /**
     * Cette methode retourne tous les mots du dico sous forme de tableau de String
     * @return
     */
    public String[] getKeys() {
        Object[] tObj = dico.keySet().toArray();
        String[] t = new String[tObj.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = tObj[i].toString();
        }
        return t;
    }

    /**
     * ouvre un fichier texte, eventuellement compresse, et retourne un Iterator
     * permettant de parcourir toutes les entrees (=lignes) textuelles contenues dans ce fichier
     *
     * @param noms tableau de tous les emplacements possibles du fichier a lire
     * @return
     */
    public static Enumeration<String> getEntries(String[] noms, boolean isUTF) {
        int i;
        for (i = 0; i < noms.length; i++) {
            File f = new File(noms[i]);
            if (f != null) {
                break;
            }
        }
        if (i < noms.length) {
            // on a trouve un fichier qui existe: on utilise celui-ci
            final InputStream bif;
            try {
                if (noms[i].endsWith(".zip")) {
                    ZipFile zf = new ZipFile(noms[i]);
                    // TODO: je ne lis que la 1ere entry, mais il faudrait aussi traiter les autres ?!
                    Enumeration it = zf.entries();
                    ZipEntry z = (ZipEntry) it.nextElement();
                    bif = zf.getInputStream(z);
                } else if (noms[i].endsWith(".gz")) {
                    bif = new GZIPInputStream(new FileInputStream(noms[i]));
                } else {
                    bif = new FileInputStream(noms[i]);
                }

                // on a recupere un inputStream vers le fichier
                class MyEnum implements Enumeration<String> {

                    BufferedReader bf = new BufferedReader(new InputStreamReader(bif));
                    String nextLine;

                    public MyEnum(boolean isUTF) {
                        try {
                            if (isUTF) {
                                bf = new BufferedReader(new InputStreamReader(bif, "UTF-8"));
                            } else {
                                bf = new BufferedReader(new InputStreamReader(bif));
                            }
                            nextLine = bf.readLine();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            nextLine = null;
                        }
                    }

                    public String nextElement() {
                        String s = nextLine;
                        try {
                            nextLine = bf.readLine();
                        } catch (IOException e) {
                            nextLine = null;
                        }
                        return s;
                    }

                    public boolean hasMoreElements() {
                        return (nextLine != null);
                    }
                }
                return new MyEnum(isUTF);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @deprecated use getEntries instead !!
     * @param noms
     * @return
     */
    public static BufferedReader getFile(String[] noms) {
        int i;
        for (i = 0; i < noms.length; i++) {
            File f = new File(noms[i]);
            if (f != null) {
                break;
            }
        }
        if (i < noms.length) {
            BufferedReader bf;
            try {
                if (noms[i].endsWith(".zip")) {
                    ZipFile zf = new ZipFile(noms[i]);
                    // TODO: je ne lis que la 1ere entry, mais il faudrait aussi traiter les autres ?!
                    Enumeration it = zf.entries();
                    ZipEntry z = (ZipEntry) it.nextElement();
                    bf = new BufferedReader(new InputStreamReader(zf.getInputStream(z)));
                } else if (noms[i].endsWith(".gz")) {
                    bf = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(noms[i]))));
                } else {
                    bf = new BufferedReader(new FileReader(noms[i]));
                }
                return bf;
            } catch (FileNotFoundException e) {
                // e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    Entree sanstirets(String mot) {
        int i = mot.indexOf('-');
        if (i < 0) {
            return null;
        }
        String[] ss = mot.split("-");
        // on cherche chaque partie individuellement puis on merge si on a TOUTES les parties
        Entree eall = new Entree();
        Entree e = null;
        for (i = 0; i < ss.length; i++) {
            e = dico.get(ss[i]);
            if (e == null) {
                return null;
            }
            eall.phonesBase += e.phonesBase;
        }
        // je ne conserve QUE les liaisons possibles du dernier "mot" du mot compose
        eall.phonesOption = e.phonesOption;
        return eall;
    }

    public List<DicoEntry> getFlexions(String lemme) {
        ArrayList<DicoEntry> l = new ArrayList<DicoEntry>();
        for (Entree e : dico.values()) {
        	if (e.getLemme()==null)
        		System.out.println("ERR FLEX "+e);
        	if (e.getLemme().equals(lemme)) l.add(e);
        }
    	return l;
    }
    
    /**
     * le mot doit correspondre a une entree du lexique.
     * Sinon, retourne null
     */
    public ArrayList<DicoEntry> getDicoEntries(String mot) {
        ArrayList<DicoEntry> l = new ArrayList<DicoEntry>();
        Entree e = dico.get(mot);
        if (e == null) {
            return l;
        }
        l.add(e);
        while (e.autrePossible != null) {
            e = e.autrePossible;
            l.add(e);
        }
        return l;
    }

    /**
     * phonetise une PHRASE
     *
     * @param mot
     * @return
     */
    public String getRule(String mot) {
        if (mot == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(mot);
        if (st.countTokens() > 1) {
            String s = "";
            while (st.hasMoreTokens()) {
                curmot = st.nextToken();
                s += getRule(curmot);
            }
            return s;
        }
        curmot = mot;
        Entree e = dico.get(mot);
        if (e == null) {
            // mot inconnu !
            e = sanstirets(mot);
            if (e == null) {
                return "";
            }
        }
        return e.getRuleAll();
    }

    public static String convertAccents(String s) {
        String r = s.replace("e1", "é");
        r = r.replace("e2", "è");
        r = r.replace("e3", "ê");
        r = r.replace("e4", "ë");
        r = r.replace("a2", "à");
        r = r.replace("a3", "â");
        r = r.replace("a4", "ä");
        r = r.replace("u2", "ù");
        r = r.replace("u3", "û");
        r = r.replace("u4", "ü");
        r = r.replace("i3", "î");
        r = r.replace("i4", "ï");
        r = r.replace("o3", "ô");
        r = r.replace("o4", "ö");
        r = r.replace("c5", "ç");
        return r;
    }

    public static String convertPhones(String s) {
        String r = "";
        int i = 0;
        if (s.charAt(0) == '*') {
            // TODO: pas de liaison possible !
            i++;
        }
        for (; i < s.length(); i++) {
            if (s.charAt(i) == '@') {
                r += " [ swa ]";
            } else if (s.charAt(i) == '*') {
                // le * est normalement place au debut pour indiquer les mots sans liaisons precedentes possibles
                // mais avec les most a tirets, ce * peut aussi se retrouver en milieu de mots !
                // pour le moment on ne le traite pas, mais il faudrait y penser...
            } else if (s.charAt(i) == 'o') {
                if (i < s.length() - 1 && s.charAt(i + 1) == '~') {
                    r += " on";
                    i++;
                } else {
                    r += " oh";
                }
            } else if (s.charAt(i) == 'h' && s.charAt(i + 1) == 'h') {
                r += " hh";
                i++;
            } else if (s.charAt(i) == '2') {
                r += " eu";
            } else if (s.charAt(i) == '9') {
                if (i < s.length() - 1 && s.charAt(i + 1) == '~') {
                    r += " in";
                    i++;
                } else {
                    r += " euf";
                }
            } else if (s.charAt(i) == '6') {
                r += " swa";
            } else if (s.charAt(i) == 'N') {
                r += " n g";
            } else if (s.charAt(i) == 'O' && i < s.length() - 1 && s.charAt(i + 1) == '/') {
                r += " oh";
                i++;
            } else if (s.charAt(i) == 'E' && i < s.length() - 1 && s.charAt(i + 1) == '/') {
                r += " eh";
                i++;
            } else if (s.charAt(i) == 'e' && i < s.length() - 1 && s.charAt(i + 1) == '~') {
                r += " in";
                i++;
            } else if (s.charAt(i) == 'a' && i < s.length() - 1 && s.charAt(i + 1) == '~') {
                r += " an";
                i++;

                // phonemes optionnels
            } else if (s.charAt(i) == '(') {
                r += " [ ";
            } else if (s.charAt(i) == ')') {
                r += " ] ";

                // variantes de prononciation
            } else if (s.charAt(i) == '{') {
                boolean bdlexdefined = false;
                if (s.length() >= i + 5) {
                    if (s.substring(i, i + 5).equals("{O~n}")) {
                        r += " ( oh n | on | on n )";
                        i += 4;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 5).equals("{TSj}")) {
                        r += " ( t S j | S j | t S )";
                        i += 4;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 5).equals("{A~n}")) {
                        r += " ( an | a n )";
                        i += 4;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 5).equals("{a~n}")) {
                        r += " ( an | an n | a n | in n | E n )";
                        i += 4;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 5).equals("{dZj}")) {
                        r += " ( d Z j | Z j | d Z )";
                        i += 4;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 5).equals("{^ks}")) {
                        r += " ( k s | g z )";
                        i += 4;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 5).equals("{^ts}")) {
                        r += " ( t s | d z )";
                        i += 4;
                        bdlexdefined = true;
                    }
                }
                if (!bdlexdefined && s.length() >= i + 4) {
                    if (s.substring(i, i + 4).equals("{6R}")) {
                        r += " ( swa R | E R )";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{6n}")) {
                        r += " ( swa n | E n )";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{6~}")) {
                        r += " ( swa n | in | in n )";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{Ei}")) {
                        r += " E [ j ]";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{TS}")) {
                        r += " [ t ] S";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{ai}")) {
                        r += " ( a j | E )";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{aI}")) {
                        r += " ( a j | i )";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{au}")) {
                        r += " ( a w | u )";
                        i += 3;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 4).equals("{dZ}")) {
                        r += " [ d ] Z";
                        i += 3;
                        bdlexdefined = true;
                    }
                }
                if (!bdlexdefined && s.length() >= i + 3) {
                    if (s.substring(i, i + 3).equals("{E}")) {
                        r += " ( E | swa )";
                        i += 2;
                        bdlexdefined = true;
                    } else if (s.substring(i, i + 3).equals("{x}")) {
                        r += " ( R | Z )";
                        i += 2;
                        bdlexdefined = true;
                    }
                }
                if (!bdlexdefined) {
                    // dans dicoLORIA, on note ainsi les alternatives
                    r += " ( ";
                }
            } else if (s.charAt(i) == '|') {
                // dans dicoLORIA, on note ainsi les alternatives
                r += " | ";
            } else if (s.charAt(i) == '}') {
                // dans dicoLORIA, on note ainsi les alternatives
                r += " ) ";
            } else if (s.charAt(i) == '#') {
                // dans dicoLORIA, on note ainsi les silences
                r += " sil ";

                // ci-dessous: simple phoneme
            } else {
                r += " " + s.charAt(i);
            }
        }
        return r + " ";
    }
}
