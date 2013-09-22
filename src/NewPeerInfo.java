
public class NewPeerInfo {
	private String peerId = null;
	private String peerAddress = null;
	private int peerPort = 0;
	private boolean completeflag;
	
	public NewPeerInfo(){}
	
	public NewPeerInfo(String pId, String pAddress, String pPort, String pCompleteflag) {
		setPeerId(pId);
		setPeerAddress(pAddress);
		setPeerPort(pPort);
		setCompleteflag(pCompleteflag);
	}

	public String getPeerId() {
		return this.peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}

	public String getPeerAddress() {
		return this.peerAddress;
	}

	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}

	public int getPeerPort() {
		return this.peerPort;
	}

	public void setPeerPort(String peerPort) {
		this.peerPort = Integer.parseInt(peerPort);
	}

	public boolean getCompleteflag() {
		return this.completeflag;
	}

	public void setCompleteflag(String completeflag) {
		if(completeflag.equals("1"))
			this.completeflag = true;
		else this.completeflag = false;
	}
}
