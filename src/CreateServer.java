import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class CreateServer extends Thread{
	 private ServerSocket mainSocket = null;
	 private ArrayList<Connect> socket = null;
	 private int localid = 0;
	 private NewPeerInfo localinfo = null;
	 private ArrayList<NewPeerInfo> peerinfovector;
		
	 public CreateServer(int localid, NewPeerInfo localinfo, ArrayList<NewPeerInfo> peerinfovector){
		 this.localid = localid;		
		 this.localinfo = localinfo;
		 this.peerinfovector = peerinfovector;
		 this.socket = new ArrayList<Connect>();
		 
		 try{
			 this.mainSocket = new ServerSocket(this.localinfo.getPeerPort()); 	
		 }catch(Exception e) {
				e.printStackTrace();	
		 }		 
	 }
	 
	 public void run(){
		 int size = this.peerinfovector.size();
		 int lastpeer = Integer.parseInt(this.peerinfovector.get(size-1).getPeerId());		 
		 int count=0;
		 Socket temp;
		 
		 while((lastpeer - this.localid) != count){  
			try{		
				temp=this.mainSocket.accept();
				this.socket.add(new Connect(temp)); 
				//if(temp != null)
					count++;
			}catch(Exception e) {
				e.printStackTrace();	
			}
		}			
		try{
			this.mainSocket.close();  
		}catch(Exception e) {
			e.printStackTrace();	
		}		
	 }
	 
	 public ArrayList<Connect> getConnect(){
		 return this.socket;
	 }
	 
}
