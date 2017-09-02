package jmud;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * This interface defines the exported methods of the MUD server object
 **/
public interface RemoteMudServer extends Remote {
		/** Return the name of this MUD */
		public String getMudName() throws RemoteException;
	
		/** Return the main entrance place in this MUD */
		public  RemoteMudPlace getEntrance() throws RemoteException ;
	
		/** Look up and return some other named place in this MUD */
		public RemoteMudPlace getNamedPlace(String name)
				throws RemoteException, MudException.NoSuchPlace;
		
		/**
		 * Dump the state of the server to a file so that it can be restored later
		 * All places, and their exits and things are dumped, but the "people"
		 * in them are not.
		 **/
		 public void dump(String password, String filename)
	 			throws RemoteException, MudException.BadPassword, IOException;
	}
