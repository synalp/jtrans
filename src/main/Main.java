package main;

import plugins.applis.SimpleAligneur.Aligneur;

public class Main {
	public static void main() {
		String[] args = {"culture.jtr"};
		try {
			Aligneur.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
