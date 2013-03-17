import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;


public class PerfectFailureDetector implements IFailureDetector {

	Process process;
	HashSet<Integer> suspects;
	Timer timer;
	long[] processLastMessage;
	
	static final int Delta = 1000; /* 1sec */
	
	class PeriodicTask extends TimerTask {
		public void run() {
			process.broadcast("heartbeat", "null");
			
			long currentTime = System.currentTimeMillis();
			for (int i = 0; i < processLastMessage.length; i++) {
				long timeDifference = currentTime - processLastMessage[i];
				if ((processLastMessage[i] != 0) && (timeDifference > (Delta + Utils.DELAY))){
					if (!suspects.contains(i))
						suspects.add(i);
				}
			}
			
			/**
			 * For debugging purposes
			 */
//			for (int p : suspects) {
//				Utils.out("Process " + (p + 1) + " crashed!");
//			}

		}
	}

	public PerfectFailureDetector(Process p) {
		this.process = p;
		this.timer = new Timer();
		this.suspects = new HashSet<Integer>();
		processLastMessage = new long[p.n];
	}

	@Override
	/* Initiates communication tasks, e.g. sending heartbeats periodically */
	public void begin() {
		timer.schedule(new PeriodicTask(), 0, Delta);
	}

	/* Handles in-coming (heartbeat) messages */
	@Override
	public void receive(Message m) {
		long recieveTime = System.currentTimeMillis();
		processLastMessage[m.getSource() - 1] = recieveTime;
		
		//print suspects as well
		Utils.out(process.pid, m.toString());
	}

	/* Returns true if ‘process’ is suspected */
	@Override
	public boolean isSuspect(Integer process) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public int getLeader() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* Notifies a blocking thread that ‘process’ has been suspected.
	 * * Used only for tasks in §2.1.3 */
	@Override
	public void isSuspected(Integer process) {
		// TODO Auto-generated method stub

	}

}
