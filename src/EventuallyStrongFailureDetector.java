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
	private int m = 0;
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
					boolean d = isAllSameDecision();
					process.broadcast("OUTCOME", Boolean.toString(d) + ","+Integer.toString(v)
							+ "," + Integer.toString(r));
				}
				else if(collect("OUTCOME", c)){
					String messagePayload = messageCollected.getPayload();
					String [] tokens = messagePayload.split(",");
					int value = Integer.parseInt(tokens[1]);
					boolean allSame = Boolean.parseBoolean(tokens[0]);
					if (allSame){
						x = value;
						if (m == c){
							finished = true;
						}
						else {
							m = c;
						}
					}
					
				}		
			}
		}
	}	

	private synchronized void recieveMessages(){
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

	private boolean isAllSameDecision(){
		int prevLeader = -1;
		for (Message m: messages){
			if (prevLeader == -1){
				prevLeader = getLeaderFromMessage(m);
			} else if (prevLeader != getLeaderFromMessage(m)) {
				return false;
			}
		}
		return true;
	}

	
	private int getRoundFromMessage(Message m){
		String messagePayload = m.getPayload();
		String [] tokens = messagePayload.split(",");
		return Integer.parseInt(tokens[1]);
	}
	
	private int getLeaderFromMessage(Message m){
		String messagePayload = m.getPayload();
		String [] tokens = messagePayload.split(",");
		return Integer.parseInt(tokens[0]);
	}

	private int majority(){
		int [] sumMajority = new int[n];
		for (Message m : messages){
			sumMajority[getLeaderFromMessage(m) - 1]++;
		}

		// Return the max
		int majority = 0;
		int max = sumMajority[majority];
		for(int i = 1; i < sumMajority.length; i++){
			if (max < sumMajority[i]){
				majority = i;
				max = sumMajority[i];
			}
		}

		return majority;
	}
	
	@Override
	public synchronized void receive(Message m){
		super.receive(m);
		if (m.getType().equals("VAL")){
			if (!suspects.contains(m.getSource()) && r == getRoundFromMessage(m)){
				messages.add(m);
				notifyAll();
			}
		}
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
