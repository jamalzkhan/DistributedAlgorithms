class EPFDProcess extends Process {

	private IFailureDetector detector;
	private String heartBeat = "heartbeat";

	public EPFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		this.detector = new EventuallyPerfectFailureDetector(this);
	}

	public void begin() {
		this.detector.begin();
	}
	
	public synchronized void receive (Message m) {
		String type = m.getType();
		if (type.equals(heartBeat)) {
			detector.receive(m);
		}
	}
	
	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		EPFDProcess p = new EPFDProcess(name, id, n);
		p.registeR();
		p.begin();
	}
}
