import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Record {
	
	private String name; /* Process p */
	private String host;
	private int    port;
	
	private boolean faulty;
	
	/* Messages awaiting to be delivered */
	private BlockingQueue<String> queue;
	
	public Record (String name, String host, int port) {
		this.name = name;
		this.host = host;
		this.port = port;
		/* By default, the process is correct */
		faulty = false;
		queue = new ArrayBlockingQueue<String>(Utils.MSG_QUEUE_SIZE);
	}
	
	public BlockingQueue<String> getQueue() {
		return queue;
	}

	public boolean isFaulty() {
		return faulty;
	}

	public void beFaulty (boolean faulty) {
		this.faulty = faulty;
	}
	
	public String getName () { return name; }
	public String getHost () { return host; }
	public int    getPort () { return port; }
	
	public String toString() {
		String s = null;
		s = String.format("[%s at %s:%d (%s)]", name, host, port, faulty);
		return s;
	}
	
	/* A simple test. */
	public static void main (String [] args) {
		Record r = new Record("P", "localhost", 1);
		System.out.println(r);
	}
}

