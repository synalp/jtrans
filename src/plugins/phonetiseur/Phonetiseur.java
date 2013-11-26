/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.*;
import utils.FileUtils;
import utils.PronunciationsLexicon;

/**
 * Le phonetiseur
 */
public class Phonetiseur {

    private int[][] mCumulAssociationUnGraphemeUnPhoneme;
    private int[][] mCumulAssociationUnGraphemeDeuxPhonemes;
    private double[][] mProbaUnGraphemeUnPhoneme;
    private double[][] mProbaUnGraphemeDeuxPhonemes;
    private DicoGraphemesPhonemes dico;
    private LexiqueGraphemesPhonemesPostag lexique;
    private Classifieurs classifieurs;
    /* "constantes" */
    private int nbDecalagesHeuristiqueDaelemans;
    private double probaPhonemeVideInit;
    private double probaCreerDoublePhoneme;
    private double epsilonConvergenceMatriceProbas;
    private int nbIterationsMaxCalculMatriceProbas;
    private String stringGraphemeVide;
    private String stringPhonemeVide;

    public Phonetiseur(PronunciationsLexicon pronuncitationsLexicon, String cheminFichierMotsAApprendre) {
        this();
        this.lexique = new LexiqueGraphemesPhonemesPostag();
        this.dico = new DicoGraphemesPhonemes(pronuncitationsLexicon, cheminFichierMotsAApprendre, lexique);
    }

    public Phonetiseur() {
        this.nbDecalagesHeuristiqueDaelemans = Configuration.NB_DECALAGES_HEURISTIQUE_DAELEMANS;
        this.probaPhonemeVideInit = Configuration.PROBA_PHONEME_VIDE_INIT;
        this.probaCreerDoublePhoneme = Configuration.PROBA_CREER_DOUBLE_PHONEME;
        this.epsilonConvergenceMatriceProbas = Configuration.EPSILON_CONVERGENCE_MATRICES_PROBAS;
        this.nbIterationsMaxCalculMatriceProbas = Configuration.NB_ITERATIONS_MAX_CALCUL_MATRICES_PROBAS;
        this.stringGraphemeVide = Configuration.STRING_DE_REMPLACEMENT_GRAPHEME_VIDE;
        this.stringPhonemeVide = Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE;
    }

    /**
     * Constructeur pour les tests
     */
    public Phonetiseur(PronunciationsLexicon pronuncitationsLexicon, String cheminFichierMotsAApprendre, int nbDecalagesHeuristiqueDaelemans, double probaPhonemeVideInit, double probaCreerDoublePhoneme, double epsilonConvergenceMatriceProbas, int nbIterationsMaxCalculMatriceProbas, String stringGraphemeVide, String stringPhonemeVide) {
        this(nbDecalagesHeuristiqueDaelemans, probaPhonemeVideInit, probaCreerDoublePhoneme, epsilonConvergenceMatriceProbas, nbIterationsMaxCalculMatriceProbas, stringGraphemeVide, stringPhonemeVide);
        this.lexique = new LexiqueGraphemesPhonemesPostag();
        this.dico = new DicoGraphemesPhonemes(pronuncitationsLexicon, cheminFichierMotsAApprendre, lexique);
    }

    public Phonetiseur(int nbDecalagesHeuristiqueDaelemans, double probaPhonemeVideInit, double probaCreerDoublePhoneme, double epsilonConvergenceMatriceProbas, int nbIterationsMaxCalculMatriceProbas, String stringGraphemeVide, String stringPhonemeVide) {
        this.nbDecalagesHeuristiqueDaelemans = nbDecalagesHeuristiqueDaelemans;
        this.probaPhonemeVideInit = probaPhonemeVideInit;
        this.probaCreerDoublePhoneme = probaCreerDoublePhoneme;
        this.epsilonConvergenceMatriceProbas = epsilonConvergenceMatriceProbas;
        this.nbIterationsMaxCalculMatriceProbas = nbIterationsMaxCalculMatriceProbas;
        this.stringGraphemeVide = stringGraphemeVide;
        this.stringPhonemeVide = stringPhonemeVide;
    }

    /*************************************************************************/
    /********************************* Algos *********************************/
    /*************************************************************************/
    /**
     * Calcul les probabilites d'association entre les graphemes et les phonemes
     * Note : le lexique (association grapheme <-> indice) est aussi sauvegarde
     */
    public void genererMatricesProbabilites() {
        if (dico == null) {
            System.out.println("Erreur : creation des probabilites impossible, " +
                    "il faut un dictionnaire.");
            return;
        }

        /**** Si le lexique ne contient pas le phoneme vide => on le rajoute ****/
        if (!lexique.contientPhoneme(stringPhonemeVide)) {
            lexique.ajouterPhoneme(stringPhonemeVide);
        }

        /**** Heuristique de Daelemans => on obtient une matrice de proba 1_Grapheme/1_Phonemes ****/
        System.out.print("Heuristique de Daelemans...");
        heuristiqueDaelemans();
        mProbaUnGraphemeUnPhoneme = Utils.convertirMatricesCumulsEnMatricesProbas(mCumulAssociationUnGraphemeUnPhoneme, mCumulAssociationUnGraphemeDeuxPhonemes, Configuration.PROBA_CREER_DOUBLE_PHONEME).getFirst();
        System.out.println(" Ok");

        /**** Calcul des matrices de probas => iterations jusqu'a convergence ****/
        System.out.println("Calcul de la matrice de probabilites...");
        faireConvergerMatricesProbabilites();
        System.out.println(" Ok");
    }

    /**
     * Calcul la matrice de cumul calculee sur le dictionnaire en utilisant l'heuristique de Daelemans
     */
    public void heuristiqueDaelemans() {
        mCumulAssociationUnGraphemeUnPhoneme = new int[lexique.getNbGraphemes()][lexique.getNbPhonemes()];

        for (Entry<String, LinkedList<SuitePhonemes>> entry : dico.getDico().entrySet()) {
            String[] tMot = Utils.stringToArrayString(entry.getKey());
            String[] tPhonemes = null;
            // Pour chaque phonetisation possible
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                tPhonemes = suitePhonemes.getTPhonemes();
                int nbDecalagesPossibles = Math.min(nbDecalagesHeuristiqueDaelemans, Math.max(tMot.length - tPhonemes.length, 0));
                for (int decalage = 0; decalage <= nbDecalagesPossibles; decalage++) {
                    int indiceMax = Math.min(tMot.length - decalage, tPhonemes.length);
                    for (int i = 0; i < indiceMax; i++) {
                        // On ajoute 2^decalage a l'association
                        // (tMot[i + 1 + decalage], tPhonemes[i])
                        int l = lexique.getIndiceFromGrapheme(tMot[i + decalage]);
                        int c = lexique.getIndiceFromPhoneme(tPhonemes[i]);
                        mCumulAssociationUnGraphemeUnPhoneme[l][c] += Math.pow(2, nbDecalagesHeuristiqueDaelemans - decalage);
                    }
                }
            }
        }
    }

    /**
     * Aligne les graphemes et phonemes passes en parametre.
     * @param utiliserVraieProbaAlignerAvecPhonemeVide mettre true pour un alignement "normal" et false si on souhaite utilise la proba initiale d'aligner avec le phoneme vide
     * @param afficherMatricesProgDynamique true si on souhaite afficher les matrices de programmation dynamique (pour debugage)
     */
    public AlignementGraphemesPhonemes alignerGraphemesPhonemes(String[] tGraphemes, String[] tPhonemes, boolean utiliserVraieProbaAlignerAvecPhonemeVide, boolean afficherMatricesProgDynamique) {
        int indicePhonemeVide = lexique.getIndiceFromPhoneme(stringPhonemeVide);

        // On instancie les 2 matrices pour l'algo de Viterbi
        double[][] mProbaDyn = new double[tPhonemes.length + 1][tGraphemes.length + 1];
        int[][] mDirectionDyn = new int[tPhonemes.length + 1][tGraphemes.length + 1];
        mProbaDyn[0][0] = 1;

        // On remplit la matrice de "programmation dynamique"
        for (int l = 0; l < mProbaDyn.length; l++) {
            for (int c = 1; c < mProbaDyn[0].length; c++) {
                double newVal = 0;
                int newDir = -1;

                int indiceGrapheme = lexique.getIndiceFromGrapheme(tGraphemes[c - 1]);

                // Cas 1 : 1 phoneme pour 1 grapheme
                if ((l >= 1) && (mProbaDyn[l - 1][c - 1] != 0f)) {
                    int indicePhoneme = lexique.getIndiceFromPhoneme(tPhonemes[l - 1]);
                    newVal = mProbaDyn[l - 1][c - 1] * mProbaUnGraphemeUnPhoneme[indiceGrapheme][indicePhoneme];
                    newDir = 1;
                }

                // Cas 2 : pas d'alignement (alignement avec le phoneme vide)
                if ((l == 0) || (mProbaDyn[l][c - 1] != 0f)) {
                    double proba = (utiliserVraieProbaAlignerAvecPhonemeVide ? mProbaUnGraphemeUnPhoneme[indiceGrapheme][indicePhonemeVide] : probaPhonemeVideInit);
                    double temp = mProbaDyn[l][c - 1] * proba;
                    boolean egaux = (newVal == 0) ? false
                            : ((temp / newVal) < (1 + Configuration.EPSILON_ALIGNEMENT_GRAPHEME_PHONEME)) &&
                            ((temp / newVal) > (1 - Configuration.EPSILON_ALIGNEMENT_GRAPHEME_PHONEME));
                    if (egaux || (temp >= newVal)) {
                        newVal = temp;
                        newDir = 0;
                    }
                }

                // Cas 3 : 2 phonemes pour 1 grapheme
                if ((l >= 2) && (mProbaDyn[l - 2][c - 1] != 0f)) {
                    int indiceDoublePhoneme = lexique.getIndiceFromDoublePhoneme(tPhonemes[l - 2], tPhonemes[l - 1]);
                    double temp;
                    if (indiceDoublePhoneme != -1) {
                        // Cas 3a : le double phoneme existe
                        temp = mProbaDyn[l - 2][c - 1] * mProbaUnGraphemeDeuxPhonemes[indiceGrapheme][indiceDoublePhoneme];
                    } else {
                        // Cas 3b : le double phoneme n'existe pas
                        temp = mProbaDyn[l - 2][c - 1] * probaCreerDoublePhoneme;
                    }
                    if (temp >= newVal) {
                        newVal = temp;
                        newDir = 2;
                    }
                }

                mProbaDyn[l][c] = newVal;
                mDirectionDyn[l][c] = newDir;
            }
        }

        // On parcourt les matrices de Viterbi
        LinkedList<String> alignGraphemes = new LinkedList<String>();
        LinkedList<String> alignPhonemes = new LinkedList<String>();

        int l = tPhonemes.length;
        int c = tGraphemes.length;

        while (c > 0) {
            // On suis le "chemin trace"
            switch (mDirectionDyn[l][c]) {
                case 0:
                    // Phoneme vide
                    alignGraphemes.addFirst(tGraphemes[c - 1]);
                    alignPhonemes.addFirst(stringPhonemeVide);
                    break;
                case 1:
                    // Alignement 1_1
                    alignGraphemes.addFirst(tGraphemes[c - 1]);
                    alignPhonemes.addFirst(tPhonemes[l - 1]);
                    l--;
                    break;
                case 2:
                    // Alignement 1_2
                    alignGraphemes.addFirst(stringGraphemeVide);
                    alignPhonemes.addFirst(tPhonemes[l - 1]);
                    alignGraphemes.addFirst(tGraphemes[c - 1]);
                    alignPhonemes.addFirst(tPhonemes[l - 2]);
                    l -= 2;
                    break;
            }
            c--;
        }

        if (afficherMatricesProgDynamique) {
            System.out.println(Utils.convertirMatriceDouble_String(mProbaDyn) + "\n\n" + Utils.convertirMatriceInt_String(mDirectionDyn));
        }

        AlignementGraphemesPhonemes agp = new AlignementGraphemesPhonemes(alignGraphemes, alignPhonemes);
        agp.setScore(Math.pow(mProbaDyn[tPhonemes.length][tGraphemes.length], 1. / tGraphemes.length));
        return agp;
    }

    /**
     * Fait converger les 2 matrices en alignant tout le dictionnaire plusieurs de suite...
     */
    public void faireConvergerMatricesProbabilites() {
        int nbIterations = 0;
        boolean continuer = true;
        boolean premierIteration = true; // a la premiere iteration on utilise la proba d'aligner avec le phoneme vide init
        ArrayList<Integer>[] mCumulEnConstructionDoublesPhonemes;
        LinkedList<double[][]> lMatricesNouvellesProbas = null;
        AlignementGraphemesPhonemes agp;
        int nbAlignementNonPrisEnCompte;


        while (continuer) {
            nbIterations++;
            nbAlignementNonPrisEnCompte = 0;
            System.out.println("    * Iteration n�" + nbIterations + " / " + nbIterationsMaxCalculMatriceProbas + " : ");
            System.out.println("      (Seuil score alignement : " + Configuration.SEUIL_SCORE_ALIGNEMENT_POUR_TENIR_COMPTE_CALCUL_MATRICE_PROBA + ") ");

            // Init des matrices de cumuls (comme on ne connait pas d'avance le nombre de double phoneme => on utilise une ArrayList pour matriceCumulEnConstructionDoublesPhonemes)
            mCumulAssociationUnGraphemeUnPhoneme = new int[lexique.getNbGraphemes()][lexique.getNbPhonemes()];
            mCumulEnConstructionDoublesPhonemes = new ArrayList[lexique.getNbGraphemes()];
            for (int i = 0; i < mCumulEnConstructionDoublesPhonemes.length; i++) {
                mCumulEnConstructionDoublesPhonemes[i] = new ArrayList<Integer>();
            }

            // Pour chaque ligne du dico => on aligne
            // Attention : certains doubles phonemes vont etre crees et d'autres vont disparaitrent
            // => on utilise la liste Temp du lexique pour reconstruire une liste de doubles phonemes
            System.out.println("        - alignement du dictionnaire...");
            for (final Entry<String, LinkedList<SuitePhonemes>> entry : dico.getDico().entrySet()) {
                for (SuitePhonemes suitePhonemes : entry.getValue()) {
                    String[] tGraphemes = Utils.stringToArrayString(entry.getKey());
                    String[] tPhonemes = suitePhonemes.getTPhonemes();

                    // On aligne
                    agp = alignerGraphemesPhonemes(tGraphemes, tPhonemes, !premierIteration, false);

                    // On parcourt l'alignement si le score > seuil
                    if (agp.getScore() >= Configuration.SEUIL_SCORE_ALIGNEMENT_POUR_TENIR_COMPTE_CALCUL_MATRICE_PROBA) {
                        int indiceGrapheme, indicePhoneme;
                        for (int i = 0; i < agp.getTailleAlignement(); i++) {
                            // Puis on met a jour les matrices de cumuls
                            if ((i < agp.getTailleAlignement() - 1) && (agp.getGrapheme(i + 1).equals("" + stringGraphemeVide))) {
                                // On est sur un double phoneme
                                indiceGrapheme = lexique.getIndiceFromGrapheme(agp.getGrapheme(i));
                                indicePhoneme = lexique.getIndiceFromDoublePhonemeTemp(agp.getPhoneme(i), agp.getPhoneme(i + 1));
                                if (indicePhoneme == -1) {
                                    // Le double phoneme n'existe pas dans le lexique en construction
                                    lexique.ajouterDoublePhonemeTemp(agp.getPhoneme(i), agp.getPhoneme(i + 1));
                                    for (int j = 0; j < lexique.getNbGraphemes(); j++) {
                                        mCumulEnConstructionDoublesPhonemes[j].add((j == indiceGrapheme) ? 1 : 0);
                                    }
                                } else {
                                    // Le double phoneme existait
                                    mCumulEnConstructionDoublesPhonemes[indiceGrapheme].set(indicePhoneme, mCumulEnConstructionDoublesPhonemes[indiceGrapheme].get(indicePhoneme) + 1);
                                }
                                i++; // On increment i, car on a consomme un double phoneme
                            } else {
                                indiceGrapheme = lexique.getIndiceFromGrapheme(agp.getGrapheme(i));
                                indicePhoneme = lexique.getIndiceFromPhoneme(agp.getPhoneme(i));
                                mCumulAssociationUnGraphemeUnPhoneme[indiceGrapheme][indicePhoneme]++;
                            }
                        }
                    } else {
                        nbAlignementNonPrisEnCompte++;
                    }
                }
            }

            // On calcul les nouvelles probas
            System.out.println("        - calcul des nouvelles probabilites...");
            mCumulAssociationUnGraphemeDeuxPhonemes = Utils.convertirTableauArrayList_En_TableauTableau(mCumulEnConstructionDoublesPhonemes);
            lMatricesNouvellesProbas = Utils.convertirMatricesCumulsEnMatricesProbas(mCumulAssociationUnGraphemeUnPhoneme, mCumulAssociationUnGraphemeDeuxPhonemes, probaCreerDoublePhoneme);

            // On verifie si on a converge
            System.out.println("          (nb alignements sous le seuil : " + nbAlignementNonPrisEnCompte + ")");
            System.out.println("        - verification de la convergence...");
            continuer = false;
            if (nbIterations >= nbIterationsMaxCalculMatriceProbas) {
                System.out.println("          => FIN, limite d'iterations atteinte");
            } else if (!lexique.pasDeDifferenceDansLesDoublesPhonemes()) {
                System.out.println("          => doubles phonemes crees ou disparus (" + lexique.getNbDoublesPhonemes() + " -> " + lexique.getNbDoublesPhonemesTemp() + ")...");
                continuer = true;
            } else {
                // Pour chaque grapheme on verifie que les probas associees ne varient pas
                for (int l = 0; l < lexique.getNbGraphemes(); l++) {
                    // Probas 1 grapheme / 1 phoneme
                    for (int c = 0; c < lexique.getNbPhonemes(); c++) {
                        if (Math.abs(mProbaUnGraphemeUnPhoneme[l][c] - lMatricesNouvellesProbas.get(0)[l][c]) >= epsilonConvergenceMatriceProbas) {
                            continuer = true;
                            break;
                        }
                    }
                    // Probas 1 grapheme / 2 phonemes
                    for (int c = 0; c < lexique.getNbDoublesPhonemes(); c++) {
                        String[] doublePhoneme = lexique.getDoublePhonemeFromIndice(c);
                        int indiceTemp = lexique.getIndiceFromDoublePhonemeTemp(doublePhoneme[0], doublePhoneme[1]);
                        if (Math.abs(mProbaUnGraphemeDeuxPhonemes[l][c] - lMatricesNouvellesProbas.get(1)[l][indiceTemp]) >= epsilonConvergenceMatriceProbas) {
                            continuer = true;
                            break;
                        }
                    }
                    if (continuer) {
                        break;
                    }
                }
                if (!continuer) {
                    System.out.println("          => FIN");
                } else {
                    System.out.println("          => pas de convergence");
                }
            }

            if (continuer) {
                System.out.println("        - preparation de l'iteration suivante...");

                // On "commit" les "vrais" doubles phonemes
                lexique.commitDoublesPhonemesTemp();

                // On stock les nouvelles probas
                mProbaUnGraphemeUnPhoneme = lMatricesNouvellesProbas.get(0).clone();
                mProbaUnGraphemeDeuxPhonemes = lMatricesNouvellesProbas.get(1).clone();
            }

            premierIteration = false;
        }
    }

    /**************************************************************************/
    /************************ Sauvegarde et chargement ************************/
    /**************************************************************************/
    /**
     * Stocke la matrice et le lexique dans le fichier passe en parametre
     */
    public void enregistrerMatriceProbaEtLexique(String fichierMatrices) throws IOException {
        PrintWriter f = FileUtils.writeFileUTF(fichierMatrices);
        StringBuffer str = new StringBuffer();

        // Le lexique
        str.append(lexique.toStringPourEnregistrer() + "\n");

        // La matrice de cumul graphemes / phonemes
        for (int l = 0; l < mCumulAssociationUnGraphemeUnPhoneme.length; l++) {
            for (int c = 0; c < mCumulAssociationUnGraphemeUnPhoneme[0].length; c++) {
                str.append(mCumulAssociationUnGraphemeUnPhoneme[l][c] + "\t");
            }
            str.delete(str.length() - 1, str.length());
            str.append("\n");
        }

        // La matrice de cumul graphemes / doubles phonemes
        for (int l = 0; l < mCumulAssociationUnGraphemeDeuxPhonemes.length; l++) {
            for (int c = 0; c < mCumulAssociationUnGraphemeDeuxPhonemes[0].length; c++) {
                str.append(mCumulAssociationUnGraphemeDeuxPhonemes[l][c] + "\t");
            }
            str.delete(str.length() - 1, str.length());
            str.append("\n");
        }

        // La matrice de probas graphemes / phonemes
        for (int l = 0; l < mProbaUnGraphemeUnPhoneme.length; l++) {
            for (int c = 0; c < mProbaUnGraphemeUnPhoneme[0].length; c++) {
                str.append(mProbaUnGraphemeUnPhoneme[l][c] + "\t");
            }
            str.delete(str.length() - 1, str.length());
            str.append("\n");
        }

        // La matrice de probas graphemes / doubles phonemes
        if (mProbaUnGraphemeDeuxPhonemes != null) {
            for (int l = 0; l < mProbaUnGraphemeDeuxPhonemes.length; l++) {
                for (int c = 0; c < mProbaUnGraphemeDeuxPhonemes[0].length; c++) {
                    str.append(mProbaUnGraphemeDeuxPhonemes[l][c] + "\t");
                }
                str.delete(str.length() - 1, str.length());
                str.append("\n");
            }
        }

        f.print(str.toString());
        f.close();
    }

    /**
     * Charge la matrice de proba a partir du fichier passe en parametre
     */
    public void chargerMatriceProbaEtLexique(String fichierMatrices) throws IOException {
    	fichierMatrices = "ressources/"+fichierMatrices;
        BufferedReader f;
        System.out.println("looking for ressource "+fichierMatrices);
//        String fpath = FileUtils.getRessource("plugins.phonetiseur.Phonetiseur", fichierMatrices);
        f = FileUtils.openFileUTF(fichierMatrices);
        String[] t;

        lexique = new LexiqueGraphemesPhonemesPostag();

        // Les graphemes
        t = f.readLine().split("\t");
        for (int i = 0; i < t.length; i++) {
            lexique.ajouterGrapheme(t[i]);
        }

        // Les phonemes
        String s = f.readLine();
        t = s.split("\t");
        for (int i = 0; i < t.length; i++) {
            lexique.ajouterPhoneme(t[i]);
        }
        if (s.charAt(s.length() - 1) == '\t') {
            // Cas ou le phoneme vide est a la fin
            lexique.ajouterPhoneme("");
        }

        // Les double phonemes
        t = f.readLine().split("\t");
        for (int i = 0; i < t.length; i += 2) {
            lexique.ajouterDoublePhoneme(t[i], t[i + 1]);
        }

        // Les posTag
        t = f.readLine().split("\t");
        for (int i = 0; i < t.length; i++) {
            lexique.ajouterPosTag(t[i]);
        }

        // Les matrice score grapheme/phonemes
        mCumulAssociationUnGraphemeUnPhoneme = new int[lexique.getNbGraphemes()][lexique.getNbPhonemes()];
        for (int l = 0; l < lexique.getNbGraphemes(); l++) {
            t = f.readLine().split("\t");
            for (int c = 0; c < lexique.getNbPhonemes(); c++) {
                mCumulAssociationUnGraphemeUnPhoneme[l][c] = new Integer(t[c]);
            }
        }

        // Les matrice score grapheme/doublesphonemes
        mCumulAssociationUnGraphemeDeuxPhonemes = new int[lexique.getNbGraphemes()][lexique.getNbDoublesPhonemes()];
        for (int l = 0; l < lexique.getNbGraphemes(); l++) {
            t = f.readLine().split("\t");
            for (int c = 0; c < lexique.getNbDoublesPhonemes(); c++) {
                mCumulAssociationUnGraphemeDeuxPhonemes[l][c] = new Integer(t[c]);
            }
        }

        // Les matrice probas grapheme/phonemes
        mProbaUnGraphemeUnPhoneme = new double[lexique.getNbGraphemes()][lexique.getNbPhonemes()];
        for (int l = 0; l < lexique.getNbGraphemes(); l++) {
            t = f.readLine().split("\t");
            for (int c = 0; c < lexique.getNbPhonemes(); c++) {
                mProbaUnGraphemeUnPhoneme[l][c] = new Double(t[c]);
            }
        }

        // Les matrice probas grapheme/doublesphonemes
        mProbaUnGraphemeDeuxPhonemes = new double[lexique.getNbGraphemes()][lexique.getNbDoublesPhonemes()];
        for (int l = 0; l < lexique.getNbGraphemes(); l++) {
            t = f.readLine().split("\t");
            for (int c = 0; c < lexique.getNbDoublesPhonemes(); c++) {
                mProbaUnGraphemeDeuxPhonemes[l][c] = new Double(t[c]);
            }
        }
        f.close();
    }

    public void enregistrerVisualisationClassifieurs(String repertoire) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter w;

        if (repertoire.charAt(repertoire.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
            repertoire += System.getProperty("file.separator");
        }

        w = FileUtils.writeFileUTF(repertoire + "simpleOuDoublePhoneme.txt");
        w.print(classifieurs.getClassifieurSimpleOuDoublePhoneme());
        w.close();

        w = FileUtils.writeFileUTF(repertoire + "1erDoublePhoneme.txt");
        w.print(classifieurs.getClassifieurDoublePhoneme1er());
        w.close();

        w = FileUtils.writeFileUTF(repertoire + "2emeDoublePhoneme.txt");
        w.print(classifieurs.getClassifieurDoublePhoneme2eme());
        w.close();

        for (int i = 0; i < lexique.getNbGraphemes(); i++) {
            String graphemeCourant = lexique.getGraphemeFromIndice(i);
            w = FileUtils.writeFileUTF(repertoire + "SimplePhoneme_" + graphemeCourant + ".txt");
            w.print(classifieurs.getTClassifieurSimplePhoneme()[i]);
            w.close();
        }
    }

    /*************************************************************************/
    /******************************* Affichage *******************************/
    /*************************************************************************/
    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        DecimalFormat df = new DecimalFormat("0.000000");

        // On affiche l'entete de la matrice
        str.append(";");
        for (int i = 0; i < lexique.getNbPhonemes(); i++) {
            str.append(lexique.getPhonemeFromIndice(i) + ";");
        }
        str.append("???;");
        for (int i = 0; i < lexique.getNbDoublesPhonemes(); i++) {
            String[] s = lexique.getDoublePhonemeFromIndice(i);
            str.append(s[0] + " " + s[1] + ";");
        }
        str.append("\n");

        if (mProbaUnGraphemeUnPhoneme == null) {
            // Pas de proba => rien
            return "Erreur : matrices de probas vide (generez la ou chargez la a partir d'un fichier...)";
        } else {
            // On affiche le reste de la matrice
            for (int l = 0; l < lexique.getNbGraphemes(); l++) {
                // La cellule a gauche
                str.append(lexique.getGraphemeFromIndice(l) + ";");
                // Les autres cellules
                for (int c = 0; c < lexique.getNbPhonemes(); c++) {
                    str.append(df.format(mProbaUnGraphemeUnPhoneme[l][c]) + ";");
                }
                // La proba "un grapheme pour deux phonemes"
                str.append(probaCreerDoublePhoneme + ";");
                // Les doubles phonemes
                for (int c = 0; c < lexique.getNbDoublesPhonemes(); c++) {
                    str.append(df.format(mProbaUnGraphemeDeuxPhonemes[l][c]) + ";");
                }
                str.append("\n");
            }
        }
        return str.toString();
    }

    /**
     * Cette methode retourne les nb plus mauvais alignements
     */
    public String getPlusMauvaisAlignementsString(int nb) {
        StringBuffer str = new StringBuffer();
        ArrayList<AlignementGraphemesPhonemes> alAlignements = new ArrayList<AlignementGraphemesPhonemes>();

        // Pour chaque entree du dico
        System.out.print("Creation des alignements...");
        for (Entry<String, LinkedList<SuitePhonemes>> entry : dico.getDico().entrySet()) {
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                alAlignements.add(alignerGraphemesPhonemes(Utils.stringToArrayString(entry.getKey()), suitePhonemes.getTPhonemes(), true, false));
            }
        }

        // Recherche des nb min
        System.out.print(" Recherche...");
        AlignementGraphemesPhonemes minAl = null;
        double min;
        nb = Math.min(nb, alAlignements.size() / 2);
        while (nb > 0) {
            min = 1;
            for (int i = 0; i < alAlignements.size() - 1; i++) {
                if (alAlignements.get(i).getScore() < min) {
                    minAl = alAlignements.get(i);
                    min = minAl.getScore();
                }
            }
            nb--;
            alAlignements.remove(minAl);
            str.append(minAl + "\n\n");
        }

        System.out.println(" Ok");
        return str.toString();
    }

    /*************************************************************************/
    /***************************** Fichiers ARFF *****************************/
    /*************************************************************************/
    /**
     * Methode qui genere les fichiers ARFF correspondant a la methode utilisant
     * 4 classifieur :
     * - un pour determiner si le grapheme courant entraine un simple ou un double phoneme
     * - un pour determiner les simples phonemes (un par grapheme)
     * - un pour determiner le 1er phoneme des doubles phonemes
     * - un pour determiner le 2eme phoneme des doubles phonemes
     */
    public void genererFichiersVecteursARFF(String repertoire) throws IOException {
        //String fSimpleOuDoublePhoneme, String fSimplePhoneme, String fDoublePhoneme1er, String fDoublePhoneme2eme
        PrintWriter fichierSimpleOuDoublePhoneme = FileUtils.writeFileUTF(repertoire + Configuration.NOM_FICHIER_ARFF_SIMPLE_OU_DOUBLE_PHONEME + ".arff");
        PrintWriter fichierDoublePhoneme1er = FileUtils.writeFileUTF(repertoire + Configuration.NOM_FICHIER_ARFF_1er_DOUBLE_PHONEME + ".arff");
        PrintWriter fichierDoublePhoneme2eme = FileUtils.writeFileUTF(repertoire + Configuration.NOM_FICHIER_ARFF_2eme_DOUBLE_PHONEME + ".arff");
        PrintWriter[] tFichierSimplePhoneme = new PrintWriter[lexique.getNbGraphemes()];
        String[] tGraphemes, tPhonemes, tContenuARFF;
        AlignementGraphemesPhonemes agp;
        File n;

        System.out.print("Generation des fichiers ARFF...");

        for (int i = 0; i < tFichierSimplePhoneme.length; i++) {
            tFichierSimplePhoneme[i] = FileUtils.writeFileUTF(repertoire + Configuration.NOM_FICHIER_ARFF_SIMPLE_PHONEME + "_" + lexique.getGraphemeFromIndice(i) + ".arff");
        }

        // Les entetes
        String[] entetes = getARFF_Entete_Methode_4Classifieurs(lexique.getStringGraphemes(","), lexique.getStringPhonemesSimples(","), lexique.getStringPosTag(","), Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR, Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
        fichierSimpleOuDoublePhoneme.println(entetes[0]);
        for (int i = 0; i < tFichierSimplePhoneme.length; i++) {
            tFichierSimplePhoneme[i].println(entetes[1]);
        }
        fichierDoublePhoneme1er.println(entetes[2]);
        fichierDoublePhoneme2eme.println(entetes[3]);

        // Le contenu
        for (Entry<String, LinkedList<SuitePhonemes>> entry : dico.getDico().entrySet()) {
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                // Pour chaque couple (mot, phonetique)
                tGraphemes = Utils.stringToArrayString(entry.getKey());
                tPhonemes = suitePhonemes.getTPhonemes();
                agp = alignerGraphemesPhonemes(tGraphemes, tPhonemes, true, false);
                tContenuARFF = agp.getVecteur_Methode_4Classifieurs(Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR, Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR, suitePhonemes.getPosTag(), stringGraphemeVide);
                if (!tContenuARFF[0].equals("")) {
                    fichierSimpleOuDoublePhoneme.println(tContenuARFF[0]);
                }
                if (!tContenuARFF[1].equals("")) {
                    String[] vecteurs = tContenuARFF[1].split("\n");
                    for (int i = 0; i < vecteurs.length; i++) {
                        String grapheme = vecteurs[i].substring(0, vecteurs[i].indexOf(","));
                        tFichierSimplePhoneme[lexique.getIndiceFromGrapheme(grapheme)].println(vecteurs[i]);
                    }
                }
                if (!tContenuARFF[2].equals("")) {
                    fichierDoublePhoneme1er.println(tContenuARFF[2]);
                }
                if (!tContenuARFF[3].equals("")) {
                    fichierDoublePhoneme2eme.println(tContenuARFF[3]);
                }
            }
        }

        // On ferme les fichiers
        fichierSimpleOuDoublePhoneme.close();
        for (int i = 0; i < tFichierSimplePhoneme.length; i++) {
            tFichierSimplePhoneme[i].close();
        }
        fichierDoublePhoneme1er.close();
        fichierDoublePhoneme2eme.close();

        System.out.println(" Ok");
    }

    /**
     * Retourne les entetes des fichiers ARFF
     * pour la methode a 4 classifieurs.
     * t[0] => simple ou double phoneme
     * t[1] => simples phonemes
     * t[2] => 1ers phonemes des doubles phonemes
     * t[3] => 2emes phonemes des doubles phonemes
     * @param graphemes tous les graphemes separes par des virgules
     * @param phonemes tous les phonemes separes par des virgules
     * @param postag tous les postag separes par des virgules
     * @param graphemeDebut String representant le debut d'un mot
     * @param graphemeFin String representant la fin d'un mot
     * @return
     */
    public static String[] getARFF_Entete_Methode_4Classifieurs(String graphemes, String phonemes, String postag, String graphemeDebut, String graphemeFin) {
        String[] t = new String[4];

        t[0] = "@relation SimpleOuDoublePhoneme\n" +
                "@attribute graphemeCentral {" + graphemes + "}\n" +
                "@attribute gGauche1 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche2 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche3 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche4 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gDroite1 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite2 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite3 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite4 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute posTag {" + postag + "}\n" +
                "@attribute typePhoneme {simple,double}\n" +
                "\n@data\n";

        t[1] = "@relation SimplesPhonemes\n" +
                "@attribute graphemeCentral {" + graphemes + "}\n" +
                "@attribute gGauche1 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche2 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche3 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche4 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gDroite1 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite2 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite3 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite4 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute posTag {" + postag + "}\n" +
                "@attribute phoneme {" + phonemes + "}\n" +
                "\n@data\n";

        t[2] = "@relation 1erDoublesPhonemes\n" +
                "@attribute graphemeCentral {" + graphemes + "}\n" +
                "@attribute gGauche1 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche2 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche3 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche4 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gDroite1 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite2 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite3 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite4 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute posTag {" + postag + "}\n" +
                "@attribute phoneme {" + phonemes + "}\n" +
                "\n@data\n";

        t[3] = "@relation 2emeDoublesPhonemes\n" +
                "@attribute graphemeCentral {" + graphemes + "}\n" +
                "@attribute phonemePrecedent {" + phonemes + "}\n" +
                "@attribute gGauche1 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche2 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche3 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gGauche4 {" + graphemeDebut + "," + graphemes + "}\n" +
                "@attribute gDroite1 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite2 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite3 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute gDroite4 {" + graphemeFin + "," + graphemes + "}\n" +
                "@attribute posTag {" + postag + "}\n" +
                "@attribute phoneme {" + phonemes + "}\n" +
                "\n@data\n";

        return t;
    }

    /************************************************************************/
    /***************************** Classifieurs *****************************/
    /************************************************************************/
    /**
     * Lance l'apprentissage des classifieurs.
     * @param repertoireFichiersARFF le repertoire ou se situe les fichiers ARFF
     */
    public void lancerApprentissage(String repertoireFichiersARFF) throws Exception {
        if (repertoireFichiersARFF.charAt(repertoireFichiersARFF.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
            repertoireFichiersARFF += System.getProperty("file.separator");
        }
        System.out.println("Classifieurs (apprentissage) :");
        classifieurs = new Classifieurs(lexique, repertoireFichiersARFF);
        classifieurs.lancerApprentissage(repertoireFichiersARFF);
    }

    /**
     * Phonetise un mot (passe sous forme de tableau de grapheme).
     * Passer null au parametre posTag si on ne souhaite pas en tenir compte
     */
    public String[] phonetiser(String[] tGrapheme, String posTag) throws Exception {
        return classifieurs.phonetiser(tGrapheme, posTag).getPhonemes();
    }

    /**
     * Sauvegarde les classifieurs dans des fichiers dans le repertoire passe en parametre
     */
    public void sauvegarderClassifieurs(String cheminRepertoireCible) throws IOException {
        if (cheminRepertoireCible.charAt(cheminRepertoireCible.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
            cheminRepertoireCible += System.getProperty("file.separator");
        }
        System.out.print("Classifieurs (sauvegarde)...");
        classifieurs.sauvegarderClassifieurs(cheminRepertoireCible);
        System.out.println(" Ok");
    }

    /**
     * Charger les classifieurs dans des fichiers dans le repertoire passe en parametre
     */
    public void chargerClassifieurs(String chemineRepertoireARFF, String cheminRepertoireModel) throws Exception {
    	chemineRepertoireARFF = "ressources/"+chemineRepertoireARFF;
    	cheminRepertoireModel = "ressources/"+cheminRepertoireModel;
    	
    	if (chemineRepertoireARFF.charAt(chemineRepertoireARFF.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
//            chemineRepertoireARFF += System.getProperty("file.separator");
        }
    	System.out.println("CALL1 "+chemineRepertoireARFF);
        if (cheminRepertoireModel.charAt(cheminRepertoireModel.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
//            cheminRepertoireModel += System.getProperty("file.separator");
        }
    	System.out.println("CALL2 "+cheminRepertoireModel);
        System.out.print("Classifieurs (chargement)...");
        classifieurs = new Classifieurs(lexique, chemineRepertoireARFF);
        classifieurs.chargerClassifieurs(cheminRepertoireModel);
        System.out.println(" Ok");
    }

    /**
     * Lance la phonetisation sur les mots sur le fichiers passe en parametre.
     * "Avec Postag" signifie qu'une phonetisation est compt� juste si et seulement si un mot du dico ayant le meme posTag que le mot de test
     * a la meme phonetisation
     * @return le pourcentage de reussite
     */
    public double testerAvecPosTag(PronunciationsLexicon pronuncitationsLexicon, String cheminFichierMotsTests, String cheminFichierMauvaisAlignements) throws Exception {
        LexiqueGraphemesPhonemesPostag l = new LexiqueGraphemesPhonemesPostag();
        DicoGraphemesPhonemes dicoTest = new DicoGraphemesPhonemes(pronuncitationsLexicon, cheminFichierMotsTests, l);
        String[] tGraphemes, tPhonemesResultat;
        ArrayList<String> alPostag;
        boolean erreur;
        AlignementGraphemesPhonemes agp;

        int nbTotalMot = 0;
        int nbOkMot = 0;
        double score;

        PrintWriter f = FileUtils.writeFileUTF(cheminFichierMauvaisAlignements);

        for (Entry<String, LinkedList<SuitePhonemes>> entry : dicoTest.getDico().entrySet()) {
            // Pour chaque mot du dico de test
            tGraphemes = Utils.stringToArrayString(entry.getKey());

            // On construit la liste de tous les postag qui apparaissent pour le mot courant
            alPostag = new ArrayList<String>();
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                if (!alPostag.contains(suitePhonemes.getPosTag())) {
                    alPostag.add(suitePhonemes.getPosTag());
                    nbTotalMot++;
                }
            }

            // On lance une phonetisation par Postag
            for (int i = 0; i < alPostag.size(); i++) {
                erreur = true;
                agp = classifieurs.phonetiser(tGraphemes, alPostag.get(i));
                tPhonemesResultat = agp.getPhonemes();
                // On recherche si le couple (resultatPhonetisation, postag) existe
                if (entry.getValue().contains(new SuitePhonemes(tPhonemesResultat, alPostag.get(i)))) {
                    nbOkMot++;
                    erreur = false;
                }

                if (erreur) {
                    // On affiche les mauvaises phonetisations
                    f.println(agp);
                    f.println("Solutions du dico :");
                    for (SuitePhonemes suitePhonemes : entry.getValue()) {
                        if (suitePhonemes.getPosTag().equals(alPostag.get(i))) {
                            f.println(suitePhonemes.toStringDebug() + "\n");
                        }
                    }
                }
            }
        }
        score = ((double) (nbOkMot * 100)) / ((double) nbTotalMot);
        f.println("\n\n=============> Resultat par mots : " + score + "%");
        f.close();

        return score;
    }

    /**
     * Lance la phonetisation sur les mots sur le fichiers passe en parametre.
     * "Sans Postag" signifie qu'une phonetisation est compt� juste si on trouve a mot du dico identique au mot de test
     * ayant la meme phonetisation, quelque soit le PosTag du mot du dico
     * @return le pourcentage de reussite
     */
    public double testerSansPosTag(PronunciationsLexicon pronuncitationsLexicon, String cheminFichierMotsTests, String cheminFichierMauvaisAlignements) throws Exception {
        // !!!!!!! ne pas renseigner la valeur du posTag => modifier Classifieurs
        LexiqueGraphemesPhonemesPostag l = new LexiqueGraphemesPhonemesPostag();
        DicoGraphemesPhonemes dicoTest = new DicoGraphemesPhonemes(pronuncitationsLexicon, cheminFichierMotsTests, l);
        String[] tGraphemes, tPhonemesResultat;
        ArrayList<String> alPostag;
        boolean erreur;
        AlignementGraphemesPhonemes agp;

        int nbTotalMot = 0;
        int nbOkMot = 0;
        double score;

        PrintWriter f = FileUtils.writeFileUTF(cheminFichierMauvaisAlignements);

        for (Entry<String, LinkedList<SuitePhonemes>> entry : dicoTest.getDico().entrySet()) {
            // Pour chaque mot du dico de test
            tGraphemes = Utils.stringToArrayString(entry.getKey());

            erreur = true;
            agp = classifieurs.phonetiser(tGraphemes, null);
            tPhonemesResultat = agp.getPhonemes();
            nbTotalMot++;

            // On recherche si une phon�tisation du mot existe, peut importe le posTag
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                String[] t = suitePhonemes.getTPhonemes();
                if (t.length == tPhonemesResultat.length) {
                    erreur = false; // On considere que les phonetisations sont les memes
                    // On repasse erreur a true si on trouve une difference...
                    for (int i = 0; i < t.length; i++) {
                        if (!t[i].equals(tPhonemesResultat[i])) {
                            erreur = true;
                        }
                    }
                    if (erreur == false) {
                        // On a trouve une phonetisation qui match... pas besoin de continuer a chercher
                        nbOkMot++;
                        break;
                    }
                }
            }

            if (erreur) {
                // On affiche les mauvaises phonetisations
                f.println(agp);
                f.println("Solutions du dico :");
                for (SuitePhonemes suitePhonemes : entry.getValue()) {
                    f.println(suitePhonemes.toStringDebug() + "\n");
                }
            }
        }
        score = ((double) (nbOkMot * 100)) / ((double) nbTotalMot);
        f.println("\n\n=============> Resultat par mots : " + score + "%");
        f.close();

        return score;
    }

    /*************************************************************************/
    /************************ Accesseurs et mutateurs ************************/
    /*************************************************************************/
    public DicoGraphemesPhonemes getDico() {
        return dico;
    }

    public void setDico(DicoGraphemesPhonemes dico) {
        this.dico = dico;
    }

    public LexiqueGraphemesPhonemesPostag getLexique() {
        return lexique;
    }

    public void setLexique(LexiqueGraphemesPhonemesPostag lexique) {
        this.lexique = lexique;
    }

    public int[][] getMCumulAssociationUnGraphemeDeuxPhonemes() {
        return mCumulAssociationUnGraphemeDeuxPhonemes;
    }

    public void setMCumulAssociationUnGraphemeDeuxPhonemes(int[][] mCumulAssociationUnGraphemeDeuxPhonemes) {
        this.mCumulAssociationUnGraphemeDeuxPhonemes = mCumulAssociationUnGraphemeDeuxPhonemes;
    }

    public int[][] getMCumulAssociationUnGraphemeUnPhoneme() {
        return mCumulAssociationUnGraphemeUnPhoneme;
    }

    public void setMCumulAssociationUnGraphemeUnPhoneme(int[][] mCumulAssociationUnGraphemeUnPhoneme) {
        this.mCumulAssociationUnGraphemeUnPhoneme = mCumulAssociationUnGraphemeUnPhoneme;
    }

    public double[][] getMProbaUnGraphemeDeuxPhonemes() {
        return mProbaUnGraphemeDeuxPhonemes;
    }

    public void setMProbaUnGraphemeDeuxPhonemes(double[][] mProbaUnGraphemeDeuxPhonemes) {
        this.mProbaUnGraphemeDeuxPhonemes = mProbaUnGraphemeDeuxPhonemes;
    }

    public double[][] getMProbaUnGraphemeUnPhoneme() {
        return mProbaUnGraphemeUnPhoneme;
    }

    public void setMProbaUnGraphemeUnPhoneme(double[][] mProbaUnGraphemeUnPhoneme) {
        this.mProbaUnGraphemeUnPhoneme = mProbaUnGraphemeUnPhoneme;
    }

    public Classifieurs getClassifieurs() {
        return classifieurs;
    }

    public void setClassifieurs(Classifieurs classifieurs) {
        this.classifieurs = classifieurs;
    }

    /*************************************************************************/
    /******************************** Anciens ********************************/
    /*************************************************************************/
    /**
     * Genere les vecteurs sous forme de fichier ARFF
     * ATTENTION : l'algo va aligner les graphemes et les phonemes du dico.
     *             On suppose que les doubles phonemes crees sont tous dans le lexique
     */
    public void genererFichierVecteursARFF_ANCIEN(String nomFichier) throws IOException {
        PrintWriter fichier = FileUtils.writeFileUTF(nomFichier);
        String[] tGraphemes, tPhonemes;
        AlignementGraphemesPhonemes agp;

        System.out.print("Generation du fichier ARFF...");
        //fichier.println(getARFF_Entete_ANCIEN("Vecteurs", lexique.getStringGraphemes(","), lexique.getStringPhonemesEtDoublesPhonemes(",", "#"), lexique.getStringPosTag(","), Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR, Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR));
        fichier.println(getARFF_Entete_ANCIEN("Vecteurs", lexique.getStringGraphemes(","), lexique.getStringPhonemesSimples(","), lexique.getStringPosTag(","), Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR, Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR));

        for (Entry<String, LinkedList<SuitePhonemes>> entry : dico.getDico().entrySet()) {
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                // Pour chaque couple (mot, phonetique)
                tGraphemes = Utils.stringToArrayString(entry.getKey());
                tPhonemes = suitePhonemes.getTPhonemes();
                agp = alignerGraphemesPhonemes(tGraphemes, tPhonemes, true, false);
                fichier.println(agp.getVecteursString(Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR, Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR, suitePhonemes.getPosTag(), stringGraphemeVide, "#"));
            }
        }

        fichier.close();
        System.out.println(" Ok");
    }

    /**
     * === ANCIEN ===
     * Retourne l'entete du fichier ARFF.
     * (Dans le cas ou l'on souhaite appeler, par la suite, la methode getCSV_Vecteur)
     */
    public static String getARFF_Entete_ANCIEN(String nomRelation, String graphemes, String phonemesEtDoublesPhonemes, String postag, String graphemeDebut, String graphemeFin) {
        StringBuffer str = new StringBuffer();

        str.append("@relation " + nomRelation + "\n");
        str.append("@attribute graphemeCentral {" + graphemes + "}\n");

        str.append("@attribute gGauche1 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gGauche2 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gGauche3 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gGauche4 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gDroite1 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gDroite2 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gDroite3 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute gDroite4 {" + graphemeDebut + "," + graphemeFin + "," + graphemes + "}\n");
        str.append("@attribute posTag {" + postag + "}\n");
        str.append("@attribute phoneme {" + phonemesEtDoublesPhonemes + "}\n");
        str.append("\n@data\n");

        return str.toString();
    }
}
