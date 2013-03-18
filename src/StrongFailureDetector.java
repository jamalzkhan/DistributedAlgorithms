import java.util.TimerTask;

import javax.rmi.CORBA.Util;


public class StrongFailureDetector extends PerfectFailureDetector {

	static final int Delta = 1000; /* 1sec */
	private int round = 1;
	private int i = this.process.pid;
	private int x = this.process.pid;
	private int n = this.process.n;
	private Message messageCollected;
	

	class ConsensusTask extends PeriodicTask{

		@Override
		public void run() {
			super.run();
			if (round <= n){
				if (round == i){
					process.broadcast("VAL", Integer.toString(x) + "," + Integer.toString(round));
					Utils.out("I broadcasted my leadership!");
				} else if (collect("VAL", round)) {
					String payLoad = messageCollected.getPayload();
					String [] messageContents = payLoad.split(",");
					int value = Integer.parseInt(messageContents[0]);
					int recievedRound = Integer.parseInt(messageContents[1]);
					if (round == recievedRound)
						x = value;
					Utils.out("Collected a new leader " + x);
				}
				round++;
			} else {
				Utils.out("The consensus was process: " + x);
			}
		}

	}



	public StrongFailureDetector(Process p) {
		super(p);
		this.periodicTask = new ConsensusTask();
	}

	private synchronized boolean collect(String value, int r){

		Utils.out("Reached collect");
		while (		!suspects.contains(r) && (
						latestMessage == null ||
						latestMessage.getSource() != r ||
						!latestMessage.getType().equals(value)
					)
				) {
			try {
				Utils.out("I got blocked and " + latestMessage.toString());
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (latestMessage != null){
			messageCollected = latestMessage;
		}
		
		Utils.out("I got unblocked");
		return !isSuspect(r);
	}

}
