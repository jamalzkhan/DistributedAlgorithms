import java.util.ArrayList;
import java.util.List;

public class EventuallyStrongFailureDetector extends EventuallyPerfectFailureDetector {
	
	private Message messageCollected;
	private boolean finished;
	private int r = 0;
	private int c = 0;
	private int i = process.pid;
	private int n = process.n;
	private int x = process.pid;
	private int thresholdListSize;
	private List<Message> messages;
	
	class ConsensusTask extends PeriodicTask {
		
		public void run() {
			super.run();
			
			if (!finished){
				r++;
				c = (r % n) + 1;
				messages.clear();
				process.broadcast("VAL", Integer.toString(x) + "," + Integer.toString(r));
				
				if (i==c){
					recieveMessages();
					/* Majority and d need to be coded up */
					int v = majority();
					int d = -1;
					process.broadcast("OUTCOME", Integer.toString(d) + ","+Integer.toString(v)
							+ "," + Integer.toString(r));
				}
				else if(collect("OUTCOME", c)){
					String messagePayload = messageCollected.getPayload();
					String [] tokens = messagePayload.split(",");
					int value = Integer.parseInt(tokens[1]);
					int currRound = Integer.parseInt(tokens[0]);
					x = value;
				}		
			}
		}
	}	
	
	private void recieveMessages(){
		while (messages.size() < thresholdListSize){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Utils.out("Collected " + messages.size() + " messages");
	}

	public EventuallyStrongFailureDetector(Process p) {
		super(p);
		this.periodicTask = new ConsensusTask();
		this.messages = new ArrayList<Message>();
		this.thresholdListSize = n - (n-1)/3 - 1;
	}
	
	private int majority(){
		return -1;
	}
	
	private synchronized boolean collect(String value, int r){

//		Utils.out("Reached collect");
		while (		!suspects.contains(r) && (
						latestMessage == null ||
						latestMessage.getSource() != r ||
						!latestMessage.getType().equals(value)
					)
				) {
			try {
//				Utils.out("I got blocked and whatever");
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (latestMessage != null){
			messageCollected = latestMessage;
		}
		
//		Utils.out("I got unblocked");
		return !isSuspect(r);
	}

}
