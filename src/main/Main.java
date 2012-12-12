package main;

import plugins.applis.SimpleAligneur.Aligneur;

public class Main {
	public static void main() {
		String[] args = {};
		try {
			Aligneur.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
