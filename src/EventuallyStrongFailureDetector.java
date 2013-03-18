public class EventuallyStrongFailureDetector extends EventuallyPerfectFailureDetector {
	
	class ConsensusTask extends PeriodicTask {
		
		public void run() {
			super.run();
		}
	}	

	public EventuallyStrongFailureDetector(Process p) {
		super(p);
		this.periodicTask = new ConsensusTask();
	}
	
	// Overiden the recieve method

}
