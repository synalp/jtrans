package main;

import plugins.applis.SimpleAligneur.Aligneur;

public class Main {
	public static void main(String args0[]) {
		String[] args = {"culture.jtr"};
		if (args0.length>0) args=args0;
		try {
			Aligneur.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
