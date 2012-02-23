package test.plugins.phonetiseur;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import plugins.phonetiseur.LexiqueGraphemesPhonemesPostag;
import static org.junit.Assert.*;

public class LexiqueGraphemesPhonemesPostagTest {

    private LexiqueGraphemesPhonemesPostag l;

    public LexiqueGraphemesPhonemesPostagTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        l = new LexiqueGraphemesPhonemesPostag();
        l.ajouterGrapheme("a");
        l.ajouterGrapheme("b");
        l.ajouterGrapheme("c");
        l.ajouterGrapheme("b");
        l.ajouterPhoneme("x");
        l.ajouterPhoneme("x");
        l.ajouterPhoneme("xx");
        l.ajouterDoublePhoneme("aa", "bb");
        l.ajouterDoublePhoneme("cc", "dd");
        l.ajouterDoublePhoneme("bb", "ee");
        l.ajouterDoublePhoneme("aa", "ee");
        l.ajouterDoublePhonemeTemp("cc", "dd");
        l.ajouterDoublePhonemeTemp("cc", "dd");
        l.ajouterDoublePhonemeTemp("cc", "dd");
        l.ajouterDoublePhonemeTemp("ee", "cc");
        l.ajouterPosTag("posTag1");
        l.ajouterPosTag("posTag2");
        l.ajouterPosTag("posTag3");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getIndiceFromGrapheme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetIndiceFromGrapheme() {
        System.out.println("LexiqueGraphemesPhonemes.getIndiceFromGrapheme");
        assertEquals(l.getIndiceFromGrapheme("a"), 0);
        assertEquals(l.getIndiceFromGrapheme("b"), 1);
        assertEquals(l.getIndiceFromGrapheme("c"), 2);
        assertEquals(l.getIndiceFromGrapheme("afsd"), -1);
    }

    /**
     * Test of getIndiceFromPhoneme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetIndiceFromPhoneme() {
        System.out.println("LexiqueGraphemesPhonemes.getIndiceFromPhoneme");
        assertEquals(l.getIndiceFromPhoneme("x"), 0);
        assertEquals(l.getIndiceFromPhoneme("xx"), 1);
        assertEquals(l.getIndiceFromPhoneme("xxx"), -1);
    }

    /**
     * Test of getIndiceFromDoublePhoneme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetIndiceFromDoublePhoneme() {
        System.out.println("LexiqueGraphemesPhonemes.getIndiceFromDoublePhoneme");
        assertEquals(l.getIndiceFromDoublePhoneme("aa", "bb"), 0);
        assertEquals(l.getIndiceFromDoublePhoneme("bb", "aa"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("cc", "dd"), 1);
        assertEquals(l.getIndiceFromDoublePhoneme("dd", "cc"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("bb", "ee"), 2);
        assertEquals(l.getIndiceFromDoublePhoneme("ee", "bb"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("aa", "ee"), 3);
        assertEquals(l.getIndiceFromDoublePhoneme("ee", "aa"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("bb", "cc"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("aa", "cc"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("cc", "aa"), -1);
        assertEquals(l.getIndiceFromDoublePhoneme("dd", "bb"), -1);
    }

    /**
     * Test of getIndiceFromDoublePhonemeTemp method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetIndiceFromDoublePhonemeTemp() {
        System.out.println("LexiqueGraphemesPhonemes.getIndiceFromDoublePhonemeTemp");
        assertEquals(l.getIndiceFromDoublePhonemeTemp("cc", "dd"), 0);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("dd", "cc"), -1);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("ee", "cc"), 1);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("cc", "ee"), -1);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("bb", "cc"), -1);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("aa", "cc"), -1);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("cc", "aa"), -1);
        assertEquals(l.getIndiceFromDoublePhonemeTemp("dd", "bb"), -1);
    }

    /**
     * Test of getIndiceFromPosTag method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetIndiceFromPosTag() {
        System.out.println("LexiqueGraphemesPhonemes.getIndiceFromPosTag");
        assertEquals(l.getIndiceFromPosTag("posTag1"), 0);
        assertEquals(l.getIndiceFromPosTag("posTag2"), 1);
        assertEquals(l.getIndiceFromPosTag("posTag3"), 2);
        assertEquals(l.getIndiceFromPosTag("posTag4"), -1);
    }

    /**
     * Test of getGraphemeFromIndice method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetGraphemeFromIndice() {
        System.out.println("LexiqueGraphemesPhonemes.getGraphemeFromIndice");
        assertEquals(l.getGraphemeFromIndice(0), "a");
        assertEquals(l.getGraphemeFromIndice(1), "b");
        assertEquals(l.getGraphemeFromIndice(2), "c");
    }

    /**
     * Test of getPhonemeFromIndice method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetPhonemeFromIndice() {
        System.out.println("LexiqueGraphemesPhonemes.getPhonemeFromIndice");
        assertEquals(l.getPhonemeFromIndice(0), "x");
        assertEquals(l.getPhonemeFromIndice(1), "xx");
    }

    /**
     * Test of getDoublePhonemeFromIndice method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetDoublePhonemeFromIndice() {
        System.out.println("LexiqueGraphemesPhonemes.getDoublePhonemeFromIndice");
        assertEquals(l.getDoublePhonemeFromIndice(0)[0], "aa");
        assertEquals(l.getDoublePhonemeFromIndice(0)[1], "bb");
        assertEquals(l.getDoublePhonemeFromIndice(1)[0], "cc");
        assertEquals(l.getDoublePhonemeFromIndice(1)[1], "dd");
        assertEquals(l.getDoublePhonemeFromIndice(2)[0], "bb");
        assertEquals(l.getDoublePhonemeFromIndice(2)[1], "ee");
        assertEquals(l.getDoublePhonemeFromIndice(3)[0], "aa");
        assertEquals(l.getDoublePhonemeFromIndice(3)[1], "ee");
    }

    /**
     * Test of getPosTagFromIndice method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetPosTagFromIndice() {
        System.out.println("LexiqueGraphemesPhonemes.getPosTagFromIndice");
        assertEquals(l.getPosTagFromIndice(0), "posTag1");
        assertEquals(l.getPosTagFromIndice(1), "posTag2");
        assertEquals(l.getPosTagFromIndice(2), "posTag3");
    }

    /**
     * Test of getNbGraphemes method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetNbGraphemes() {
        System.out.println("LexiqueGraphemesPhonemes.getNbGraphemes");
        assertEquals(l.getNbGraphemes(), 3);
    }

    /**
     * Test of getNbPhonemes method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetNbPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getNbPhonemes");
        assertEquals(l.getNbPhonemes(), 2);
    }

    /**
     * Test of getNbDoublesPhonemes method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetNbDoublesPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getNbDoublesPhonemes");
        assertEquals(l.getNbDoublesPhonemes(), 4);
    }

    /**
     * Test of getNbPosTag method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testGetNbPosTag() {
        System.out.println("LexiqueGraphemesPhonemes.getNbPosTag");
        assertEquals(l.getNbPosTag(), 3);
    }

    /**
     * Test of contientGrapheme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testContientGrapheme() {
        System.out.println("LexiqueGraphemesPhonemes.contientGrapheme");
        assertTrue(l.contientGrapheme("a"));
        assertTrue(l.contientGrapheme("b"));
        assertTrue(l.contientGrapheme("c"));
        assertFalse(l.contientGrapheme("x"));
    }

    /**
     * Test of contientPhoneme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testContientPhoneme() {
        System.out.println("LexiqueGraphemesPhonemes.contientPhoneme");
        assertTrue(l.contientPhoneme("x"));
        assertTrue(l.contientPhoneme("xx"));
        assertFalse(l.contientPhoneme("a"));
    }

    /**
     * Test of contientDoublePhoneme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testContientDoublePhoneme() {
        System.out.println("LexiqueGraphemesPhonemes.contientDoublePhoneme");
        assertTrue(l.contientDoublePhoneme("aa", "bb"));
        assertFalse(l.contientDoublePhoneme("bb", "aa"));
        assertTrue(l.contientDoublePhoneme("cc", "dd"));
        assertFalse(l.contientDoublePhoneme("dd", "cc"));
        assertTrue(l.contientDoublePhoneme("bb", "ee"));
        assertFalse(l.contientDoublePhoneme("ee", "bb"));
        assertTrue(l.contientDoublePhoneme("aa", "ee"));
        assertFalse(l.contientDoublePhoneme("ee", "aa"));
        assertFalse(l.contientDoublePhoneme("bb", "cc"));
        assertFalse(l.contientDoublePhoneme("dd", "bb"));
        assertFalse(l.contientDoublePhoneme("ee", "cc"));
    }

    /**
     * Test of contientDoublePhonemeTemp method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testContientDoublePhonemeTemp() {
        System.out.println("LexiqueGraphemesPhonemes.contientDoublePhonemeTemp");
        assertFalse(l.contientDoublePhonemeTemp("cc", "ee"));
        assertTrue(l.contientDoublePhonemeTemp("ee", "cc"));
        assertTrue(l.contientDoublePhonemeTemp("cc", "dd"));
        assertFalse(l.contientDoublePhonemeTemp("dd", "cc"));
        assertFalse(l.contientDoublePhonemeTemp("bb", "cc"));
        assertFalse(l.contientDoublePhonemeTemp("dd", "bb"));
        assertFalse(l.contientDoublePhonemeTemp("ee", "cccs"));
    }

    /**
     * Test of contientPosTag method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testContientPosTag() {
        System.out.println("LexiqueGraphemesPhonemes.contientPosTag");
        assertTrue(l.contientPosTag("posTag1"));
        assertTrue(l.contientPosTag("posTag2"));
        assertTrue(l.contientPosTag("posTag3"));
        assertFalse(l.contientPosTag("posTag4"));
    }

    /**
     * Test of ajouterPhoneme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testAjouterPhoneme() {
        System.out.println("LexiqueGraphemesPhonemes.ajouterPhoneme");
        assertTrue(l.ajouterPhoneme("zzzzzzzzz"));
        assertFalse(l.ajouterPhoneme("zzzzzzzzz"));
        assertEquals(l.getNbPhonemes(), 3);
        assertTrue(l.contientPhoneme("zzzzzzzzz"));
    }

    /**
     * Test of ajouterDoublePhoneme method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testAjouterDoublePhoneme() {
        System.out.println("LexiqueGraphemesPhonemes.ajouterDoublePhoneme");
        assertTrue(l.ajouterDoublePhoneme("zz", "zzz"));
        assertFalse(l.ajouterDoublePhoneme("zz", "zzz"));
        assertEquals(l.getNbDoublesPhonemes(), 5);
        assertTrue(l.contientDoublePhoneme("zz", "zzz"));
    }

    /**
     * Test of ajouterDoublePhonemeTemp method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testAjouterDoublePhonemeTemp() {
        System.out.println("LexiqueGraphemesPhonemes.ajouterDoublePhonemeTemp");
        assertTrue(l.ajouterDoublePhonemeTemp("zz", "zzz"));
        assertFalse(l.ajouterDoublePhonemeTemp("zz", "zzz"));
        assertTrue(l.contientDoublePhonemeTemp("zz", "zzz"));
    }

    /**
     * Test of ajouterPosTag method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testAjouterPosTag() {
        System.out.println("LexiqueGraphemesPhonemes.ajouterPosTag");
        assertTrue(l.ajouterPosTag("ppppp"));
        assertFalse(l.ajouterPosTag("ppppp"));
        assertEquals(l.getNbPosTag(), 4);
        assertTrue(l.contientPosTag("ppppp"));
    }

    /**
     * Test of toStringPourEnregistrer method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testToStringPourEnregistrer() {
        System.out.println("LexiqueGraphemesPhonemes.toStringPourEnregistrer");
        assertEquals(l.toStringPourEnregistrer(), "a\tb\tc\nx\txx\naa\tbb\tcc\tdd\tbb\tee\taa\tee\nposTag1\tposTag2\tposTag3");
    }

    /**
     * Test of raz method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testRaz() {
        System.out.println("LexiqueGraphemesPhonemes.raz");
        l.raz();
        assertEquals(l.getNbDoublesPhonemes(), 0);
        assertEquals(l.getNbGraphemes(), 0);
        assertEquals(l.getNbPhonemes(), 0);
        assertEquals(l.getNbDoublesPhonemesTemp(), 0);
        assertEquals(l.getNbPosTag(), 0);
    }

    /**
     * Test of commitDoublesPhonemesTemp method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testCommitDoublesPhonemesTemp() {
        System.out.println("LexiqueGraphemesPhonemes.commitDoublesPhonemesTemp");
        l.commitDoublesPhonemesTemp();

        assertTrue(l.contientGrapheme("a"));
        assertTrue(l.contientGrapheme("b"));
        assertTrue(l.contientGrapheme("c"));
        assertFalse(l.contientGrapheme("ee"));
        assertTrue(l.contientPhoneme("x"));
        assertTrue(l.contientPhoneme("xx"));
        assertFalse(l.contientPhoneme("ee"));
        assertTrue(l.contientDoublePhoneme("cc", "dd"));
        assertFalse(l.contientDoublePhoneme("dd", "cc"));
        assertTrue(l.contientDoublePhoneme("ee", "cc"));
        assertFalse(l.contientDoublePhoneme("cc", "ee"));
        assertFalse(l.contientDoublePhoneme("aa", "bb"));
        assertFalse(l.contientDoublePhoneme("bb", "aa"));
        assertFalse(l.contientDoublePhoneme("bb", "ee"));
        assertFalse(l.contientDoublePhoneme("ee", "bb"));
        assertFalse(l.contientDoublePhonemeTemp("cc", "dd"));
        assertFalse(l.contientDoublePhonemeTemp("ee", "cc"));
    }

    /**
     * Test of pasDeDifferenceDansLesDoublesPhonemes method, of class LexiqueGraphemesPhonemes.
     */
    @Test
    public void testPasDeDifferenceDansLesDoublesPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.pasDeDifferenceDansLesDoublesPhonemes");
        LexiqueGraphemesPhonemesPostag lgp = new LexiqueGraphemesPhonemesPostag();
        lgp.ajouterDoublePhoneme("cb", "p");
        lgp.ajouterDoublePhoneme("czzb", "sd");
        lgp.ajouterDoublePhoneme("czzaaab", "sd");
        lgp.ajouterDoublePhonemeTemp("czzaaab", "sd");
        lgp.ajouterDoublePhonemeTemp("czzb", "sd");
        lgp.ajouterDoublePhonemeTemp("cb", "p");
        assertTrue(lgp.pasDeDifferenceDansLesDoublesPhonemes());

        lgp = new LexiqueGraphemesPhonemesPostag();
        lgp.ajouterDoublePhoneme("cb", "p");
        lgp.ajouterDoublePhoneme("czzb", "sd");
        lgp.ajouterDoublePhoneme("czzaaab", "sd");
        lgp.ajouterDoublePhoneme("a", "a");
        lgp.ajouterDoublePhonemeTemp("czzaaab", "sd");
        lgp.ajouterDoublePhonemeTemp("czzb", "sd");
        lgp.ajouterDoublePhonemeTemp("cb", "p");
        assertFalse(lgp.pasDeDifferenceDansLesDoublesPhonemes());

        lgp = new LexiqueGraphemesPhonemesPostag();
        lgp.ajouterDoublePhoneme("cb", "p");
        lgp.ajouterDoublePhoneme("czzb", "sd");
        lgp.ajouterDoublePhoneme("czzaaab", "sd");
        lgp.ajouterDoublePhonemeTemp("czzaaab", "sd");
        lgp.ajouterDoublePhonemeTemp("czzb", "sd");
        lgp.ajouterDoublePhonemeTemp("cb", "p");
        lgp.ajouterDoublePhonemeTemp("s", "s");
        assertFalse(lgp.pasDeDifferenceDansLesDoublesPhonemes());

        lgp = new LexiqueGraphemesPhonemesPostag();
        lgp.ajouterDoublePhoneme("cb", "p");
        lgp.ajouterDoublePhoneme("czzb", "sd");
        lgp.ajouterDoublePhoneme("sd", "czzb");
        lgp.ajouterDoublePhonemeTemp("czzaaab", "sd");
        lgp.ajouterDoublePhonemeTemp("czzb", "sd");
        lgp.ajouterDoublePhonemeTemp("cb", "p");
        assertFalse(lgp.pasDeDifferenceDansLesDoublesPhonemes());
    }

    /**
     * Test of raz method, of class getArrayGraphemes.
     */
    @Test
    public void testGetArrayGraphemes() {
        System.out.println("LexiqueGraphemesPhonemes.getArrayGraphemes");
        String[] str = l.getArrayGraphemes();
        assertEquals(str[0], "a");
        assertEquals(str[1], "b");
        assertEquals(str[2], "c");
    }

    /**
     * Test of raz method, of class getArrayPhonemes.
     */
    @Test
    public void testGetArrayPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getArrayPhonemes");
        String[] str = l.getArrayPhonemes();
        assertEquals(str[0], "x");
        assertEquals(str[1], "xx");
    }

    /**
     * Test of raz method, of class getArrayDoublesPhonemes.
     */
    @Test
    public void testGetArrayDoublesPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getArrayDoublesPhonemes");
        String[][] str = l.getArrayDoublesPhonemes();
        assertEquals(str[0][0], "aa");
        assertEquals(str[0][1], "bb");
        assertEquals(str[1][0], "cc");
        assertEquals(str[1][1], "dd");
        assertEquals(str[2][0], "bb");
        assertEquals(str[2][1], "ee");
    }

    /**
     * Test of raz method, of class getArrayPosTag.
     */
    @Test
    public void testGetArrayPosTag() {
        System.out.println("LexiqueGraphemesPhonemes.getArrayPosTag");
        String[] str = l.getArrayPosTag();
        assertEquals(str[0], "posTag1");
        assertEquals(str[1], "posTag2");
        assertEquals(str[2], "posTag3");

    }

    /**
     * Test of raz method, of class getStringGraphemes.
     */
    @Test
    public void testGetStringGraphemes() {
        System.out.println("LexiqueGraphemesPhonemes.getStringGraphemes");
        String str = l.getStringGraphemes(",");
        assertEquals(str, "a,b,c");
    }

    /**
     * Test of raz method, of class getStringPhonemesSimples.
     */
    @Test
    public void testGetStringPhonemesSimples() {
        System.out.println("LexiqueGraphemesPhonemes.getStringPhonemesSimples");
        String str = l.getStringPhonemesSimples(",");
        assertEquals(str, "x,xx");
    }

    /**
     * Test of raz method, of class getStringDoublesPhonemes.
     */
    @Test
    public void testGetStringDoublesPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getStringDoublesPhonemes");
        String str = l.getStringDoublesPhonemes(",", " ");
        assertEquals(str, "aa bb,cc dd,bb ee,aa ee");
    }

    /**
     * Test of raz method, of class getStringPosTag.
     */
    @Test
    public void testGetStringPosTag() {
        System.out.println("LexiqueGraphemesPhonemes.getStringPosTag");
        String str = l.getStringPosTag(",");
        assertEquals(str, "posTag1,posTag2,posTag3");
    }

    /**
     * Test of raz method, of class getString1erDoublesPhonemes.
     */
    @Test
    public void testGetString1erDoublesPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getString1erDoublesPhonemes");
        String str = l.getString1erDoublesPhonemes(",");
        assertEquals(str, "aa,cc,bb");
    }

    /**
     * Test of raz method, of class getString2emeDoublesPhonemes.
     */
    @Test
    public void testGetString2emeDoublesPhonemes() {
        System.out.println("LexiqueGraphemesPhonemes.getString2emeDoublesPhonemes");
        String str = l.getString2emeDoublesPhonemes(",");
        assertEquals(str, "bb,dd,ee");
    }
}