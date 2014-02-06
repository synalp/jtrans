/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package fr.loria.synalp.jtrans.phonetiseur;

import java.io.*;
import java.util.*;

import fr.loria.synalp.jtrans.utils.DicoEntry;
import fr.loria.synalp.jtrans.utils.FileUtils;
import fr.loria.synalp.jtrans.utils.PronunciationsLexicon;

/**
 * Classe qui charge le dictionnaire Grapheme -> Phoneme.
 * Lors du chargement, une liste des graphemes et des phonemes rencontres est cree.
 */
public class DicoGraphemesPhonemes {

    private Hashtable<String, LinkedList<SuitePhonemes>> dico;

    public DicoGraphemesPhonemes(PronunciationsLexicon pronuncitationsLexicon, String cheminFichierMotsAApprendre, LexiqueGraphemesPhonemesPostag lexique) {
        String mot, motAppostropheModifiee, posTag;
        String[] tPhonemes;
        ArrayList<DicoEntry> alEntrees;
        ArrayList<String> alPhonemes;

        System.out.println("Chargement du dictionnaire... ");
        dico = new Hashtable<String, LinkedList<SuitePhonemes>>();
        try {
            BufferedReader f = FileUtils.openFileUTF(cheminFichierMotsAApprendre);

            while ((mot = f.readLine()) != null) {
                mot = mot.toLowerCase();

                // On remplace les appostrophes (bug avec format ARFF)
                motAppostropheModifiee = mot.replace('\'', Configuration.CHAR_DE_REMPLACEMENT_APPOSTROPHE);

                /*
                System.out.println("\n=========== " + motAppostropheModifiee + " ============");
                alEntrees = pronuncitationsLexicon.getDicoEntries(mot);
                for (int i = 0; i < alEntrees.size(); i++) {
                System.out.println("*** " + alEntrees.get(i).getPOStag() + " ***");
                alPhonemes = alEntrees.get(i).getExtensionNoLiaisonsWithSwa();
                for (int j = 0; j < alPhonemes.size(); j++) {
                System.out.println(alPhonemes.get(j) );
                }
                }
                 */

                if (motNonRejete(motAppostropheModifiee) && !dico.containsKey(motAppostropheModifiee)) {
                    // Le mot est accepte
                    alEntrees = pronuncitationsLexicon.getDicoEntries(mot);
                    for (int i = 0; i < alEntrees.size(); i++) {
                        // Suivant le posTag
                        posTag = alEntrees.get(i).getPOStag().toString();
                        //////////////////alPhonemes = alEntrees.get(i).getExtensionNoLiaisonsWithSwa();
                        alPhonemes = alEntrees.get(i).getExtensionNoLiaisonsWithSwa(); // <==============================  SWA  !!!!!!!!!!!!!!!!!!!!!!!
                        for (int j = 0; j < alPhonemes.size(); j++) {
                            // Toutes les phonetisations
                            tPhonemes = getTableauSansSil(alPhonemes.get(j)); ///////////////////////////////////////////////////:
                            if (tPhonemes.length <= mot.length() * 2) {
                                // On rejete les mots dans le nb de phonemes est 2 fois plus gd que le nb de graphemes
                                remplirLexiqueGraphemesPhonemesPosTag(motAppostropheModifiee, tPhonemes, posTag, lexique);
                                if (!dico.containsKey(motAppostropheModifiee)) {
                                    // Mot inconnu
                                    LinkedList<SuitePhonemes> ll = new LinkedList<SuitePhonemes>();
                                    ll.add(new SuitePhonemes(tPhonemes, posTag));
                                    dico.put(motAppostropheModifiee, ll);
                                } else {
                                    // Mot connu => on rajoute sa SuitePhonemes a la suite
                                    dico.get(motAppostropheModifiee).add(new SuitePhonemes(tPhonemes, posTag));
                                }
                            }
                        }
                    }
                }
                if (!motNonRejete(motAppostropheModifiee)) {
                    System.out.println("   - Mot rejete : " + mot);
                }
            }
        } catch (UnsupportedEncodingException ex) {
            System.out.println(ex.getMessage());
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("===> Ok");
    }

    private void remplirLexiqueGraphemesPhonemesPosTag(String graphemes, String[] tPhonemes, String posTag, LexiqueGraphemesPhonemesPostag lexique) {
        String[] tGraphemes = Utils.stringToArrayString(graphemes);
        for (int i = 0; i < tGraphemes.length; i++) {
            lexique.ajouterGrapheme(tGraphemes[i]);
        }
        for (int i = 0; i < tPhonemes.length; i++) {
            lexique.ajouterPhoneme(tPhonemes[i]);
        }
        lexique.ajouterPosTag(posTag);
    }

    private String[] getTableauSansSil(String str) {
        return str.replaceAll("sil", "").replaceAll("  ", " ").split(" ");
    }

    public static boolean motNonRejete(String mot) {
        return (mot.length() > 1) && (!mot.contains("!")) && (!mot.contains(System.getProperty("file.separator"))) && (!mot.contains(Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR)) && (!mot.contains(Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR)) && (!mot.contains(Configuration.STRING_DE_REMPLACEMENT_GRAPHEME_VIDE)) && (!mot.contains(" ")) && (!mot.contains("0")) && (!mot.contains("1")) && (!mot.contains("2")) && (!mot.contains("3")) && (!mot.contains("4")) && (!mot.contains("5")) && (!mot.contains("6")) && (!mot.contains("7")) && (!mot.contains("8")) && (!mot.contains("9"));
    }

    public Hashtable<String, LinkedList<SuitePhonemes>> getDico() {
        return dico;
    }
}
