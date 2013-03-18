import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;


public class PerfectFailureDetector implements IFailureDetector {

	Process process;
	HashSet<Integer> suspects;
	Timer timer;
	long[] processLastMessage;
	TimerTask periodicTask;
	protected Message latestMessage;
	
	static final int Delta = 1000; /* 1sec */
	
	class PeriodicTask extends TimerTask {
		public void run() {
			process.broadcast("heartbeat", "null");
			
			long currentTime = System.currentTimeMillis();
			for (int i = 0; i < processLastMessage.length; i++) {
				long timeDifference = currentTime - processLastMessage[i];
				if ((processLastMessage[i] != 0) && (timeDifference > (Delta + Utils.DELAY))){
					if (!suspects.contains(i)){
						System.out.println("Suspected " + (i+1));
						suspects.add(i);
						isSuspected(i);
					}
						
				}
			}

		}
	}

	public PerfectFailureDetector(Process p) {
		this.process = p;
		this.timer = new Timer();
		this.suspects = new HashSet<Integer>();
		processLastMessage = new long[p.n];
		this.periodicTask = new PeriodicTask();
	}

	@Override
	/* Initiates communication tasks, e.g. sending heartbeats periodically */
	public void begin() {
		timer.schedule(this.periodicTask, 0, Delta);
	}

	/* Handles in-coming (heartbeat) messages */
	@Override
	public synchronized void receive(Message m) {
		long recieveTime = System.currentTimeMillis();
		processLastMessage[m.getSource() - 1] = recieveTime;
		latestMessage = m;
		//print suspects as well
		Utils.out(process.pid, m.toString());
		notifyAll();

	}

	/* Returns true if ‘process’ is suspected */
	@Override
	public boolean isSuspect(Integer process) {
		return suspects.contains(process);
	}


	@Override
	public int getLeader() {
		return 0;
	}

	/* Notifies a blocking thread that ‘process’ has been suspected.
	 * * Used only for tasks in §2.1.3 */
	@Override
	public synchronized void isSuspected(Integer process) {
		notifyAll();
	}

}
