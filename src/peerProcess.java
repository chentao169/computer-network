
public class peerProcess {
	 	
	public static void main(String[] args) {
		int localid = Integer.parseInt(args[0]);				
		
		CreateConnect creatconnect = new CreateConnect(localid);	
		creatconnect.work();			
	}

}