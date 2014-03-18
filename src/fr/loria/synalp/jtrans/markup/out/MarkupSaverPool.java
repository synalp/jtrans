package fr.loria.synalp.jtrans.markup.out;

import fr.loria.synalp.jtrans.markup.MarkupPluginPool;

public class MarkupSaverPool extends MarkupPluginPool<MarkupSaver> {

	private static final MarkupSaverPool instance = new MarkupSaverPool();


	private MarkupSaverPool() {
		super(MarkupSaver.class, "Saver");
	}


	public static MarkupSaverPool getInstance() {
		return instance;
	}

}
