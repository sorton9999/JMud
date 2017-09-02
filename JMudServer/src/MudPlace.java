import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.io.*;
import java.util.*;
import jmud.*;

/**
 * This class implements the RemoteMudPlace interface and exports a
 * bunch of remote methods that are part of the MUD.  The
 * MudClient interacts primarily with these methods.  See the comment
 * for RemoteMudPlace for an overview.
 * The MudPlace class is Serializable so that places can be saved to disk
 * along with the MudServer that contains them.  Note, however that the
 * names and people fields are marked transient, so they are not serialized
 * along with the place (because it wouldn't make sense to try to save
 * RemoteMudPerson objects, even if they could be serialized).
 **/
public class MudPlace extends UnicastRemoteObject
											implements RemoteMudPlace, Serializable {
	String placename, description;          // information about the place itself
	Vector exits = new Vector();            // ames of exits from this place
	Vector destinations = new Vector();     // where the exits go to
	Vector things = new Vector();           // names of things in this place
	Vector descriptions = new Vector();     // descriptions of those things
	transient Vector names = new Vector();  // names of people in this place
	transient Vector people = new Vector(); // RemoteMudPerson objects
	MudServer server;                       // server for this place
	
	/** A no-arg constructor for de-serialization only.  Do not call!! */
	public MudPlace() throws RemoteException { super(); }
	
	/**
	 * Constructor creates a place, and calls a server method to
	 * register the object so that it will be accessible by name
	 **/
	public MudPlace(MudServer server, String placename, String description)
			throws RemoteException, MudException.PlaceAlreadyExists {
		System.out.println(" >>>> MudPlace: Constructing a place....");
		this.server = server;
		this.placename = placename;
		this.description = description;
		server.setPlaceName(this, placename);  // Register the place
	}
	
	/** Remote method to return the name of the place */
	public String getPlaceName() throws RemoteException { return placename; }
	
	/** Remote method to return the description of this place */
	public String getDescription() throws RemoteException { return description; }
	
	/** Remote method to return a vector of names of people in this place */
	public Vector getNames() throws RemoteException { return names; }
	
	/** Remote method to return a vector of names of things */
	public Vector getThings() throws RemoteException { return things; }
	
	/** Remote method to return a vector of names of exits from this place */
	public Vector getExits() throws RemoteException { return exits; }
	
	/**
	 * Remote method to return a RemoteMudPerson object corresponding to
	 * the specified name, or throws an exception if no such person is here.
	 **/
	public RemoteMudPerson getPerson(String name)
			throws RemoteException, MudException.NoSuchPerson {
		synchronized(names) {
			// what about when there are 2 of the same name?
			int i = names.indexOf(name);
			if (i == -1) throw new MudException.NoSuchPerson();
			return (RemoteMudPerson) people.elementAt(i);
		}
	}
	
	/**
	 * Remote method returns a description of the named thing, or 
	 * throws an exception if no such thing exists.
	 **/
	public String examineThing(String name) throws RemoteException, MudException.NoSuchThing {
		synchronized(things) {
			int i = things.indexOf(name);
			if (i == -1) throw new MudException.NoSuchThing();
			return (String) descriptions.elementAt(i);
		}
	}
	
	/**
	 * Remote method moves the specified RemoteMudPerson from this place in
	 * the named direction (i.e. through the named exit) to whatever place
	 * is there.  It throws exceptions if the specified person isn't in this
	 * place to begin with, or if they are already in the place through the exit
	 * or if the exit doesn't exist, or if the exit links to another MUD server
	 * and the server isn't functioning
	 **/
	public RemoteMudPlace go(RemoteMudPerson who, String direction)
			throws RemoteException, MudException.NotThere, MudException.AlreadyThere, MudException.NoSuchExit, MudException.LinkFailed {
		//make sure the direction is valid, and get destination if it is
		Object destination;
		synchronized(exits) {
			int i = exits.indexOf(direction);
			if (i == -1) throw new MudException.NoSuchExit();
			destination = destinations.elementAt(i);
		}
		
		// If destination is a string, it is a place on another server, so connect
		// to that server.  Otherwise, it is a place already on this server.
		// Throw an exception if we cannot connect to the other server.
		RemoteMudPlace newplace;
		if (destination instanceof String) {
			try {
				String t = (String) destination;
				int pos = t.indexOf('@');
				String url = t.substring(0, pos);
				String placename = t.substring(pos+1);
				RemoteMudServer s = (RemoteMudServer) Naming.lookup(url);
				newplace = s.getNamedPlace(placename);
			}
			catch (Exception e) { throw new MudException.LinkFailed(); }
		}
		// If the destination is not a string, then it is a local Place
		else newplace = (RemoteMudPlace) destination;
		
		// Make sure the person is here and get their name.
		// Throw an exception if they are not here.
		String name = verifyPresence(who);
		
		// Move the person out of here, and tell everyone present about it
		this.exit(who, name + " has gone " + direction);
		
		// Put the person in the new Place.
		// Send a message to everyone present in the new place.
		String fromwhere;
		if (newplace instanceof MudPlace)  // going to a local place
			fromwhere = placename;
		else
			fromwhere = server.getMudName() + "." + placename;
		newplace.enter(who, name, name + " has arrived from: " + fromwhere);
		
		// Return the new RemoteMudPlace object to the client so they
		// know where they are now at.
		return newplace;
	}
	
	/**
	 * Remote method sends a message to everyone in the room.  Used to
	 * say things to everyone.  Requires that the speaker be in this place.
	 **/
	public void speak(RemoteMudPerson speaker, String msg)
			throws RemoteException, MudException.NotThere {
		String name = verifyPresence(speaker);
		tellEveryone(name + ": " + msg);
	}
	
	/**
	 * Remote method sends a messagfe to everyone in the room.  Used to
	 * do things that people can see.  Requires that the actor be in this place.
	 **/
	public void act(RemoteMudPerson actor,  String msg)
			throws RemoteException, MudException.NotThere {
		String name = verifyPresence(actor);
		tellEveryone(name + " " + msg);
	}
	
	/**
	 * Remote method creates a new thing in this room.
	 * It requires that the creator be in this room.
	 **/
	public void createThing(RemoteMudPerson creator, String name,
													String description)
			throws RemoteException, MudException.NotThere, MudException.AlreadyThere {
		// Make sure the creator is here
		String creatorname = verifyPresence(creator);
		synchronized(things) {
			// Make sure there isn't already something with this name
			if (things.indexOf(name) != -1) throw new MudException.AlreadyThere();
			// Add the thing name and descriptions to the appropriate lists
			things.addElement(name);
			descriptions.addElement(description);
		}
		// Tell everyonr about the new thing and its creator
		tellEveryone(creatorname + " has created a " + name);
	}
	
	/**
	 * Remove a thing from this room.  Throws exceptions if the person
	 * who does the removing isn't in the room, or if there is no
	 * such thing there.
	 **/
	public void destroyThing(RemoteMudPerson destroyer, String thing)
			throws RemoteException, MudException.NotThere, MudException.NoSuchThing {
		// Verify the destroyer is here
		String name = verifyPresence(destroyer);
		synchronized(things) {
			// Verify that there is a thing by that name in this room.
			int i = things.indexOf(thing);
			if (i == -1) throw new MudException.NoSuchThing();
			// And remove its name and description from their lists
			things.removeElementAt(i);
			descriptions.removeElementAt(i);	
		}
		// Let everyone know of the demise of this thing.
		tellEveryone(name + " had destroyed the " + thing);
	}
	
	/**
	 * Create a new place in this MUD with the specified name and description.
	 * The new place is accessible from this place through the specified
	 * exit, and this place is accessible from the new place through the
	 * specified entrance.  The creator must be in this place in order to
	 * create an aexit from this place.
	 **/
	public void createPlace(RemoteMudPerson creator, String exit, String entrance,
	                        String name, String description)
			throws RemoteException, MudException.NotThere, MudException.ExitAlreadyExists, MudException.PlaceAlreadyExists {
		// Verify the creator is actually in this place
		String creatorname = verifyPresence(creator);
		synchronized(exits) {  // Only allow one client to change exits at a time
			// Check that the exit doesn't already exist
			if (exits.indexOf(exit) != -1) throw new MudException.ExitAlreadyExists();
			// Create the new place, registering its name with the server
			MudPlace destination = new MudPlace(server, name, description);
			// Link from there back to here
			destination.exits.addElement(entrance);
			destination.destinations.addElement(this);
			// And link from here to there
			exits.addElement(exit);
			destinations.addElement(destination);
		}
		// Let everyone know about the new exit, and the new place beyond
		tellEveryone(creatorname + " has created a new place: " + exit);
	}
	
	/**
	 * Create a new exit from this MUD, linked to a named place in a named
	 * MUD on a named host (this can also be used to link to a named place in
	 * the current MUD, of cource).  Because of the possibilities of deadlock,
	 * this method only links from here to there; it does not create a return
	 * exit from there to here.  That must be done with a separate call.
	 **/
	public void linkTo(RemoteMudPerson linker, String exit,
										 String hostname, String mudname, String placename)
			throws RemoteException, MudException.NotThere, MudException.ExitAlreadyExists, MudException.NoSuchPlace {
		// Verify linker is actually here
		String name = verifyPresence(linker);
		
		// Check that the link target actually exists.  Throw NoSuchPlace if not.
		// Note that NoSuchPlace may also mean "NoSuchMud" or "MudNotResponding".
		String url = "rmi://" + hostname + '/' + MudException.mudPrefix + mudname;
		try {
			RemoteMudServer s = (RemoteMudServer) Naming.lookup(url);
			RemoteMudPlace destination = s.getNamedPlace(placename);
		}
		catch (Exception e) { throw new MudException.NoSuchPlace(); }
		
		synchronized(exits)  {
			// Check that the exit doesn't already exist
			if (exits.indexOf(exit) != -1) throw new MudException.ExitAlreadyExists();
			// Add the exit to the list of exit names
			exits.addElement(exit);
			// And add the destination to the list of destinations.  Note that
			// the destination is stored as a string rather than as a RemoteMudPlace.
			// This is because if the remote server goes down then comes back up
			// again, a RemoteMudPlace is not valid, but the string still is.
			destinations.addElement(url + '@' + placename);
		}
		// Let everyone know about the new exit and where it leads
		tellEveryone(name + " had linked " + exit + " to " +
								 "'" + placename + "' in MUD '" + mudname +
								 "' on host " + hostname);
	}
	
	/**
	 * Close an exit that leads out of this place.  It does not close
	 * the return exit from there back to here.  Note that this method
	 * does not destroy the place that the exit leads to.  In the
	 * current implementation, there is no way to destroy a place.
	 **/
	public void close(RemoteMudPerson who, String exit)
			throws RemoteException, MudException.NotThere, MudException.NoSuchExit {
		// Check that the person closing the exit is actually here.
		String name = verifyPresence(who);
		synchronized(exits) {
			// Check that the exit exists.
			int i = exits.indexOf(exit);
			if (i == -1) throw new MudException.NoSuchExit();
			// Remove it and its associated destination.
			exits.removeElementAt(i);
			destinations.removeElementAt(i);
		}
		// Let everyone know that the exit doesn't exist anymore
		tellEveryone(name + " has closed exit " + exit);
	}
	
	/**
	 * Remove a person from this place.  If there is a message, send it to
	 * everyone who is left in this place.  If the specified person is not
	 * here, this method does nothing and doesn't throw an exception.  This
	 * method is called by go(), and the client should call it when the user
	 * quits.  The client should not allow the user to invoke it directly.
	 **/
	public void exit(RemoteMudPerson who, String message) throws RemoteException {
		String name;
		synchronized(names) {
			int i = people.indexOf(who);
			if (i == -1) return;
			name = (String) names.elementAt(i);
			names.removeElementAt(i);
			people.removeElementAt(i);
		}
		if (message != null) tellEveryone(name + ": " + message);
	}
	
	/**
	 * Puts a person into this place, assigning them the specified name and
	 * displaying a message to anyone else who is in that place.  This method
	 * is called by go(), and the client should call it to initially place a
	 * person into the MUD.  Once the person is in the MUD, however, the client 
	 * should restrict them to using go() and should not allow them to use
	 * this method directly.  If there have been network problems. a client
	 * might call this method to restore a person to this place, in case they've
	 * been bumped out (A person will be bumped out of a place if the server
	 * tries to send a message to them and gets a RemoteException).
	 **/
	public void enter(RemoteMudPerson who, String name, String message)
			throws RemoteException, MudException.AlreadyThere {
		// Send the message to everyone in this place.
		if (message != null) tellEveryone(name + ": " + message);
		
		// Add person to this place
		synchronized(names) {
			if (people.indexOf(who) != -1) throw new MudException.AlreadyThere();
			names.addElement(name);
			people.addElement(who);
		}
	}
	
	/**
	 * This final	remote method returns the server object for the MUD 
	 * in which this place exists.  The client should not allow the
	 * user to invoke this method directly.
	 **/
	public RemoteMudServer getServer() throws RemoteException { return server; }
	
	/**
	 * Create and start a thread that sends out a message to everyone
	 * in this place.  If it gets a RemoteException talking to a person.
	 * it silently removes that person from this place.  This is not
	 * a remote method, but is used internally by a number of remote
	 * methods.
	 **/
	protected void tellEveryone(final String message) {
		// If there is no-one here, don't bother sending the message!
		if (people.size() == 0) return;
		// Make a copy of the people here now.  The message is sent asynchronously
		// and the list of people in the room may change before the message is
		// sent to everyone.
		final Vector recipients = (Vector) people.clone();
		// Create and start a thread to send the message, using  an anonymous
		// class.  We do this because sending the message to everyone in this
		// place might take some time, (particularly on a slow or flaky network)
		// and we don't want to wait.
		new Thread() {
			public void run() {
				// Loop through the recipients
				for (int i=0; i<recipients.size(); i++) {
					RemoteMudPerson person = (RemoteMudPerson) recipients.elementAt(i);
					// Try to send the message to each one
					try { person.tell(message); }
					// If it fails, assume that that person's client or network has
					// failed, and silently remove them from this place.
					catch (RemoteException e) {
						try { MudPlace.this.exit(person, null); }
						catch (Exception ex) {}
					}
				}
			}
		}.start();
	}
		
	/**
	 * Convenience method which checks whether the specified person is
	 * here.  If so, it returns their name.  If not, it throws a 
	 * NotThere exception.
	 **/
	protected String verifyPresence(RemoteMudPerson who) throws MudException.NotThere {
		int i = people.indexOf(who);
		if (i == -1) throw new MudException.NotThere();
		else return (String) names.elementAt(i);
	}
		
	/**
	 * Used for custom de-serialization.  Since the vectors of
	 * people and of their names are transient, they are not serialized
	 * with the rest of this place.  Therefore, when the place is 
	 * de-serialized, those vectors have to be recreated (empty).
	 **/
	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();   // Read most of the object as normal
		names = new Vector();     // Then recreate the names vector
		people = new Vector();    // and recreate the people vector.
	}
		
	/** This constant is a version number for serialization */
	static final long serialVersionUID = 5090967989223703026L;
}
