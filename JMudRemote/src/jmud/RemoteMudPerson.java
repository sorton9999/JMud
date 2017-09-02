package jmud;

import java.rmi.Remote;
import java.rmi.RemoteException;

	/**
	* This interface defines the methods exported by a "person" object that
	* is in the MUD
	**/
public interface RemoteMudPerson extends Remote {
	/** Return a full description of the person */
	public String getDescription() throws RemoteException;
	
	/** Deliver a message to the person */
	public void  tell(String message) throws RemoteException;
}
