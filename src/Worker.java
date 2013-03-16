import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Worker extends Thread {

	private Socket s;
	private Registrar r;
	private int myprocess;
	
	/* Random delay generator */
	Random random;
	private static final double mean = (double) Utils.DELAY;
	private static final double stdv = mean / 2.0;
	
	/* Throughput measurements */
	private long count;
	private long _t_recv;

	public Worker(Socket s, Registrar r) {
		this.s = s;
		this.r = r;

		myprocess = Utils.INFINITY;
		
		random = new Random();
		
		count = 0;
	}
	
	private void unicast (Message m, int delay) {
		
		int src = m.getSource();
		int dst = m.getDestination();
		
		Record source = r.find (src);
		Record destination = r.find (dst);
		
		if ((source == null) || (destination == null)) {
			/* In this unlikely event. */
			String msg =
				String.format("Error: link <P%d, P%d> does not exist.", 
			src, dst);
			System.err.println(msg);
			System.exit(1);
		}

		/* Utils.out(r.pid, String.format("%s > %s", source, destination)); */
		if (! source.isFaulty() && ! destination.isFaulty()) {
			if (delay >= 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ignored) {}
			}
			try {
				destination.getQueue().put(m.pack());
			} catch (InterruptedException ignored) {}
		}
		return ;
	}
	
	private int getDelay () {
		int d;
		double x;
		int y;
		if (Utils.GAUSSIAN) {
			x = (random.nextGaussian() * stdv) + mean;
			y = (int) Math.round(x);
			if (y < 0)
				d = 0;
			else
				d = y;
		} else
			d = Utils.DELAY;
		return d;
	}
	
	private void deliver (Message m) {
		int source = m.getSource();
		int destination = m.getDestination();
		if (destination != -1) {
			unicast(m, getDelay());
		} else {
			int delay;
			boolean drop = false;
			boolean first = true;
			/* Broadcast. */
			for (int i = 1; i <= r.n; i++) {
				drop = (source == i && Utils.SELFMSGENABLED == false);
				if (drop)
					continue;

				if (first) {
					delay = getDelay();
					first = false;
				} else delay = -1;
				
				m.setDestination(i);
				unicast(m, delay);
			}
		}
	}
	
	public void run() {
		
		InputStreamReader input;
		BufferedReader b;
		PrintWriter p;
		
		String message, reply;
		boolean result;
		
		long t__recv, dt;
		double rate;

		try {
			input = new InputStreamReader(s.getInputStream());
			b = new BufferedReader(input);
			p = new PrintWriter(s.getOutputStream());
			
			while ((message = b.readLine()) != null) {
				
				Message m = Message.parse(message);
				
				if (m.getDestination() == r.pid) { 
					myprocess = m.getSource();
					/* 
					 * Note that a process that registers blocks 
					 * until all other processes have registered.
					 */
					String payload = m.getPayload();
					/* Payload is of the form `name:host:port` */
					StringTokenizer tokens = new StringTokenizer(payload, ":");
					String name = tokens.nextToken();
					String host = tokens.nextToken();
					int port = Integer.parseInt(tokens.nextToken());
					Record record = new Record(name, host, port);
					new Thread(new MessageHandler(record)).start();
					result = r.registeR (record);
					/* Synchronise P(i), for all i. */
					synchronized (r.getLock()) {
						while (! r.areRegistered()) {
							try {
								r.getLock().wait();
							} catch (InterruptedException ignored) {}
						}
					}
					reply = (result ? "OK" : "ERR");
					p.println(reply);
					p.flush();
					
					/* Yield the processor; allow other threads to be notified. */
					Thread.yield();
				
				} else { /* Relay message. */
					
					count += 1;
					if (count == 1)
						_t_recv = System.currentTimeMillis();
					if (count % Utils.STEP == 0) {
						t__recv = System.currentTimeMillis();
						dt = t__recv - _t_recv;
						rate = (double) (Utils.STEP * 1000) / (double) dt;
						Utils.out(r.pid, String.format("[W %03d][RECV] %06d\t%10.1f", myprocess, count, rate));
						_t_recv = t__recv;
					}
					deliver (m);
					/* Assumes always correct. */
					p.println("OK");
					p.flush();
				}
			}
			/* Null message. */
			p.close();
			b.close();
			s.close();
		} catch (IOException e) {
			System.err.println(String.format("Error: P%d's worker has failed.", myprocess));
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
	class MessageHandler extends Thread { /* Again, once per process... */
		
		private Socket socket;
		
		private InputStreamReader input;
		private BufferedReader b;
		private PrintWriter p;
		
		private String name;
		private String host;
		private int    port;

		private BlockingQueue<String> queue;
		
		public MessageHandler(Record record) {
			name = record.getName();
			host = record.getHost();
			port = record.getPort();
			queue = record.getQueue();
		}
		
		private String getInfo () {
			String s = null;
			s = String.format("[%s at %s:%d]", name, host, port);
			return s;	
		}
		
		private Socket connect () {
			Socket s = null;
			int attempts = 0;
			do {
				attempts ++;
				try {
					s = new Socket(host, port);
					s.setKeepAlive(true);
					s.setSoTimeout(0);

				} catch (Exception e) {
					String msg =
						String.format("Warning: connection attempt %d to %s failed.",
					attempts, getInfo());
					System.err.println(msg);
					System.err.println(e.getMessage());
					try {
						Thread.sleep(random.nextInt(100) + 1); /* [0..100] + 1 > 0.*/
					} catch (InterruptedException ignored) {}
				}
			} while (s == null);
			return s;
		}
		
		private void init () { /* Configure I/O */
			try {
				input = new InputStreamReader(socket.getInputStream());
				b = new BufferedReader(input);
				p = new PrintWriter(socket.getOutputStream());

			} catch (IOException e) { 
				/* Ignore for now. */
			}
			return ;
		}
		
		public void run () {
			socket = connect();
			init ();
			for (;;) {
				String message = null;
				try {
					message = queue.take();
				} catch (InterruptedException ignored) {}
				write(message);
			}
		}
		
		private boolean write (String message) {
			p.println(message);
			p.flush();
			return true;
		}
	}
}
