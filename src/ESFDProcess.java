
public class ESFDProcess extends Process {

	private IFailureDetector detector;
	private String heartBeat = "heartbeat";

	public ESFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		this.detector = new EventuallyStrongFailureDetector(this);
	}
	
	public void begin() {
		this.detector.begin();
	}

	public synchronized void receive (Message m) {
		String type = m.getType();
		if (type.equals(heartBeat) || type.equals("VAL")) {
			detector.receive(m);
		}
	}

	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		ESFDProcess p = new ESFDProcess(name, id, n);
		p.registeR();
		p.begin();
	}

}
