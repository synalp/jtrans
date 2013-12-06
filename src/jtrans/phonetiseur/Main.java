/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.phonetiseur;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;

import jtrans.utils.BDLex;
import jtrans.utils.FileUtils;

/**
 * Permet d'executer le phonetiseur en standAlone
 */
public class Main {

    public static final String NOM_REP_ARFF = "arff/";
    public static final String NOM_REP_MODELS = "models/";
    public static final String NOM_FICHIER_GRAPHEMES_PHONEMES_MATRICE_PROBAS = "graphemes_phonemes_matriceProba";

    public static void main(String[] args) throws Exception {
        File repPourPhonetiseur, repARFF, repModels;
        String nomRepARFF = null, nomRepModels = null;
        PhonetiseurFacade phonetiseur;

        if (args.length < 2) {
            afficherAideEtFin();
        } else {
            nomRepARFF = obtenirCheminRepAvecSeparateurALaFin(args[1]) + NOM_REP_ARFF;
            nomRepModels = obtenirCheminRepAvecSeparateurALaFin(args[1]) + NOM_REP_MODELS;
        }

        if (args[0].equals("-a")) {
            /***************************************************************/
            /************************ Apprentissage ************************/
            /***************************************************************/
            if (args.length < 4) {
                afficherAideEtFin();
            }

            // On cree les repertoires si besoin
            repPourPhonetiseur = new File(args[1]);
            if (!repPourPhonetiseur.mkdir() && !repPourPhonetiseur.isDirectory()) {
                System.out.println("Erreur lors de la creation du repertoire \"" + args[1] + "\"");
            }
            repARFF = new File(nomRepARFF);
            repARFF.mkdir();
            repModels = new File(nomRepModels);
            repModels.mkdir();

            // On charge JHTK
            String[] tDicos = new String[args.length - 3];
            for (int i = 2; i < args.length - 1; i++) {
                tDicos[i - 2] = args[i];
            }
            BDLex bdlex = new BDLex(tDicos, true);

            // On lance l'apprentissage
            phonetiseur = new PhonetiseurFacade(obtenirCheminRepAvecSeparateurALaFin(args[1]) + NOM_FICHIER_GRAPHEMES_PHONEMES_MATRICE_PROBAS, nomRepARFF, nomRepModels);
            phonetiseur.lancerApprentissage(bdlex, args[args.length - 1]);

        } else if (args[0].equals("-p")) {
            /***************************************************************/
            /************************ Phonetisation ************************/
            /***************************************************************/
            if (args.length != 4) {
                afficherAideEtFin();
            }

            phonetiseur = new PhonetiseurFacade(obtenirCheminRepAvecSeparateurALaFin(args[1]) + NOM_FICHIER_GRAPHEMES_PHONEMES_MATRICE_PROBAS, nomRepARFF, nomRepModels);
            phonetiseur.chargerClassifieurs();

            BufferedReader r = FileUtils.openFileUTF(args[2]);
            PrintWriter w = FileUtils.writeFileUTF(args[3]);
            String sMotAPhonetiser, posTag;
            String[] t;

            while ((sMotAPhonetiser = r.readLine()) != null) {
                t = sMotAPhonetiser.split(";");
                posTag = ((t.length > 1) && (!t[1].equals(""))) ? t[1] : null;
                if (!DicoGraphemesPhonemes.motNonRejete(t[0])) {
                    System.out.println("Mot rejete : " + t[0]);
                } else {
                    try {
                        w.println(t[0] + ";" + ((posTag == null) ? "" : posTag) + ";" + phonetiseur.phonetiser(t[0], posTag));
                    } catch (Exception ex) {
                        String p = (posTag == null) ? "" : "(posTag : " + posTag + ") ";
                        System.out.println("Erreur : mot \"" + t[0] + "\" " + p + "non phonetise. Peut etre a cause d'un graph�me ou d'un PosTag inconnu...");
                    }
                }
            }

            r.close();

            w.close();
        } else {
            afficherAideEtFin();
        }

    }

    public static void afficherAideEtFin() {
        System.out.println("\n\n" +
                "=============================\n" +
                "======== Utilisation ========\n" +
                "=============================\n\n" +
                "Pour executer un apprentissage :\n" +
                "    \"Main\" -a repDonnees fichierDico_1 ... fichierDico_n fichierMotsAApprendre\n" +
                "Pour executer une phonetisation :\n" +
                "    \"Main\" -p repDonnees fichier_mots_posTags fichierResultats\n\n" +
                "Avec :\n" +
                "  - repDonnees : un repertoire utilise par le phon�tiseur pour lire/ecrire differentes informations,\n" +
                "  - fichierDico_i : un dictionnaire au format BDLex (attention : le fichier doit etre au format UTF8 ou les accents doivent etre au format BDLex),\n" +
                "  - fichierMotsAApprendre : un fichier contenant la liste des mots a apprendre dans les dictionnaires (un mot par ligne),\n" +
                "  - fichierResultats : le chemin vers un fichier dans lequel le phonetiseur placera ses resultats,\n" +
                "  - fichier_mots_posTags : un fichier contenant les mots a phonetiser. Les accents NE doivent PAS etre au format BDLex. Le fichier doit etre compose d'une ligne par mot. Un mot est suivi d'un point-virgule puis de son posTag. Ne rien mettre a la place du posTag si on ne souhaite pas l'indiquer.\n");
        System.exit(0);
    }

    public static String obtenirCheminRepAvecSeparateurALaFin(
            String cheminRep) {
        if (cheminRep.charAt(cheminRep.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            cheminRep += System.getProperty("file.separator");
        }

        return cheminRep;
    }
}
