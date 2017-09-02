import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import jmud.*;

/**
 * This is the class which represents a Person inhabiting the
 * MUD.  It maintains only a little bit if state, and has only
 * two exported methods.
 **/
public class MudPerson extends UnicastRemoteObject implements RemoteMudPerson {
	/**
	 * 
	 */
	private static final  long serialVersionUID = 1L;
	String name;             // The name of the person
	String description;      // The person's description
	PrintWriter tellStream;  // Where to send messages we receive
	
	public MudPerson(String n, String d, PrintWriter out)
			throws RemoteException {
		System.out.println("  >>>> MudPerson: Constructing a Person....");
		name = n;
		description = d;
		tellStream = out;
	}
	
	/** Return the person's name.  Not a remote method */
	public String getName() { return name; }
	
	/** Set the person's name.  Not a remote method */
	public void setName(String n) { name = n; }
	
	/** Set the person's description.  Not a remote method */
	public void setDescription(String d) { description = d; }
	
	/** Set the stream that messages to us should be written to.  Not remote. */
	public void setTellStream(PrintWriter out) { tellStream = out; }
	
	/** A remote method that returns this person's description */
	public String getDescription() throws RemoteException { return description; }
	
	/**
	 * Remote method that delivers a message to the person (i.e. it delivers
	 * a message to the user controlling the "person").
	 **/
	public void tell(String message) throws RemoteException {
		tellStream.println(message);
		tellStream.flush();
	}
}
