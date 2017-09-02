import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.io.*;
import java.util.*;
import jmud.*;

/**
 * Client class for the MUD
 **/
public class MudClient {
	
	/**
	 * The main program.  It expects two or three arguments:
	 *   0) the name of the host on which the mud server is running
	 *   1) the name of the MUD on that host
	 *   2) the name of a place w/in that MUD to start at (optional).
	 *
	 * It uses the Naming.lookup() method to obtain a RemoteMudServer object
	 * for the named MUD on the specific host.  Then it uses the getEntrance()
	 * or getNamedPlace() method of RemoteMudServer to obtain the starting
	 * RemoteMudPlace object.  It prompts the user for their name and
	 * description and creates a MudPerson object.  Finally, it passes
	 * the person and the place to runMud() to begin interaction with the MUD.
	 **/
	public static void main(String[] args) {
		try {
            boolean use_gui = false;   // Use a GUI front-end instead of text-only
			String hostname = null;    // Each MUD is uniquely identified by a
			String mudname = null;     //  host and MUD name.
			String placename = null;   // Each place w/in a MUD has a unique name
            int arg_len = args.length;
            //MudClientShow client = null;
            
            for (int i=0; i<arg_len; ++i) {
                if (args[i].compareTo("-g") == 0)
                    use_gui = true;
                else if (args[i].compareTo("-h") == 0)
                    hostname = args[(i+1)];
                else if (args[i].compareTo("-m") == 0)
                    mudname = args[(i+1)];
                else if (args[i].compareTo("-p") == 0)
                    placename = args[(i+1)];
            }
			// Set the RMI security manager so that untrusted stub objects loaded
			// over the network can't cause havoc.
			System.setSecurityManager(new RMISecurityManager());
		
			// Look up the RemoteMudServer object for the named MUD using
			// the default registry on the specified host.  Note the use of
			// the Mud.mudPrefix constant to help prevent nameing conflicts
			// in the registry.
			RemoteMudServer server =
				(RemoteMudServer) Naming.lookup("rmi://" + hostname + "/" +
												MudException.mudPrefix + "." + mudname);
																				
			// If the user did not specify a place in the mud, use getEntrance()
			// to get the initial place.  Otherwise, call getNamedPlace() to
			// find the initial place.
			RemoteMudPlace location = null;
			if (placename == null) location = server.getEntrance();
			else location = (RemoteMudPlace) server.getNamedPlace(placename);
		
			// Greet the user and ask for their name and description.
			// This relies on getLine() and getMultiLine() defined below.
			System.out.println("Welcome to " + mudname);
			String name = getLine("Enter your name: ");
			String description = getMultiLine("Please describe what " +
											  "people see when they look at you: ");
																	    
			// Define an output stream that the MudPerson object will use to
			// display messages sent to the user.  We'll use the console.
            PrintWriter myout = new PrintWriter(new OutputStreamWriter(System.out));
		
			// Create a MudPerson object to represent the user in the MUD.
			// Use the specified name and description and the output stream.
			MudPerson me = new MudPerson(name, description, myout);
		
            //if (use_gui)
            //    client = new MudClientShow(server, location, me);
                
			// Lower this thread's priority one notch so that broadcast messages
			// can appear even when we're blocking I/O.  This is necessary
			// on the Linux platform, but may not be necessary on all platforms.
			int pri = Thread.currentThread().getPriority();
			Thread.currentThread().setPriority(pri-1);
		
			// Finally, put the MudPerson into the RemoteMudPlace, and start
			// prompting the user for commands.
            //if (use_gui)
            //    client.runMud();
            //else
                runMud(location, me);
		}
		// If anything goes wrong, print a message and exit
		catch(Exception e) {
			System.out.println(e);
			System.out.println("Usage: java MudClient -g -h <host> -m <mud> [-p <place>]");
			System.exit(1);
		}
	}
	
	/**
	 * The main loop of the MudClient.  It places the person into the 
	 * place (using the enter() method of RemoteMudPlace).  Then it
	 * calls the look() method to describe the place to the user and
	 * enters a command loop to prompt the user for a command and
	 * process the command.
	 **/
	public static void runMud(RemoteMudPlace entrance, MudPerson me)
			throws RemoteException {
		RemoteMudPlace location = entrance;  // The current place
		String myname = me.getName();        // The person's name
		String placename = null;             // The name of the current place
		String mudname = null;               // The name of the mud of that place
		
		System.out.println(" * Client start......\n");
		try {
			// Enter the MUD
			location.enter(me, myname, myname + " has entered the MUD.");
			// Figure out where we are (for the prompt)
			mudname = location.getServer().getMudName();
			placename = location.getPlaceName();
			// Describe the place to the user
			look(location);
		}
		catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
		
		// Now that we've entered the MUD, begin a command loop to process
		// the user's commands.  Note that there is a huge block of catch
		// statements at the bottom of the loop to handle all the things that
		// could go wrong each time through.
		for (;;) {     // Loop until the user types "quit"
			try {    // catch any exceptions that occur in the loop
				// pause just a bit before printing the prompt, to give output
				// generated indirectly by the last command a chance to appear.
				try { Thread.sleep(200); } catch (InterruptedException e) {}
				
				// Display a prompt and get the user's input
				String line = getLine(mudname + '.' + placename + "> ");
				
				// Break the input into command and an argument that consists
				// of the rest of the line.  Convert the command to lowercase.
				String cmd, arg;
				int i = line.indexOf(' ');
				if (i == -1) { cmd = line; arg = null; }
				else {
					cmd = line.substring(0, i).toLowerCase();
					arg = line.substring(i+1);
				}
				if (arg == null) arg = "";
				
				// Now go process the command.  Many of which invoke one of the
				// remote methods of the current RemoteMudPlace object.
				
				// LOOK: Describe the place and its things, people and exits
				if (cmd.equals("look")) look(location);
				// EXAMINE: Describe a named thing
				else if (cmd.equals("examine"))
					System.out.println(location.examineThing(arg));
				// DESCRIBE: Describe a named person
				else if (cmd.equals("describe")) {
					try {
						RemoteMudPerson p = location.getPerson(arg);
						System.out.println(p.getDescription());
					}
					catch (RemoteException e) {
						System.out.println(arg + " is having technical difficulties. " +
															 "No description is available.");
					}
				}
				// GO: Go in a named direction
				else if (cmd.equals("go")) {
					location = location.go(me, arg);
					mudname = location.getServer().getMudName();
					placename = location.getPlaceName();
					look(location);
				}
				// SAY: Say something to everyone
				else if (cmd.equals("say")) location.speak(me, arg);
				// DO: Do something that will be described to everyone
				else if (cmd.equals("do")) location.act(me, arg);
				// TALK: Say something to one named person
				else if (cmd.equals("talk")) {
					try {
						RemoteMudPerson p = location.getPerson(arg);
						String msg = getLine("What do you want to say?: ");
						p.tell(myname + " says \"" + msg + "\"");
					}
					catch (RemoteException e) {
						System.out.println(arg + " is having technical difficulties. " +
															 "Can't talk to named person.");
				 	}
				}
				// CHANGE: Change my own description
				else if (cmd.equals("change"))
					me.setDescription(getMultiLine("Describe yourself for others: "));
				// CREATE: Create a new thing in this place
				else if (cmd.equals("create")) {
					if (arg.length() == 0)
						throw new IllegalArgumentException("name expected");
					String desc = getMultiLine("Please describe the " + arg + ": ");
					location.createThing(me, arg, desc);
				}
				// DESTROY: Destroy the named thing
				else if (cmd.equals("destroy")) location.destroyThing(me, arg);
				// OPEN: Create a new place and connect this place to it through
				// the exit specified in the argument.
				else if (cmd.equals("open")) {
					if (arg.length() == 0)
						throw new IllegalArgumentException("direction expected");
					String name = getLine("What is the name of the new place?: ");
					String back = getLine("What is the direction from " +
																"there bach to here?: ");
					String desc = getMultiLine("Please describe " + name + ": ");
					location.createPlace(me, arg, back, name, desc);
				}
				// CLOSE: Close a named exit.  Note: only closes an exit
				// uni-directionally, and does not destroy any places.
				else if (cmd.equals("close")) {
					if (arg.length() == 0)
						throw new IllegalArgumentException("direction expected");
					location.close(me, arg);
				}
				// LINK: Create a new exit that connects to an existing place
				// that may be in another MUD running on another host.
				else if (cmd.equals("link")) {
					if (arg.length() == 0)
						throw new IllegalArgumentException("direction expected");
					String host = getLine("What host are you linking to?: ");
					String mud = getLine("What is the name of the MUD on that host?: ");
					String place = getLine("What is the place name in that MUD?: ");
					location.linkTo(me, arg, host, mud, place);
					System.out.println("Don't forget to make a link from there " +
														 "back to here !!");
				}
				// DUMP: Save the state of this MUD into the named file, if 
				// the password is correct.
				else if (cmd.equals("dump")) {
					if (arg.length() == 0)
						throw new IllegalArgumentException("filename expected");
					String password = getLine("Password: ");
					location.getServer().dump(password, arg);
				}
				// QUIT: Quit the game
				else if (cmd.equals("quit")) {
					try { location.exit(me, myname + " has left."); }
					catch (Exception e) {}
					System.out.println("..... Bye .....");
					System.out.flush();
					System.exit(0);
				}
				// HELP: Print out a big help message
				else if (cmd.equals("help")) {
					String help =
						"Commands are:\n" +
						"look: Look around\n" +
						"examine <thing>: Examine the named thing in more detail\n" +
						"describe <person>: Describe the named person\n" +
						"go <direction>: Go in the named deirection (i.e. a named exit)\n" +
						"say <message>: Say something to everyone\n" +
						"do <message>: Tell everyone that you are doing something\n" +
						"talk <person>: Talk to the named person. Will prompt for message\n" +
						"change: Change how you are desribed. Will prompt for input\n" +
						"create <thing>: Create a new thing. Prompts for description\n" +
						"destroy <thing>: Destroy a thing\n" +
						"open <direction>: Create an adjoining place. Prompts for input\n" +
						"close <direction>: Close an exit from this place\n" +
						"link <direction>: Create an exit to an existing place,\n" +
						"\tperhaps on another server. Will prompt for input\n" +
						"dump <filename>: Save server state. Prompts for password\n" +
						"quit: Leave the MUD\n" + 
						"help: Display this message";
					System.out.println(help);
				}
				// Otherwise, this is an unrecognized command
				else System.out.println("Unknown command. Try 'help'.");
			}
			// Handle the many possible types of MudException
			catch (MudException.MudExceptionBase e) {
				if (e instanceof MudException.NoSuchThing)
					System.out.println("There isn't any such thing here.");
				else if (e instanceof MudException.NoSuchPerson)
					System.out.println("There isn't anyone by that name here.");
				else if (e instanceof MudException.NoSuchExit)
					System.out.println("There isn't an exit in that direction.");
				else if (e instanceof MudException.NoSuchPlace)
					System.out.println("There isn't any such place.");
				else if (e instanceof MudException.ExitAlreadyExists)
					System.out.println("There is already an exit in that direction.");
				else if (e instanceof MudException.PlaceAlreadyExists)
					System.out.println("There is already a place with that name.");
				else if (e instanceof MudException.LinkFailed)
					System.out.println("That exit is not functioning.");
				else if (e instanceof MudException.BadPassword)
					System.out.println("Invalid Password.");
				else if (e instanceof MudException.NotThere)      // shouldn't happen
					System.out.println("You can't do that when you're not there.");
				else if (e instanceof MudException.AlreadyThere)  // shouldn't happen
					System.out.println("You can't go there; you're already there.");
			}
			// Handle RMI exceptions
			catch (RemoteException e) {
				System.out.println("The MUD is having technical difficulties.");
				System.out.println("Perhaps the server has crashed.");
				System.out.println(e);
			}
			// Handle everything else that can go wrong.
			catch (Exception e) {
				System.out.println("Syntax or other error:");
				System.out.println(e);
				System.out.println("Try using the 'help' command.");
			}
		}
	}
	
	/**
	 * Convenience method used in several places in the runMud() method 
	 * above.  It displays the name and description of the current place
	 * (including the name of the mud the place is in), and also displays
	 * the list of things, people, and exits in the current place.
	 **/
	public static void look(RemoteMudPlace p) 
			throws RemoteException, MudException.MudExceptionBase {
		String mudname = p.getServer().getMudName();  // Mud name
		String placename = p.getPlaceName();          // Place name
		String description = p.getDescription();      // Place description
		Vector things = p.getThings();                // List of things here
		Vector names = p.getNames();                  // List of people here
		Vector exits = p.getExits();                  // List of exits from here
		
		// Print it all out
		System.out.println("You are in: " + placename + " of the Mud: " + mudname);
		System.out.println(description);
		System.out.print("Things here: ");
		for (int i=0; i<things.size(); i++) {     // Display list of things.
			if (i > 0) System.out.print(", ");
			System.out.print(things.elementAt(i));
		}
		System.out.print("\nPeople here: ");      
		for (int i=0; i<names.size(); i++) {      // Display list of people.
			if (i > 0) System.out.print(", ");
			System.out.print(names.elementAt(i));
		}
		System.out.print("\nExits here: ");
		for (int i=0; i<exits.size(); i++) {      // Display list of exits
			if (i > 0) System.out.print(", ");
			System.out.print(exits.elementAt(i));
		}
		System.out.println();                     // Blank line
		System.out.flush();                       // Make it appear now.
	}
	
	/** Reads lines from the console */
	static BufferedReader in = 
		new BufferedReader(new InputStreamReader(System.in));
		
	/**
	 * Convenience method for prompting the user and getting a line of input.
	 * It guarantees that the line is not empty and strips off whitespace
	 * at the beginning and end of the line.
	 **/
	public static String getLine(String prompt) {
		String line = null;
		do {                   // Loop until a non-empty line is entered
			try {
				System.out.print(prompt);             // display prompt
				System.out.flush();                   // display right away
				line = in.readLine();                 // get a line of input
				if (line != null) line = line.trim(); // strip off whitespace
			} catch (Exception e) {}                // ignore any errors
		} while ((line == null) || (line.length() == 0));
		return line;
	}
	
	/**
	 * Convenience method for getting multi-line input from the user.
	 * It prompts for the input, displays instructions, and guarantees
	 * that the input is not empty.  It also allows the user to enter the
	 * name of a file from which text will be read.
	 **/
	public static String getMultiLine(String prompt) {
		String text = "";
		for (;;) {    // We'll break out of this loop when we get non-empty input.
			try {
				BufferedReader br = in;     // The stream to read from.
				System.out.println(prompt); // Display the prompt
				// Display some instructions
				System.out.println("You can enter multiple lines. " +
													 "End with a '.' on a line by itself.\n" +
													 "Or enter a '<<' followed by a filename");
				// Make the prompt and instructions appear now.
				System.out.flush();
				// Read lines
				String line;
				while ((line = br.readLine()) != null) {  // Until EOF
					if (line.equals(".")) break;            // or until a dot by itself
					// Or, if a file is specified, start reading from it instead of
					// from the console.
					if (line.trim().startsWith("<<")) {
						String filename = line.trim().substring(2).trim();
						br = new BufferedReader(new FileReader(filename));
						continue;    // Don't count the << line as part of the input.
					}
					else text += line + "\n";   // Add the line to the collected input
				}
				// If we got at least one line, return it.  Otherwise, chastise the
				// user and go back to the prompt and the instructions.
				if (text.length() > 0) return text;
				else System.out.println("Please enter at least one line.");
			}
			// If there were errors, for example an IO error reading a file,
			// display the error and loop again, displaying prompt and instructions.
			catch (Exception e) { System.out.println(e); }
		}
	}
}
