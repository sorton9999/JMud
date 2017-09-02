package jmud;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;


/**
 * This is the most important remote interface for the MUD.  It defines the
 * methods exported by the "places" or "rooms" within the MUD.  Each place
 * has a name and a description, and also maintains a list of "people" in
 * the place, things in the place, and exits from the place.  There are
 * methods to get the RemoteMudPerson object for a named person, to get a
 * description of a named thing, and to go through a named exit.
 * There are methods for interacting with other people in the MUD.
 * There are methods for building the MUD by creating and destroying
 * things, adding new places (and new exits to those places), for linking
 * a place through a new exit to some other place (possibly on another
 * MUD server), and for closing down an existing exit.
 **/
 public interface RemoteMudPlace extends Remote {
 	/** Look up the name of this place */
 	public String getPlaceName() throws RemoteException;
 	
 	/** Get a description of this place */
 	public String getDescription() throws RemoteException;
 	
 	/** Find out the names of all people here */
 	public Vector getNames() throws RemoteException;
 	
 	/** Get names of all things here */
 	public Vector getThings() throws RemoteException;
 	
 	/** Get the names of all ways out of here */
 	public Vector getExits() throws RemoteException;
 	
 	/** Get the RemoteMudPerson object for the named person */
 	public RemoteMudPerson getPerson(String name)
 			throws RemoteException, MudException.NoSuchPerson;
 		
 	/** Get more details about a named thing */
 	public String  examineThing(String name) 
 			throws RemoteException, MudException.NoSuchThing;
 	
 	/** Use the named exit */
 	public RemoteMudPlace go(RemoteMudPerson who, String direction)
 			throws RemoteException, MudException.NotThere, MudException.AlreadyThere, MudException.NoSuchExit, MudException.LinkFailed;
 		
 	/** Send a message of the form "David: hi everyone" */
 	public void speak(RemoteMudPerson speaker, String msg)
 			throws RemoteException, MudException.NotThere;
 		
 	/** Send a message of the form "David laughs loudly" */
 	public void act(RemoteMudPerson speaker, String msg)
 			throws RemoteException, MudException.NotThere;
 		
 	/** Add a new thing in this place */
 	public void createThing(RemoteMudPerson who, String name,
 													String description)
 			throws RemoteException, MudException.NotThere, MudException.AlreadyThere;
 		
 	/** Remove a thing from this place */
 	public void destroyThing(RemoteMudPerson who, String thing)
 			throws RemoteException, MudException.NotThere, MudException.NoSuchThing;
 		
 	/** Create a new place, bi-directionally linked to this one by an exit */
 	public void createPlace(RemoteMudPerson creator,
 							String exit, String entrance,
 							String name, String description)
 			throws RemoteException, MudException.NotThere, MudException.ExitAlreadyExists, MudException.PlaceAlreadyExists;
 		
 	/** 
 	 * Link this place (unidirectionally) to some existing place.  The
 	 * destination place may even be on another server.
 	 **/
 	 public void linkTo(RemoteMudPerson who, String exit,
 	 					String hostname, String mudname, String placename)
 	 		throws RemoteException, MudException.NotThere, MudException.ExitAlreadyExists, MudException.NoSuchPlace;
 	 	
 	/** Remove an existing exit */
 	public void close(RemoteMudPerson who, String exit)
 	 		throws RemoteException, MudException.NotThere, MudException.NoSuchExit;
 	 	
 	/**
 	 * Remove this person from this place, leaving them nowhere.
 	 * Send the specified message to everyone left in the place
 	 **/
 	 public void exit(RemoteMudPerson who, String message)
 	  		throws RemoteException, MudException.NotThere;
 	  	
 	/**
 	 * Put a person in a place, assigning their name, and sending the
 	 * specified message to everyone else in the place.  The client should
 	 * not make this method available to the user.  They should use go()
 	 * instead.
 	 **/
 	 public void enter(RemoteMudPerson who, String name, String message)
 	   		throws RemoteException, MudException.AlreadyThere;
 	   	
 	/**
 	 * Return the server object of the MUD that "contains" this place
 	 * This method should not be directly visible to the player.
 	 **/
 	 public RemoteMudServer getServer() throws RemoteException;
 }
 
