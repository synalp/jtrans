package fr.loria.synalp.jtrans.markup;

public class ParsingException extends Exception {
	public ParsingException(String message) {
		super(message);
	}

	public ParsingException(int lineNumber, String line, String message) {
		super("Error at line " + lineNumber + ": "
				+ message + " (\"" + line + "\")");
	}
}
