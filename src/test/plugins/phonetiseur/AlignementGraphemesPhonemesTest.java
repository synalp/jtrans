package test.plugins.phonetiseur;

import java.util.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import plugins.phonetiseur.AlignementGraphemesPhonemes;
import plugins.phonetiseur.Configuration;

import static org.junit.Assert.*;

public class AlignementGraphemesPhonemesTest {

    private final String[] graphemes1 = {"m", "a", "i", "s", "o", "n"};
    private final String[] phonemes1 = {"m", "E", Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE, "z", "o~", Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE};
    private LinkedList<String> lGraphemes2 = new LinkedList<String>();
    private LinkedList<String> lPhonemes2 = new LinkedList<String>();
    private ArrayList<String> alGraphemes3 = new ArrayList<String>();
    private ArrayList<String> alPhonemes3 = new ArrayList<String>();
    private ArrayList<String> lGraphemes4 = new ArrayList<String>();
    private ArrayList<String> lPhonemes4 = new ArrayList<String>();
    private final AlignementGraphemesPhonemes agp1 = new AlignementGraphemesPhonemes(graphemes1, phonemes1);
    private AlignementGraphemesPhonemes agp2;
    private AlignementGraphemesPhonemes agp3;
    private AlignementGraphemesPhonemes agp4;

    public AlignementGraphemesPhonemesTest() {
    }

    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {

        // n� 2
        lGraphemes2.add("m");
        lGraphemes2.add("a");
        lGraphemes2.add("l");
        lGraphemes2.add("o");
        lGraphemes2.add("t");
        lGraphemes2.add("r");
        lGraphemes2.add("u");

        lPhonemes2.add("m");
        lPhonemes2.add("a");
        lPhonemes2.add("l");
        lPhonemes2.add("O");
        lPhonemes2.add("t");
        lPhonemes2.add("R");
        lPhonemes2.add("y");

        agp2 = new AlignementGraphemesPhonemes(lGraphemes2, lPhonemes2);
        agp2.setScore(.6);

        // n�3
        alGraphemes3.add("x");
        alGraphemes3.add(Configuration.STRING_DE_REMPLACEMENT_GRAPHEME_VIDE);
        alGraphemes3.add("y");
        alGraphemes3.add("l");
        alGraphemes3.add("o");
        alGraphemes3.add("c");
        alGraphemes3.add("o");
        alGraphemes3.add("p");
        alGraphemes3.add("e");

        alPhonemes3.add("k");
        alPhonemes3.add("s");
        alPhonemes3.add("i");
        alPhonemes3.add("l");
        alPhonemes3.add("O");
        alPhonemes3.add("k");
        alPhonemes3.add("O");
        alPhonemes3.add("p");
        alPhonemes3.add("@");

        agp3 = new AlignementGraphemesPhonemes(alGraphemes3, alPhonemes3);
        agp3.setScore(.6);

        // n� 4
        lGraphemes4.add("e");
        lGraphemes4.add("x");
        lGraphemes4.add("_");
        lGraphemes4.add("o");

        lPhonemes4.add("E");
        lPhonemes4.add("g");
        lPhonemes4.add("z");
        lPhonemes4.add("O");

        agp4 = new AlignementGraphemesPhonemes(lGraphemes4, lPhonemes4);
        agp4.setScore(.01);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getTailleAlignement method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetTailleAlignement() {
        System.out.println("AlignementGraphemesPhonemes.getTailleAlignement");
        assertEquals(agp1.getTailleAlignement(), 6);
        assertEquals(agp2.getTailleAlignement(), 7);
        assertEquals(agp3.getTailleAlignement(), 9);
    }

    /**
     * Test of getScore method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetScore() {
        System.out.println("AlignementGraphemesPhonemes.getScore");
        assertEquals(agp1.getScore(), -1, 0.00000001);
        assertEquals(agp2.getScore(), 0.6, 0.00000001);
        assertEquals(agp3.getScore(), 0.6, 0.00000001);
    }

    /**
     * Test of getGrapheme method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetGrapheme() {
        System.out.println("AlignementGraphemesPhonemes.getGrapheme");
        assertEquals(agp1.getGrapheme(0), "m");
        assertEquals(agp1.getGrapheme(1), "a");
        assertEquals(agp1.getGrapheme(2), "i");
        assertEquals(agp1.getGrapheme(3), "s");
        assertEquals(agp1.getGrapheme(4), "o");
        assertEquals(agp1.getGrapheme(5), "n");

        assertEquals(agp2.getGrapheme(0), "m");
        assertEquals(agp2.getGrapheme(1), "a");
        assertEquals(agp2.getGrapheme(2), "l");
        assertEquals(agp2.getGrapheme(3), "o");
        assertEquals(agp2.getGrapheme(4), "t");
        assertEquals(agp2.getGrapheme(5), "r");
        assertEquals(agp2.getGrapheme(6), "u");

        assertEquals(agp3.getGrapheme(0), "x");
        assertEquals(agp3.getGrapheme(1), Configuration.STRING_DE_REMPLACEMENT_GRAPHEME_VIDE + "");
        assertEquals(agp3.getGrapheme(2), "y");
        assertEquals(agp3.getGrapheme(3), "l");
        assertEquals(agp3.getGrapheme(4), "o");
        assertEquals(agp3.getGrapheme(5), "c");
        assertEquals(agp3.getGrapheme(6), "o");
        assertEquals(agp3.getGrapheme(7), "p");
        assertEquals(agp3.getGrapheme(8), "e");
    }

    /**
     * Test of getPhoneme method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetPhoneme() {
        System.out.println("AlignementGraphemesPhonemes.getPhoneme");
        assertEquals(agp1.getPhoneme(0), "m");
        assertEquals(agp1.getPhoneme(1), "E");
        assertEquals(agp1.getPhoneme(2), Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE + "");
        assertEquals(agp1.getPhoneme(3), "z");
        assertEquals(agp1.getPhoneme(4), "o~");
        assertEquals(agp1.getPhoneme(5), Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE + "");

        assertEquals(agp2.getPhoneme(0), "m");
        assertEquals(agp2.getPhoneme(1), "a");
        assertEquals(agp2.getPhoneme(2), "l");
        assertEquals(agp2.getPhoneme(3), "O");
        assertEquals(agp2.getPhoneme(4), "t");
        assertEquals(agp2.getPhoneme(5), "R");
        assertEquals(agp2.getPhoneme(6), "y");

        assertEquals(agp3.getPhoneme(0), "k");
        assertEquals(agp3.getPhoneme(1), "s");
        assertEquals(agp3.getPhoneme(2), "i");
        assertEquals(agp3.getPhoneme(3), "l");
        assertEquals(agp3.getPhoneme(4), "O");
        assertEquals(agp3.getPhoneme(5), "k");
        assertEquals(agp3.getPhoneme(6), "O");
        assertEquals(agp3.getPhoneme(7), "p");
        assertEquals(agp3.getPhoneme(8), "@");
    }

    /**
     * Test of getVecteursString method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetVecteursString_1() {
        System.out.println("AlignementGraphemesPhonemes.getVecteursString_1");
        String str1 = agp1.getVecteursString("$", "�", "PoStAg", "_", " ");

        String str2 =
                "m,$,$,$,$,a,i,s,o,PoStAg,m\n" +
                "a,m,$,$,$,i,s,o,n,PoStAg,E\n" +
                "i,a,m,$,$,s,o,n,�,PoStAg," + Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE + "\n" +
                "s,i,a,m,$,o,n,�,�,PoStAg,z\n" +
                "o,s,i,a,m,n,�,�,�,PoStAg,o~\n" +
                "n,o,s,i,a,�,�,�,�,PoStAg," + Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE + "";
        assertEquals(str1, str2);
    }

    /**
     * Test of getVecteursString method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetVecteursString_2() {
        System.out.println("AlignementGraphemesPhonemes.getVecteursString_2");
        String str1 = agp4.getVecteursString("$", "�", "PoStAg", "_", " ");
        String str2 =
                "e,$,$,$,$,x,o,�,�,PoStAg,E\n" +
                "x,e,$,$,$,o,�,�,�,PoStAg,g z\n" +
                "o,x,e,$,$,�,�,�,�,PoStAg,O";
        assertEquals(str1, str2);
    }

    /**
     * Test of getVecteur_Methode_4Classifieurs method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetVecteur_Methode_4Classifieurs() {
        System.out.println("AlignementGraphemesPhonemes.getVecteur_Methode_4Classifieurs");
        String[] str1 = agp4.getVecteur_Methode_4Classifieurs("$", "�", "PoStAg", "_");
        String[] str2 = new String[4];

        str2[0] = "e,$,$,$,$,x,o,�,�,PoStAg,simple\n" +
                "x,e,$,$,$,o,�,�,�,PoStAg,double\n" +
                "o,x,e,$,$,�,�,�,�,PoStAg,simple";

        str2[1] = "e,$,$,$,$,x,o,�,�,PoStAg,E\n" +
                "o,x,e,$,$,�,�,�,�,PoStAg,O";

        str2[2] = "x,e,$,$,$,o,�,�,�,PoStAg,g";

        str2[3] = "x,g,e,$,$,$,o,�,�,�,PoStAg,z";

        assertEquals(str1[0], str2[0]);
        assertEquals(str1[1], str2[1]);
        assertEquals(str1[2], str2[2]);
        assertEquals(str1[3], str2[3]);
    }

    /**
     * Test of getVecteur_Methode_4Classifieurs method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetVecteur_Methode_4Classifieurs_2() {
        System.out.println("AlignementGraphemesPhonemes.getVecteur_Methode_4Classifieurs_2");
        String[] str1, str2;

        str1 = agp1.getVecteur_Methode_4Classifieurs("$", "�", "PoStAg", "_");
        str2 = new String[4];

        str2[0] = "m,$,$,$,$,a,i,s,o,PoStAg,simple\n" +
                "a,m,$,$,$,i,s,o,n,PoStAg,simple\n" +
                "i,a,m,$,$,s,o,n,�,PoStAg,simple\n" +
                "s,i,a,m,$,o,n,�,�,PoStAg,simple\n" +
                "o,s,i,a,m,n,�,�,�,PoStAg,simple\n" +
                "n,o,s,i,a,�,�,�,�,PoStAg,simple";

        str2[1] = "m,$,$,$,$,a,i,s,o,PoStAg,m\n" +
                "a,m,$,$,$,i,s,o,n,PoStAg,E\n" +
                "i,a,m,$,$,s,o,n,�,PoStAg,_\n" +
                "s,i,a,m,$,o,n,�,�,PoStAg,z\n" +
                "o,s,i,a,m,n,�,�,�,PoStAg,o~\n" +
                "n,o,s,i,a,�,�,�,�,PoStAg,_";

        str2[2] = "";

        str2[3] = "";

        assertEquals(str1[0], str2[0]);
        assertEquals(str1[1], str2[1]);
        assertEquals(str1[2], str2[2]);
        assertEquals(str1[3], str2[3]);
    }

    /**
     * Test of getVecteur_Methode_4Classifieurs method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetVecteur_Methode_4Classifieurs_3() {
        System.out.println("AlignementGraphemesPhonemes.getVecteur_Methode_4Classifieurs_3");
        String[] str1, str2;

        str1 = agp2.getVecteur_Methode_4Classifieurs("$", "�", "PoStAg", "_");
        str2 = new String[4];

        str2[0] = "m,$,$,$,$,a,l,o,t,PoStAg,simple\n" +
                "a,m,$,$,$,l,o,t,r,PoStAg,simple\n" +
                "l,a,m,$,$,o,t,r,u,PoStAg,simple\n" +
                "o,l,a,m,$,t,r,u,�,PoStAg,simple\n" +
                "t,o,l,a,m,r,u,�,�,PoStAg,simple\n" +
                "r,t,o,l,a,u,�,�,�,PoStAg,simple\n" +
                "u,r,t,o,l,�,�,�,�,PoStAg,simple";

        str2[1] = "m,$,$,$,$,a,l,o,t,PoStAg,m\n" +
                "a,m,$,$,$,l,o,t,r,PoStAg,a\n" +
                "l,a,m,$,$,o,t,r,u,PoStAg,l\n" +
                "o,l,a,m,$,t,r,u,�,PoStAg,O\n" +
                "t,o,l,a,m,r,u,�,�,PoStAg,t\n" +
                "r,t,o,l,a,u,�,�,�,PoStAg,R\n" +
                "u,r,t,o,l,�,�,�,�,PoStAg,y";

        str2[2] = "";

        str2[3] = "";

        assertEquals(str1[0], str2[0]);
        assertEquals(str1[1], str2[1]);
        assertEquals(str1[2], str2[2]);
        assertEquals(str1[3], str2[3]);
    }

    /**
     * Test of getVecteur_Methode_4Classifieurs method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetVecteur_Methode_4Classifieurs_4() {
        System.out.println("AlignementGraphemesPhonemes.getVecteur_Methode_4Classifieurs_4");
        String[] str1, str2;

        str1 = agp3.getVecteur_Methode_4Classifieurs("$", "�", "PoStAg", "_");
        str2 = new String[4];

        str2[0] = "x,$,$,$,$,y,l,o,c,PoStAg,double\n" +
                "y,x,$,$,$,l,o,c,o,PoStAg,simple\n" +
                "l,y,x,$,$,o,c,o,p,PoStAg,simple\n" +
                "o,l,y,x,$,c,o,p,e,PoStAg,simple\n" +
                "c,o,l,y,x,o,p,e,�,PoStAg,simple\n" +
                "o,c,o,l,y,p,e,�,�,PoStAg,simple\n" +
                "p,o,c,o,l,e,�,�,�,PoStAg,simple\n" +
                "e,p,o,c,o,�,�,�,�,PoStAg,simple";

        str2[1] = "y,x,$,$,$,l,o,c,o,PoStAg,i\n" +
                "l,y,x,$,$,o,c,o,p,PoStAg,l\n" +
                "o,l,y,x,$,c,o,p,e,PoStAg,O\n" +
                "c,o,l,y,x,o,p,e,�,PoStAg,k\n" +
                "o,c,o,l,y,p,e,�,�,PoStAg,O\n" +
                "p,o,c,o,l,e,�,�,�,PoStAg,p\n" +
                "e,p,o,c,o,�,�,�,�,PoStAg,@";

        str2[2] = "x,$,$,$,$,y,l,o,c,PoStAg,k";

        str2[3] = "x,k,$,$,$,$,y,l,o,c,PoStAg,s";

        assertEquals(str1[0], str2[0]);
        assertEquals(str1[1], str2[1]);
        assertEquals(str1[2], str2[2]);
        assertEquals(str1[3], str2[3]);
    }

    /**
     * Test of toString method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testToString() {
        System.out.println("AlignementGraphemesPhonemes.toString");
        String g, p;
        g = "m\ta\ti\ts\to\tn\t";
        p = "m\tE\t" + Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE + "\tz\to~\t" + Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE + "\t";
        assertEquals(agp1.toString(), g + "\n" + p);

        g = "m\ta\tl\to\tt\tr\tu\t";
        p = "m\ta\tl\tO\tt\tR\ty\t";
        assertEquals(agp2.toString(), g + "\n" + p + "\n(Score : " + 0.6 + ")");

        g = "x\t" + Configuration.STRING_DE_REMPLACEMENT_GRAPHEME_VIDE + "\ty\tl\to\tc\to\tp\te\t";
        p = "k\ts\ti\tl\tO\tk\tO\tp\t@\t";
        assertEquals(agp3.toString(), g + "\n" + p + "\n(Score : " + 0.6 + ")");
    }

    /**
     * Test of comparerScore method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testComparerScore() {
        System.out.println("AlignementGraphemesPhonemes.comparerScore");
        assertEquals(agp1.comparerScore(agp2), -1);
        assertEquals(agp1.comparerScore(agp3), -1);
        assertEquals(agp2.comparerScore(agp1), 1);
        assertEquals(agp3.comparerScore(agp1), 1);
        assertEquals(agp2.comparerScore(agp3), 0);
        assertEquals(agp2.comparerScore(agp2), 0);
        assertEquals(agp3.comparerScore(agp2), 0);
    }

    /**
     * Test of getPhonemes method, of class AlignementGraphemesPhonemes.
     */
    @Test
    public void testGetPhonemes() {
        System.out.println("AlignementGraphemesPhonemes.getPhonemes");

        String[] t;

        t = agp1.getPhonemes();
        assertEquals(t[0], "m");
        assertEquals(t[1], "E");
        assertEquals(t[2], "z");
        assertEquals(t[3], "o~");

        t = agp2.getPhonemes();
        assertEquals(t[0], "m");
        assertEquals(t[1], "a");
        assertEquals(t[2], "l");
        assertEquals(t[3], "O");
        assertEquals(t[4], "t");
        assertEquals(t[5], "R");
        assertEquals(t[6], "y");

        t = agp3.getPhonemes();
        assertEquals(t[0], "k");
        assertEquals(t[1], "s");
        assertEquals(t[2], "i");
        assertEquals(t[3], "l");
        assertEquals(t[4], "O");
        assertEquals(t[5], "k");
        assertEquals(t[6], "O");
        assertEquals(t[7], "p");
        assertEquals(t[8], "@");
    }
}