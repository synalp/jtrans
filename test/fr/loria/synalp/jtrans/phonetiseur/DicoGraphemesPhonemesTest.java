package fr.loria.synalp.jtrans.phonetiseur;

//import corpus.Morphalou;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.loria.synalp.jtrans.phonetiseur.Configuration;
import fr.loria.synalp.jtrans.phonetiseur.DicoGraphemesPhonemes;
import fr.loria.synalp.jtrans.phonetiseur.LexiqueGraphemesPhonemesPostag;

public class DicoGraphemesPhonemesTest {

    private static String cheminsMotsDico = Configuration.REPERTOIRE_FICHIERS_POUR_LES_TESTS_PHONETISEUR + "motsDico.txt";
    private static DicoGraphemesPhonemes dgp;
    private static LexiqueGraphemesPhonemesPostag lexique = new LexiqueGraphemesPhonemesPostag();

    public DicoGraphemesPhonemesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        //dgp = new DicoGraphemesPhonemes(new ChargeurMorphalou("test/plugins/phonetiseur/dicoTest.txt", ""), cheminsMotsDico, lexique);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getDico method, of class DicoGraphemesPhonemes.
     */
    @Test
    public void testGetDico() {
        System.out.println("=======> Faire tests de la classe DicoGraphemesPhonemes <=======");
    /*
    System.out.println("DicoGraphemesPhonemes.getDico");
    Hashtable<String, LinkedList<SuitePhonemes>> dico = dgp.getDico();

    LinkedList<SuitePhonemes> ll;
    SuitePhonemes sp;

    assertEquals(dico.size(), 3);

    // Cas d'une mauvaise clef
    ll = dico.get("zzzzzzzzzzzzzz");
    assertNull(ll);

    ll = dico.get("etc");
    assertNull(ll);

    ll = dico.get("a b c");
    assertNull(ll);

    ll = dico.get("mezzo-voce");
    assertEquals(ll.size(), 1);
    sp = ll.get(0);
    assertEquals(sp.getNbPhonemes(), 10);
    assertEquals(sp.getPhoneme(0), "m");
    assertEquals(sp.getPhoneme(1), "e");
    assertEquals(sp.getPhoneme(2), "d");
    assertEquals(sp.getPhoneme(3), "z");
    assertEquals(sp.getPhoneme(4), "oh");
    assertEquals(sp.getPhoneme(5), "v");
    assertEquals(sp.getPhoneme(6), "O");
    assertEquals(sp.getPhoneme(7), "t");
    assertEquals(sp.getPhoneme(8), "s");
    assertEquals(sp.getPhoneme(9), "swa");
    assertEquals(sp.getPosTag(), "unk");

    ll = dico.get("mezzotinto");
    assertEquals(ll.size(), 1);
    sp = ll.get(0);
    assertEquals(sp.getNbPhonemes(), 9);
    assertEquals(sp.getPhoneme(0), "m");
    assertEquals(sp.getPhoneme(1), "e");
    assertEquals(sp.getPhoneme(2), "z");
    assertEquals(sp.getPhoneme(3), "O");
    assertEquals(sp.getPhoneme(4), "t");
    assertEquals(sp.getPhoneme(5), "i");
    assertEquals(sp.getPhoneme(6), "n");
    assertEquals(sp.getPhoneme(7), "t");
    assertEquals(sp.getPhoneme(8), "oh");
    assertEquals(sp.getPosTag(), "unk");

    ll = dico.get("mi");
    for (int i = 0; i < ll.size(); i++) {
    //System.out.println(ll.get(i).toString() + "\n\n");
    }

    assertEquals(lexique.getNbGraphemes(), 10);
    assertEquals(lexique.getNbPhonemes(), 12);
    assertEquals(lexique.getNbDoublesPhonemes(), 0);
    assertEquals(lexique.getNbDoublesPhonemesTemp(), 0);
    assertEquals(lexique.getNbPosTag(), 1);

    assertEquals(lexique.getGraphemeFromIndice(0), "m");
    assertEquals(lexique.getGraphemeFromIndice(1), "e");
    assertEquals(lexique.getGraphemeFromIndice(2), "z");
    assertEquals(lexique.getGraphemeFromIndice(3), "o");
    assertEquals(lexique.getGraphemeFromIndice(4), "-");
    assertEquals(lexique.getGraphemeFromIndice(5), "v");
    assertEquals(lexique.getGraphemeFromIndice(6), "c");
    assertEquals(lexique.getGraphemeFromIndice(7), "t");
    assertEquals(lexique.getGraphemeFromIndice(8), "i");
    assertEquals(lexique.getGraphemeFromIndice(9), "n");

    assertEquals(lexique.getPhonemeFromIndice(0), "m");
    assertEquals(lexique.getPhonemeFromIndice(1), "e");
    assertEquals(lexique.getPhonemeFromIndice(2), "d");
    assertEquals(lexique.getPhonemeFromIndice(3), "z");
    assertEquals(lexique.getPhonemeFromIndice(4), "oh");
    assertEquals(lexique.getPhonemeFromIndice(5), "v");
    assertEquals(lexique.getPhonemeFromIndice(6), "O");
    assertEquals(lexique.getPhonemeFromIndice(7), "t");
    assertEquals(lexique.getPhonemeFromIndice(8), "s");
    assertEquals(lexique.getPhonemeFromIndice(9), "swa");
    assertEquals(lexique.getPhonemeFromIndice(10), "i");
    assertEquals(lexique.getPhonemeFromIndice(11), "n");

    assertEquals(lexique.getPosTagFromIndice(0), "unk");
     * */
    }
}