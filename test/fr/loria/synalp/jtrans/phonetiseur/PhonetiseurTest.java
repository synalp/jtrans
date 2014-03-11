package fr.loria.synalp.jtrans.phonetiseur;

import java.io.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.loria.synalp.jtrans.phonetiseur.AlignementGraphemesPhonemes;
import fr.loria.synalp.jtrans.phonetiseur.Configuration;
import fr.loria.synalp.jtrans.phonetiseur.LexiqueGraphemesPhonemesPostag;
import fr.loria.synalp.jtrans.phonetiseur.Phonetiseur;
import fr.loria.synalp.jtrans.phonetiseur.Utils;
import static org.junit.Assert.*;

public class PhonetiseurTest {

    public PhonetiseurTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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
     * Test of AlignementGraphemesPhonemes method, of class Utils.
     */
    @Test
    public void testAlignementGraphemesPhonemes_1() {
        System.out.println("Phonetiseur.alignementGraphemesPhonemes_1");

        LexiqueGraphemesPhonemesPostag lexique = new LexiqueGraphemesPhonemesPostag();
        lexique.ajouterGrapheme("a");
        lexique.ajouterGrapheme("b");
        lexique.ajouterGrapheme("c");
        lexique.ajouterPhoneme("d");
        lexique.ajouterPhoneme("e");
        lexique.ajouterPhoneme("f");

        double[][] mProbas_1_1 = {
            {.5, .3, .19},
            {.1, .8, .09},
            {.6, .3, .09}};

        Phonetiseur p = new Phonetiseur(2, .5, .01, 12, 42, "_g", "_f");
        p.setMProbaUnGraphemeUnPhoneme(mProbas_1_1);
        p.setLexique(lexique);

        AlignementGraphemesPhonemes agp = p.alignerGraphemesPhonemes(Utils.stringToArrayString("abbc"), Utils.stringToArrayString("ed"), false, false);
        assertEquals(agp.getScore(), Math.pow(0.12, 1. / 4.), 0.0000001);
        assertEquals(agp.getTailleAlignement(), 4);
        assertEquals(agp.getGrapheme(0), "a");
        assertEquals(agp.getGrapheme(1), "b");
        assertEquals(agp.getGrapheme(2), "b");
        assertEquals(agp.getGrapheme(3), "c");
        assertEquals(agp.getPhoneme(0), "_f");
        assertEquals(agp.getPhoneme(1), "e");
        assertEquals(agp.getPhoneme(2), "_f");
        assertEquals(agp.getPhoneme(3), "d");
    }

    /**
     * Test of AlignementGraphemesPhonemes method, of class Utils.
     */
    @Test
    public void testAlignementGraphemesPhonemes_2() {
        System.out.println("Phonetiseur.alignementGraphemesPhonemes_2");

        LexiqueGraphemesPhonemesPostag lexique = new LexiqueGraphemesPhonemesPostag();
        lexique.ajouterGrapheme("a");
        lexique.ajouterGrapheme("b");
        lexique.ajouterGrapheme("c");
        lexique.ajouterPhoneme("d");
        lexique.ajouterPhoneme("e");
        lexique.ajouterPhoneme("f");

        double[][] mProbas_1_1 = {
            {.2, .2, .59},
            {.05, .9, .04},
            {.01, .9, .09}};

        Phonetiseur p = new Phonetiseur(2, .5, .01, 12, 42, "_g", "_f");
        p.setMProbaUnGraphemeUnPhoneme(mProbas_1_1);
        p.setLexique(lexique);

        AlignementGraphemesPhonemes agp = p.alignerGraphemesPhonemes(Utils.stringToArrayString("acb"), Utils.stringToArrayString("ede"), false, false);
        assertEquals(agp.getScore(), Math.pow(0.0045, 1. / 3.), 0.0000001);
        assertEquals(agp.getTailleAlignement(), 4);
        assertEquals(agp.getGrapheme(0), "a");
        assertEquals(agp.getGrapheme(1), "c");
        assertEquals(agp.getGrapheme(2), "b");
        assertEquals(agp.getGrapheme(3), "_g");
        assertEquals(agp.getPhoneme(0), "_f");
        assertEquals(agp.getPhoneme(1), "e");
        assertEquals(agp.getPhoneme(2), "d");
        assertEquals(agp.getPhoneme(3), "e");
    }

    /**
     * Test of AlignementGraphemesPhonemes method, of class Utils.
     */
    @Test
    public void testAlignementGraphemesPhonemes_3() {
        System.out.println("Phonetiseur.alignementGraphemesPhonemes_3");

        LexiqueGraphemesPhonemesPostag lexique = new LexiqueGraphemesPhonemesPostag();
        lexique.ajouterGrapheme("a");
        lexique.ajouterGrapheme("b");
        lexique.ajouterGrapheme("c");
        lexique.ajouterPhoneme("d");
        lexique.ajouterPhoneme("e");
        lexique.ajouterPhoneme("f");
        lexique.ajouterDoublePhoneme("e", "d");

        double[][] mProbas_1_1 = {
            {.2, .2, .59},
            {.05, .9, .04},
            {.01, .9, .09}};

        double[][] mProbas_2_1 = {
            {0},
            {0},
            {.9}};

        Phonetiseur p = new Phonetiseur(2, .5, .01, 12, 42, "_g", "_f");
        p.setMProbaUnGraphemeUnPhoneme(mProbas_1_1);
        p.setMProbaUnGraphemeDeuxPhonemes(mProbas_2_1);
        p.setLexique(lexique);

        AlignementGraphemesPhonemes agp = p.alignerGraphemesPhonemes(Utils.stringToArrayString("acb"), Utils.stringToArrayString("ede"), false, false);
        assertEquals(agp.getScore(), Math.pow(0.405, 1. / 3.), 0.0000001);
        assertEquals(agp.getTailleAlignement(), 4);
        assertEquals(agp.getGrapheme(0), "a");
        assertEquals(agp.getGrapheme(1), "c");
        assertEquals(agp.getGrapheme(2), "_g");
        assertEquals(agp.getGrapheme(3), "b");
        assertEquals(agp.getPhoneme(0), "_f");
        assertEquals(agp.getPhoneme(1), "e");
        assertEquals(agp.getPhoneme(2), "d");
        assertEquals(agp.getPhoneme(3), "e");
    }

    /**
     * Test of enregistrerMatriceProbaEtLexique et chargerMatriceProbaEtLexique method, of class Phonetiseur.
     */
    @Test
    public void testEnregistrerChargerMatriceProbaEtLexique() throws IOException {
        System.out.println("Phonetiseur.enregistrerMatriceProbaEtLexique");
        int[][] mScoreGP = {{1, 5, 3, 2}, {6, 12, 5, 2}, {1, 2, 6, 4}};
        int[][] mScoreG2P = {{0, 23}, {5, 3}, {91, 3}};
        double[][] mProbaGP = {{.1, .8, .05, .05}, {.3, .4, .1, .2}, {0, 0, .9, .1}};
        double[][] mProbaG2P = {{0.5, 0.36}, {.11, 0.04}, {.1, 4}};
        LexiqueGraphemesPhonemesPostag lgp = new LexiqueGraphemesPhonemesPostag();
        lgp.ajouterGrapheme("a");
        lgp.ajouterGrapheme("b");
        lgp.ajouterGrapheme("c");
        lgp.ajouterPhoneme("p1");
        lgp.ajouterPhoneme("p2");
        lgp.ajouterPhoneme("p3");
        lgp.ajouterPhoneme("p4");
        lgp.ajouterDoublePhoneme("dd1", "dd2");
        lgp.ajouterDoublePhoneme("dd3", "dd4");
        lgp.ajouterPosTag("fefz");
        lgp.ajouterPosTag("fefzz");
        lgp.ajouterPosTag("fefzzz");
        lgp.ajouterPosTag("fefzzzz");
        lgp.ajouterPosTag("fefzzzzz");

        Phonetiseur p = new Phonetiseur(34, 0, 0, 0, 0, "_g", "_f");
        p.setLexique(lgp);
        p.setMProbaUnGraphemeUnPhoneme(mProbaGP);
        p.setMProbaUnGraphemeDeuxPhonemes(mProbaG2P);
        p.setMCumulAssociationUnGraphemeUnPhoneme(mScoreGP);
        p.setMCumulAssociationUnGraphemeDeuxPhonemes(mScoreG2P);
        p.enregistrerMatriceProbaEtLexique(Configuration.REPERTOIRE_CIBLE_FICHIERS_DONNEES + "fichierTest______.txt");

        Phonetiseur p2 = new Phonetiseur(34, 0, 0, 0, 0, "_g", "_f");
        p2.setLexique(new LexiqueGraphemesPhonemesPostag());
        p2.chargerMatriceProbaEtLexique(Configuration.REPERTOIRE_CIBLE_FICHIERS_DONNEES + "fichierTest______.txt");

        assertEquals(p2.getLexique().getNbGraphemes(), 3);
        assertEquals(p2.getLexique().getGraphemeFromIndice(0), "a");
        assertEquals(p2.getLexique().getGraphemeFromIndice(1), "b");
        assertEquals(p2.getLexique().getGraphemeFromIndice(2), "c");

        assertEquals(p2.getLexique().getNbPhonemes(), 4);
        assertEquals(p2.getLexique().getPhonemeFromIndice(0), "p1");
        assertEquals(p2.getLexique().getPhonemeFromIndice(1), "p2");
        assertEquals(p2.getLexique().getPhonemeFromIndice(2), "p3");
        assertEquals(p2.getLexique().getPhonemeFromIndice(3), "p4");

        assertEquals(p2.getLexique().getNbDoublesPhonemes(), 2);
        assertEquals(p2.getLexique().getDoublePhonemeFromIndice(0)[0], "dd1");
        assertEquals(p2.getLexique().getDoublePhonemeFromIndice(0)[1], "dd2");
        assertEquals(p2.getLexique().getDoublePhonemeFromIndice(1)[0], "dd3");
        assertEquals(p2.getLexique().getDoublePhonemeFromIndice(1)[1], "dd4");

        assertEquals(p2.getLexique().getNbPosTag(), 5);
        assertEquals(p2.getLexique().getPosTagFromIndice(0), "fefz");
        assertEquals(p2.getLexique().getPosTagFromIndice(1), "fefzz");
        assertEquals(p2.getLexique().getPosTagFromIndice(2), "fefzzz");
        assertEquals(p2.getLexique().getPosTagFromIndice(3), "fefzzzz");
        assertEquals(p2.getLexique().getPosTagFromIndice(4), "fefzzzzz");

        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[0][0], 1);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[0][1], 5);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[0][2], 3);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[0][3], 2);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[1][0], 6);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[1][1], 12);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[1][2], 5);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[1][3], 2);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[2][0], 1);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[2][1], 2);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[2][2], 6);
        assertEquals(p2.getMCumulAssociationUnGraphemeUnPhoneme()[2][3], 4);

        assertEquals(p2.getMCumulAssociationUnGraphemeDeuxPhonemes()[0][0], 0);
        assertEquals(p2.getMCumulAssociationUnGraphemeDeuxPhonemes()[0][1], 23);
        assertEquals(p2.getMCumulAssociationUnGraphemeDeuxPhonemes()[1][0], 5);
        assertEquals(p2.getMCumulAssociationUnGraphemeDeuxPhonemes()[1][1], 3);
        assertEquals(p2.getMCumulAssociationUnGraphemeDeuxPhonemes()[2][0], 91);
        assertEquals(p2.getMCumulAssociationUnGraphemeDeuxPhonemes()[2][1], 3);

        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[0][0], .1f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[0][1], .8f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[0][2], .05f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[0][3], .05f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[1][0], .3f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[1][1], .4f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[1][2], .1f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[1][3], .2f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[2][0], 0f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[2][1], 0f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[2][2], .9f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeUnPhoneme()[2][3], .1f, 0.0000001f);

        assertEquals(p2.getMProbaUnGraphemeDeuxPhonemes()[0][0], .5f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeDeuxPhonemes()[0][1], .36f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeDeuxPhonemes()[1][0], .11f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeDeuxPhonemes()[1][1], .04f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeDeuxPhonemes()[2][0], .1f, 0.0000001f);
        assertEquals(p2.getMProbaUnGraphemeDeuxPhonemes()[2][1], 4f, 0.0000001f);

        File n = new File(Configuration.REPERTOIRE_CIBLE_FICHIERS_DONNEES + "fichierTest______.txt");
        n.delete();
    }

    /**
     * Test of getARFF_Entete_Methode_4Classifieurs method, of class Phonetiseur.
     */
    @Test
    public void testGetARFF_Entete_Methode_4Classifieurs() throws IOException {
        System.out.println("Phonetiseur.getARFF_Entete_Methode_4Classifieurs");
        String[] t = Phonetiseur.getARFF_Entete_Methode_4Classifieurs("g1,g2", "ph1,ph2", "p1,p2", "$", "�");
        assertEquals(t[0], "@relation SimpleOuDoublePhoneme\n" +
                "@attribute graphemeCentral {g1,g2}\n" +
                "@attribute gGauche1 {$,g1,g2}\n" +
                "@attribute gGauche2 {$,g1,g2}\n" +
                "@attribute gGauche3 {$,g1,g2}\n" +
                "@attribute gGauche4 {$,g1,g2}\n" +
                "@attribute gDroite1 {�,g1,g2}\n" +
                "@attribute gDroite2 {�,g1,g2}\n" +
                "@attribute gDroite3 {�,g1,g2}\n" +
                "@attribute gDroite4 {�,g1,g2}\n" +
                "@attribute posTag {p1,p2}\n" +
                "@attribute typePhoneme {simple,double}\n" +
                "\n@data\n");
        assertEquals(t[1], "@relation SimplesPhonemes\n" +
                "@attribute graphemeCentral {g1,g2}\n" +
                "@attribute gGauche1 {$,g1,g2}\n" +
                "@attribute gGauche2 {$,g1,g2}\n" +
                "@attribute gGauche3 {$,g1,g2}\n" +
                "@attribute gGauche4 {$,g1,g2}\n" +
                "@attribute gDroite1 {�,g1,g2}\n" +
                "@attribute gDroite2 {�,g1,g2}\n" +
                "@attribute gDroite3 {�,g1,g2}\n" +
                "@attribute gDroite4 {�,g1,g2}\n" +
                "@attribute posTag {p1,p2}\n" +
                "@attribute phoneme {ph1,ph2}\n" +
                "\n@data\n");
        assertEquals(t[2], "@relation 1erDoublesPhonemes\n" +
                "@attribute graphemeCentral {g1,g2}\n" +
                "@attribute gGauche1 {$,g1,g2}\n" +
                "@attribute gGauche2 {$,g1,g2}\n" +
                "@attribute gGauche3 {$,g1,g2}\n" +
                "@attribute gGauche4 {$,g1,g2}\n" +
                "@attribute gDroite1 {�,g1,g2}\n" +
                "@attribute gDroite2 {�,g1,g2}\n" +
                "@attribute gDroite3 {�,g1,g2}\n" +
                "@attribute gDroite4 {�,g1,g2}\n" +
                "@attribute posTag {p1,p2}\n" +
                "@attribute phoneme {ph1,ph2}\n" +
                "\n@data\n");
        assertEquals(t[3], "@relation 2emeDoublesPhonemes\n" +
                "@attribute graphemeCentral {g1,g2}\n" +
                "@attribute phonemePrecedent {ph1,ph2}\n" +
                "@attribute gGauche1 {$,g1,g2}\n" +
                "@attribute gGauche2 {$,g1,g2}\n" +
                "@attribute gGauche3 {$,g1,g2}\n" +
                "@attribute gGauche4 {$,g1,g2}\n" +
                "@attribute gDroite1 {�,g1,g2}\n" +
                "@attribute gDroite2 {�,g1,g2}\n" +
                "@attribute gDroite3 {�,g1,g2}\n" +
                "@attribute gDroite4 {�,g1,g2}\n" +
                "@attribute posTag {p1,p2}\n" +
                "@attribute phoneme {ph1,ph2}\n" +
                "\n@data\n");
    }
}