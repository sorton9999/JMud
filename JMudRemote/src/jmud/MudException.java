package jmud;


public class MudException
{
	   /**
	    * This is a generic exception class that serves as the superclass
	    * for a bunch of more specific exception types
	    **/
	   public static class MudExceptionBase extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}   
	   	
	   /**
	    * These specific exception classes are thrown in various contexts.
	    * The exception class name contains all the information about the
	    * exception; no detail messages are provided by these classes.
	    **/
	   public static class NotThere extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class AlreadyThere extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class NoSuchThing extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class NoSuchPerson extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class NoSuchExit extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class NoSuchPlace extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class ExitAlreadyExists extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class PlaceAlreadyExists extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class LinkFailed extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	   public static class BadPassword extends MudExceptionBase {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	    
	   /**
	    * This constant is used as a prefix to the MUD name when the server
	    * registers the mud with the RMI registry, and when the client looks
	    * up the MUD in the registry.  Using this prefix helps prevent the
	    * possibility of name collisions.
	    **/
	   public static final String mudPrefix = "ABC";
	   //public static final String mudPrefix = "//localhost:1099/MudServer";
	
}

