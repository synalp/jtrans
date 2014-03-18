package fr.loria.synalp.jtrans.markup;

public interface MarkupPlugin {

	/**
	 * Returns a friendly description for the format handled by this plugin.
	 */
	public String getFormat();


	/**
	 * Returns the most common extension for files in this format.
	 * Should be lowercase and include the initial period (e.g.: ".txt").
	 */
	public String getExt();

}
