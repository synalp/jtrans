package utils;

/**
 * Enumeration of the return values for WGETJava.
 * @author David Cheeseman
 *
 */
public enum WGETJavaResults {
	/**
	 * Failure to connect to the URL. 
	 */
	FAILED_IO_EXCEPTION,
	
	/**
	 * Failure to determine file type from the URL connection. 
	 */
	FAILED_UKNOWNTYPE,
	
	
	/**
	 * File downloaded sucessfully. 
	 */
	COMPLETE
}
