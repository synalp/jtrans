/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur.bazar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import plugins.utils.FileUtils;

public class TransformerDicoNomsPropresAuFormatBDLex {

    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        if (args.length != 2) {
            System.out.println("Il faut 2 parametres:\n" +
                    "   - Le chemin vers le fichier *.bdlex\n" +
                    "   - Le chemin vers le repertoire cible");
            System.exit(0);
        }

        String repCible, s;

        repCible = args[1];
        if (repCible.charAt(repCible.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
            repCible += System.getProperty("file.separator");
        }

        BufferedReader r = FileUtils.openFileISO(args[0]);
        HashMap<Character, LinkedList<String>> hm = new HashMap<Character, LinkedList<String>>();
        char k;
        while ((s = r.readLine()) != null) {
            k = Character.toLowerCase(s.charAt(0));
            if ((k == 'â') || (k == 'à') || (k == 'ä')) {
                k = 'a';
            } else if ((k == 'ê') || (k == 'é') || (k == 'è') || (k == 'ë')) {
                k = 'e';
            } else if ((k == 'î') || (k == 'ï')) {
                k = 'i';
            } else if ((k == 'ô') || (k == 'ö')) {
                k = 'o';
            } else if ((k == 'û') || (k == 'ù') || (k == 'ü')) {
                k = 'u';
            } else if (k == 'ç') {
                k = 'c';
            }
            if (!hm.containsKey(k)) {
                hm.put(k, new LinkedList<String>());
            }
            hm.get(k).push(s);
        }
        r.close();

        PrintWriter wAppostropheALaFin = FileUtils.writeFileUTF(repCible + "apostrophe.B.flx");
        PrintWriter wTraitUnionDebut = FileUtils.writeFileUTF(repCible + "clitic.B.flx");
        for (char key : hm.keySet()) {
            String sLigne;
            PrintWriter w = FileUtils.writeFileUTF(repCible + key + ".B.flx");
            while (hm.get(key).size() > 0) {
                sLigne = hm.get(key).pop();
                if (sLigne.substring(sLigne.length() - 1, sLigne.length()).equals("'")) {
                    wAppostropheALaFin.write(sLigne);
                } else if (sLigne.substring(0, 1).equals("-")) {
                    wTraitUnionDebut.write(sLigne);
                } else {
                    w.println(sLigne);
                }
            }
            w.close();
        }
        wAppostropheALaFin.close();
        wTraitUnionDebut.close();



    /*
    BufferedReader r = FileUtils.openFileUTF(args[0]);
    PrintWriter wApprentissage = FileUtils.writeFileUTF(args[1]);
    PrintWriter wTest = FileUtils.writeFileUTF(args[2]);
    String ligne;
    int compt = -1;

    while ((ligne = r.readLine()) != null) {
    compt = (compt + 1) % 10;
    if (compt == 1) {
    wTest.write(ligne + "\n");
    } else {
    wApprentissage.write(ligne + "\n");
    }
    }

    r.close();
    wApprentissage.close();
    wTest.close();
     */
    }
}
