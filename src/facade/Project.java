package facade;

import plugins.speechreco.aligners.sphiinx4.Alignment;
import plugins.text.ListeElement;
import plugins.text.elements.Locuteur_Info;
import plugins.text.regexp.TypeElement;

import java.util.List;

/**
 * Used for easy serialization (for now).
 * Eventually this class should become more useful.
 * TODO: centralize project methods here
 */
public class Project {
	public List<Locuteur_Info> speakers;
	public ListeElement elts;
	public String wavname;
	public String txtfile;
	public Alignment words = new Alignment();
	public Alignment phons = new Alignment();
	public List<TypeElement> types;
}