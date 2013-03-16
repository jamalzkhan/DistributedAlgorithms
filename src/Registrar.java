import java.util.concurrent.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class Registrar {
	
	public static final int pid = 0; /* P0 is reserved for this server */
	
	/* The size of the system */
	public int n;
	
	/* The "switch" table */
	private ConcurrentHashMap<String, Record> registry;
	
	/* A lock as a barrier to synchronise all clients */
	private Object lock;
	
	public Registrar(int n) {
		
		this.n = n;
		registry = new ConcurrentHashMap<String, Record>(n, 0.9f, n);
		
		/* Synchronize worker threads and, consequently, Process.registeR() */
		lock = new Object();
		
		/* Fault management */
		new Thread(new FaultManager(this)).start();
	}
	
	public Object getLock () { return lock; }
	
	private void tryNotify () {
		synchronized (lock) {
			if (areRegistered()) /* If all processes have registered, notify them */
				lock.notifyAll();
		}
	}
	
	public boolean areRegistered () { /* The condition for synchronisation */
		return (registry.size() == n);
	}
	
	/* Process registration */
	public boolean registeR (Record record) {
		String key = record.getName();
		Record result = registry.put(key, record);
		/* Synchronize */
		tryNotify ();
		return (result == null) ? true : false; 
	}
	
	public void update (Record record) {
		String key = record.getName();
		registry.put(key, record);
	}
	
	public Record find (int pid) {
		String key;
		key = String.format("P%d", pid);
		return find(key);
	}

	public Record find (String key) {
		Record record;
		record = registry.get(key);
		return record;
	}
	
	public boolean contains(String key) {
		return registry.containsKey(key);
	}
	
	private void handle (Socket client) {
		new Thread(new Worker(client, this)).start();
	}
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.err.println("usage: java Registrar [integer]");
			System.exit(1);
		}
		
		int n = Integer.parseInt(args[0]);

		Registrar server = new Registrar(n);
		Utils.out(server.pid, String.format("Registrar started; n = %d.",n));
		ServerSocket serversocket = null;
		boolean done = false;
		try {
			serversocket = new ServerSocket(Utils.REGISTRAR_PORT, n);
		
		} catch (IOException e) {
			System.err.println("Error: failure to launch registrar.");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		while (! done) {	
			Socket clientsocket = null;
			try {
				clientsocket = serversocket.accept();
			
			} catch (IOException e) {
				System.err.println("Error: failure to accept connection at Registrar.");
				System.err.println(e.getMessage());
				System.exit(1);
			}
			/* And so it begins... */
			server.handle(clientsocket);
		}
	}
}
