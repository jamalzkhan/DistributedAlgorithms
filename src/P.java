class P extends Process {
	public P(String name, int pid, int n) {
		super(name, pid, n);
	}

	public void begin() {
	}

	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		P p = new P(name, id, n);
		p.registeR();
		p.begin();
	}
}