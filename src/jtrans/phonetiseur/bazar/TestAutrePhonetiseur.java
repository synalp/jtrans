/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.phonetiseur.bazar;

import java.io.*;
import java.util.*;

import jtrans.utils.BDLex;
import jtrans.utils.DicoEntry;
import jtrans.utils.FileUtils;
import jtrans.utils.PronunciationsLexicon;

public class TestAutrePhonetiseur {

    public static void main(String[] args) throws Exception {
        PronunciationsLexicon pl = new BDLex("/home/jean/StageLORIA/Dicos/BDLEX-V2.1/BDLex", "");

        int nbTotal = 0;
        int nbOk = 0;
        boolean ok;

        BufferedReader r = FileUtils.openFileISO("/home/jean/Bureau/motsAApprendre_TEST_iso.phon");
        String s, mot, phonetique;

        while ((s = r.readLine()) != null) {
            String[] t = s.split(",");
            nbTotal++;
            mot = t[0];
            phonetique = convertPhonemes(t[2].substring(1));

            String debut = null, fin = null;
            if (phonetique.length() >= 4) {
                debut = phonetique.substring(0, phonetique.length() - 4);
                fin = phonetique.substring(phonetique.length() - 3, phonetique.length());
            }


            String aAfficher = "";
            ArrayList<DicoEntry> al = pl.getDicoEntries(mot);
            ok = false;
            for (int i = 0; i < al.size(); i++) {
                ArrayList<String> solutions = al.get(i).getExtensionNoLiaisonsWithSwa();
                for (int j = 0; j < solutions.size(); j++) {
                    aAfficher += "/" + solutions.get(j) + "/      ";
                    boolean idemSansSwa = (phonetique.length() >= 4) ? fin.equals("swa") && debut.equals(solutions.get(j)) : false;
                    if (solutions.get(j).equals(phonetique) || idemSansSwa) {
                        ok = true;
                        nbOk++;
                        j = solutions.size();
                        i = al.size();
                    }
                }
            }
            if (!ok) {
                String sansSwa = ((phonetique.length() >= 4) && fin.equals("swa")) ? " OU /" + debut + "/" : "";
                System.out.println("====> " + mot + " -> /" + phonetique + "/" + sansSwa);
                System.out.println("Phonetisation de BDlex :\n" + aAfficher + "\n");
            }
        }

        r.close();
        System.out.println(nbOk + " / " + nbTotal + " => " + (100 * ((double) nbOk) / ((double) nbTotal)) + "%");
    }

    private static String convertPhonemes(String s) {
        s = s + " ";
        s = s.replaceAll("a~ ", "an ");
        s = s.replaceAll("E/ ", "eh ");
        s = s.replaceAll("o~ ", "on ");
        s = s.replaceAll("@ ", "swa ");
        s = s.replaceAll("O/ ", "oh ");
        s = s.replaceAll("9 ", "euf ");
        s = s.replaceAll("2 ", "eu ");
        s = s.replaceAll("e~ ", "in ");

        return s.substring(0, s.length() - 1);
    }
}
