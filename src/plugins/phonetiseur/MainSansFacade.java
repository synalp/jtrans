/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur;

import java.io.*;
import java.util.*;
import java.util.Map.*;

import plugins.utils.*;
import weka.core.*;

/**
 * Ce Main regroupe differentes utilisation du phonetiseur, sans utiliser la facade
 */
public class MainSansFacade {

    public static void main(String[] args) throws Exception {

        String nomRepTravail = "/home/jean/StageLORIA/PourExecution/";//"/home/jean/StageLORIA/";//"./";
        String nomRepListesDeMots = "/home/jean/StageLORIA/ListesDeMots/";//"/home/jean/StageLORIA/ListesDeMots/";//"../ListesDeMots/";
        String nomFichierProbasSauv = "===-MatricesProbas_090824-===.sauv";
        String nomFichierMotsTRAIN = "motsAApprendre_dicoNomsPropre_TRAIN.txt";//"motsAApprendre_dicoNomsPropre_TRAIN.txt";//"motsAApprendre_BDELex_TRAIN.txt";//"motsAApprendre_Morphalou_TRAIN.txt";
        String nomFichierMotsTEST = "motsAApprendre_dicoNomsPropre_TEST.txt";//"motsAApprendre_dicoNomsPropre_TEST.txt";//"motsAApprendre_BDELex_TEST.txt";//"motsAApprendre_Morphalou_TEST.txt";
        Phonetiseur phonetiseur;
        //PronunciationsLexicon pL = new Morphalou("/home/jean/StageLORIA/Dicos/ListeFinal_Morph_Dico_f.txt", "");//new Morphalou("../ListeFinal_Morph_Dico_f.txt", "");
        //PronunciationsLexicon pL = new BDLex("/home/jean/StageLORIA/Dicos/BDLEX-V2.1/BDLex", "");//new BDLex("/home/jean/StageLORIA/Dicos/BDLEX-V2.1/BDLex", "");//new BDLex("../BDLex", "");
        PronunciationsLexicon pL = new BDLex("/home/jean/StageLORIA/Dicos/BDLexNomsPropres", "");//new BDLex("", "/home/jean/Bureau/dico_nompropres.bdlex");//new BDLex("/home/jean/StageLORIA/Dicos/BDLexNomsPropres", "");
        PrintWriter f;


//      /*****************************************************************************/
//      /* On charge le dictionnaire, on genere la matrice de probas puis on la sauv */
//      /*****************************************************************************/
        phonetiseur = new Phonetiseur(pL, nomRepListesDeMots + nomFichierMotsTRAIN);
        phonetiseur.genererMatricesProbabilites();
        phonetiseur.enregistrerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
        System.out.println("========> Fin generation matrice probas\n\n");
//
//      /******************************************/
//      /* On veut afficher la matrice des probas */
//      /******************************************/
        f = FileUtils.writeFileUTF(nomRepTravail + "matriceProbas.csv");
//        phonetiseur = new Phonetiseur();
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
        f.print(phonetiseur);
        f.close();
        System.out.println("========> Fin matrice probas -> CSV\n\n");
//
//
//      /*****************************************/
//      /* On veut afficher tous les alignements */
//      /*****************************************/
        f = FileUtils.writeFileUTF(nomRepTravail + "alignements.txt");
//        phonetiseur = new Phonetiseur(pL, nomRepListesDeMots + nomFichierMotsTRAIN);
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
        for (Entry<String, LinkedList<SuitePhonemes>> entry : phonetiseur.getDico().getDico().entrySet()) {
            for (SuitePhonemes suitePhonemes : entry.getValue()) {
                f.println(phonetiseur.alignerGraphemesPhonemes(Utils.stringToArrayString(entry.getKey()), suitePhonemes.getTPhonemes(), true, false) + "\n");
            }
        }
        f.close();
        System.out.println("========> Fin alignements\n\n");
//
//      /****************************************************************************/
//      /* On charge la matrice de probas et affichage des plus mauvais alignements */
//      /****************************************************************************/
        f = FileUtils.writeFileUTF(nomRepTravail + "mauvaisAlignements.txt");
//        phonetiseur = new Phonetiseur(pL, nomRepListesDeMots + nomFichierMotsTRAIN);
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
        f.println(phonetiseur.getPlusMauvaisAlignementsString(100));
        f.close();
        System.out.println("========> Fin mauvais alignements\n\n");
//
//      /*********************/
//      /* Test d'alignement */
//      /*********************/
//        phonetiseur = new Phonetiseur();
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
//        String[] g = {"c", "o", "-", "c", "l", "e", "r", "c"};
//        String[] p = {"k", "oh", "k", "l", "E", "R"};
//        System.out.println(phonetiseur.alignerGraphemesPhonemes(g, p, true, true));
//        String[] g2 = {"c", "h", "a", "u", "d"};
//        String[] p2 = {"S", "O"};
//        System.out.println(phonetiseur.alignerGraphemesPhonemes(g2, p2, true, true));
//        System.out.println("========> Fin tests alignements\n\n");
//
//      /********************************/
//      /* On veut generer les vecteurs */
//      /********************************/
//        phonetiseur = new Phonetiseur(pL, nomRepListesDeMots + nomFichierMotsTRAIN);
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
        phonetiseur.genererFichiersVecteursARFF(nomRepTravail + "ARFF/");
//        System.out.println("========> Fin generation vecteurs\n\n");
//
//      /*************************************************/
//      /* On veut lancer et sauvegarder l'apprentissage */
//      /*************************************************/
//        phonetiseur = new Phonetiseur();
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
        phonetiseur.lancerApprentissage(nomRepTravail + "ARFF/");
        phonetiseur.sauvegarderClassifieurs(nomRepTravail + "Models/");
        System.out.println("========> Fin apprentissage\n\n");
//
//      /*****************************************************/
//      /* On enregistre la "visualisation" des classifieurs */
//      /*****************************************************/
        phonetiseur.enregistrerVisualisationClassifieurs(nomRepTravail + "VisualisationClassifieurs/");
//
//      /*******************************************/
//      /* On va charger les classifieurs et tests */
//      /*******************************************/
//        phonetiseur = new Phonetiseur();
//        phonetiseur.chargerMatriceProbaEtLexique(nomRepTravail + nomFichierProbasSauv);
//        phonetiseur.chargerClassifieurs(nomRepTravail + "ARFF/", nomRepTravail + "Models/");
        System.out.println("Avec posTag : " + phonetiseur.testerAvecPosTag(pL, nomRepListesDeMots + nomFichierMotsTEST, nomRepTravail + "mauvaisesPhonetisationsAvecPosTag.txt") + "%");
        System.out.println("Sans posTag : " + phonetiseur.testerSansPosTag(pL, nomRepListesDeMots + nomFichierMotsTEST, nomRepTravail + "mauvaisesPhonetisationsSansPosTag.txt") + "%");
        System.out.println("Avec posTag sur Train : " + phonetiseur.testerAvecPosTag(pL, nomRepListesDeMots + nomFichierMotsTRAIN, nomRepTravail + "mauvaisesPhonetisationsAvecPosTag_SUR_TRAIN.txt") + "%");
    }
}
