package plugins.speechreco.grammaire;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * classe qui gère les entités nommées numériques
 * 
 * Principe = le fichier ENnumerique contient une liste d'occurrence
 * avec les prononciations possibles et les classes EN possibles
 * 
 * On parse ensuite le texte avec une distance qui permet de remplacer un chiffre
 * par un autre a moindre cout, et on estime ainsi de maniere "MBL" les prononciations
 * possibles (en adaptant la prononciation initiale) et les classes EN possibles.
 * 
 * @author cerisara
 *
 */
public class ENnum {
	enum typesnum {quantite, ordinal, date, poids, distance, temperature, heure,
		surface,
		argent, volume, vitesse, angle, pourcentage, telephone};

    ArrayList<String> exemples = new ArrayList<String>();
    ArrayList<typesnum> categories = new ArrayList<ENnum.typesnum>();
    ArrayList<List<String>> pronon = new ArrayList<List<String>>();
		
	void loadBaseDexemples() {
		InputStream is = getClass().getResourceAsStream("/ressources/ENnumerique.txt");
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			typesnum cat = null;
			for (;;) {
				String s = f.readLine().trim();
				if (s==null) break;
				if (s.length()==0) continue;
				if (s.charAt(0)=='=') {
					cat = typesnum.valueOf(s.substring(1).trim());
				} else {
					String[] ss = s.split("=");
					if (ss.length>=2) {
						exemples.add(ss[0].trim());
						categories.add(cat);
						pronon.add(Arrays.asList(ss).subList(1, ss.length));
					}
				}
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	float dist(String ex) {
		return 0;
	}
}
