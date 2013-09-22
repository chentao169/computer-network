import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class CreateClient extends Thread{
	private ArrayList<NewPeerInfo> peerInfoVector = null;
	private int size = 0;
	private NewPeerInfo localinfo = null;
	private int localid = 0;
	private ArrayList<Connect> csocket;
	
	public CreateClient(int localid){				
		this.localid = localid;		
		this.peerInfoVector = new ArrayList<NewPeerInfo>();
		this.csocket = new ArrayList<Connect>();
		String st = null;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((st = in.readLine()) != null) {				
				 String[] tokens = st.split(" ");	
				 NewPeerInfo temp = new NewPeerInfo(tokens[0], tokens[1], tokens[2],tokens[3]);
			     peerInfoVector.add(temp);
			     
			     if(Integer.parseInt(tokens[0]) == this.localid)
			    	 this.localinfo = temp;			    	 
			}			
			in.close();
		}catch (Exception ex){
			System.out.println(ex.toString());
		}  
		
		this.size = peerInfoVector.size();	
	}
	
	public void run(){ 	
		for(int i=0; i< size-1; i++){
			int peerid = Integer.parseInt(this.peerInfoVector.get(i).getPeerId());			
			if( peerid < this.localid)
				this.csocket.add(new Connect(this.peerInfoVector.get(i)));			
			else break;				
		}			
	}
	
	public NewPeerInfo getLocalInfo(){
		return this.localinfo;
	}
	
	public ArrayList<Connect> getConnect(){
		return this.csocket;
	}	
	
	public ArrayList<NewPeerInfo> getPeerInfoVector(){
		return this.peerInfoVector;
	}
}
	
	
