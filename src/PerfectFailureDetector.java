import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


public class PerfectFailureDetector implements IFailureDetector {

	Process p;
	LinkedList<Integer> suspects;
	Timer timer;
	int[] processLastMessage;
	
	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", "null");
		}
	}

	public PerfectFailureDetector(Process p) {
		this.p = p;
		processLastMessage = new int[p.n];
	}
	
	@Override
	/* Initiates communication tasks, e.g. sending heartbeats periodically */
	public void begin() {
		
	}

	/* Handles in-coming (heartbeat) messages */
	@Override
	public void receive(Message m) {
		// TODO Auto-generated method stub

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
