/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur.bazar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import utils.FileUtils;

public class DecouperFichierListeDeMots {

    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        if (args.length != 3) {
            System.out.println("Il faut 3 parametres:\n" +
                    "   - Le chemin vers le dictionnaire\n" +
                    "   - le chemin vers le nouveau fichier d'apprentissage\n" +
                    "   - le chemin vers le nouveau fichier de test");
            System.exit(0);
        }
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
    }
}
