import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.util.BitSet;
import java.util.Calendar;

public class Connect implements Comparable<Connect>{
	private Socket csocket = null;
	private DataInputStream cin = null;
	private DataOutputStream cout = null;
	private WriteLog writelog = null;
	private DateFormat dataformat;
	private Calendar calendar;
	private CreateConnect createconnect;
		
	public static enum statustype {choke, unchoke, complete};
	public static enum interest {interested, notinterested};
	private statustype peerstatus;
	private statustype localstatus;
	private interest peerinterest;
	private interest localinterest;
	private NewPeerInfo peerinfo = null;
	private NewPeerInfo localinfo = null;	
	private volatile BitSet peerbitfield;
	private volatile BitSet localbitfield;
	private int downloadrate = 0;	
	
	private ReceiveMessage recethread;
	private byte[] buffer;
	private int bytenum;
	
	private ReadConfig  readconfig;
	private int piecenum;
	private int lastpiecesize;
	private boolean stop;  //stop connect and its threads
	private boolean connecttype; // false--server, true--client
	private boolean block;  //false--not block, true--block for request new piece	
	private byte[] payload = null;
	
	public void Init(){
		this.peerstatus = statustype.choke;
		this.localstatus = statustype.choke;
		this.peerinterest = interest.notinterested;
		this.localinterest = interest.notinterested;
		this.stop = false;
		return;
	}
	
	//client connection
	public Connect(NewPeerInfo peerinfo){
		if(peerinfo==null)
			return;	
		
		Init();
		this.peerinfo = peerinfo;		
		this.connecttype = true;
		
		try{
			this.csocket = new Socket(this.peerinfo.getPeerAddress(), this.peerinfo.getPeerPort());
			this.cin = new DataInputStream(this.csocket.getInputStream());  
			this.cout = new DataOutputStream(this.csocket.getOutputStream());  			
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}
	
	//sever connection
	public Connect(Socket socket) {
		if(socket==null)
			return;
					
		Init();
		this.connecttype = false;
		this.peerinfo = new NewPeerInfo();
		
		try{
			this.csocket= socket;			
			this.cin = new DataInputStream(this.csocket.getInputStream());  
			this.cout = new DataOutputStream(this.csocket.getOutputStream()); 
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}
	
	public void clearBuffer(){
		java.util.Arrays.fill(this.buffer, (byte)0);
	}
	
	public void clearPayload(){
		java.util.Arrays.fill(this.payload, (byte)0);
	}
	
	public class ReceiveMessage extends Thread{				
		public void run(){
			while(stop == false)
				try {
					if(cin.available()>0)
						receMessage();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}		
		// choke, unchoke, interested, notinterested, have, bitfield, request, piece
		public void receMessage(){		
			byte[] length = new byte[4];		// total length
			try {
				cin.readFully(length, 0, 4);
			} catch (IOException e) {
				e.printStackTrace();
			}
									
			if(new String(length).equals("CEN5")){
				receHandShake();
				return;
			}			
				
			byte[] type = new byte[1];
			try {
				cin.readFully(type, 0, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int size = byte2int(length) -4 -1; //size of payload
			clearPayload();
			if(size > 0){				
				try {					
					cin.readFully(this.payload, 0, size);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			byte mtype = type[0];
			switch(mtype){
				case (byte)0:
					receNoPayload((byte)0);
					break;
				case 1:
					receNoPayload((byte)1);
					break;
				case 2:
					receNoPayload((byte)2);
					break;
				case 3:
					receNoPayload((byte)3);
					break;
				case 4:
					receHave(payload);
					break;
				case 5: 
					receBitField(payload);
					break;
				case 6:
					receRequest(payload);
					break;
				case 7:				
					recePiece(payload, size);
					break;
				case 8:
					receNoPayload((byte)8);
					break;
				default:
					System.out.println("wrong message type!");
			}				
	}	
		
	synchronized public void sendHandShake(){		
		byte[] head = "CEN5501C2008SPRING".getBytes();
		byte[] zero = {0,0,0,0,0,0,0,0,0,0};
		byte[] peerid = this.localinfo.getPeerId().getBytes();
		
		clearBuffer();
		System.arraycopy(head, 0, this.buffer, 0, 18);
		System.arraycopy(zero, 0, this.buffer, 18, 10);
		System.arraycopy(peerid, 0, this.buffer, 28, 4);	
		this.bytenum = 32;
		
		try {
			this.cout.write(this.buffer, 0, this.bytenum);
			this.cout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			clearBuffer();
			this.bytenum = 0;				
		}
		if(this.connecttype == true){
			String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+this.localinfo.getPeerId()
					+" makes a connection to Peer "+this.peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
		}
		//System.out.println(logMessage);
	}	
	
	public void receHandShake(){
		byte[] message = new byte[32-4];
		
		try {
			this.cin.read(message,0,32-4);
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
		byte[] head = new byte[18-4];
		System.arraycopy(message, 0, head, 0, 18-4);
		
		byte[] peerid = new byte[4];
		System.arraycopy(message, 28-4, peerid, 0, 4);
				
		this.peerinfo.setPeerId(new String(peerid));
		
		if(this.connecttype==false){
			String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ this.localinfo.getPeerId()
					+" is connected from Peer " + new String(peerid) + ".\n";
			this.writelog.writeLog(logMessage);
		}
		//System.out.println(logMessage);
		if(connecttype == false)
			sendHandShake();
		
		sendBitField();						
	}
		
	public int getBitfieldSize(){
		int filesize = this.readconfig.getFileSize();
		int piecesize = this.readconfig.getPieceSize();

		if(filesize%piecesize == 0)
			return filesize/piecesize;
		else return filesize/piecesize+1;
	}
	
	public byte[] setBitfield(){
		int piecenum = getBitfieldSize();
		int bytenum = 0;
		if(piecenum%8 == 0)
			bytenum = piecenum/8;
		else bytenum = piecenum/8+1;
		
		byte[] localbitfield = new byte[bytenum];	
		for(int i=0; i< bytenum-1; i++)
			localbitfield[i] = (byte) 0xff;				
		localbitfield[bytenum-1] = (byte)(0xff<<(8-(piecenum-(bytenum-1)*8)));		
		return localbitfield;
	}
	
	synchronized public void sendBitField(){		
		if(this.localinfo.getCompleteflag()){
			byte[] bitfield = setBitfield();
			byte[] buffer = new byte[bitfield.length+1+4];
			System.arraycopy(int2byte(bitfield.length+1+4), 0, buffer, 0, 4);
			buffer[4] = (byte)5;
			System.arraycopy(bitfield, 0, buffer, 5, bitfield.length);
			this.bytenum = bitfield.length + 4 + 1;	
			try {
				this.cout.write(buffer, 0, this.bytenum);
				this.cout.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				clearBuffer();
				this.bytenum = 0;				
			}
		}	
	}
	
	public void receBitField(byte[] payload){		
		if(payload[0] !=0){
			this.peerbitfield.set(0, this.piecenum, true);
			this.peerstatus = statustype.complete;
			if(!this.localbitfield.equals(this.peerbitfield))
				this.sendNoPayload((byte)2);  
		}		
		return;				
	}
	
	public boolean getPeerFieldStatus(int index){
		return this.peerbitfield.get(index);
	}
			
	//0-choke, 1-unchoke, 2-interested, 3-not interested
	synchronized public void sendNoPayload(byte type){		
		int length = 4+1;
		byte[] seg1 = int2byte(length);	
		
		System.arraycopy(seg1, 0, this.buffer, 0, 4);
		this.buffer[4] = type;  		
		this.bytenum = length;
		try {
			this.cout.write(this.buffer, 0, this.bytenum);
			this.cout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			clearBuffer();
			this.bytenum = 0;				
		}
		
		switch(type){
		case 0:
			this.peerstatus =  statustype.choke;
			/*logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" send choke msg to "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			System.out.println(logMessage);*/
			break;
		case 1:
			this.peerstatus =  statustype.unchoke;
			/*logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" send unchoke msg to "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			System.out.println(logMessage);*/
			break;
		case 2:
			this.peerinterest =  interest.interested;
			/*logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" send interest msg to "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);*/
			break;
		case 3:
			this.peerinterest =  interest.notinterested;
			/*logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" send uninterest msg to "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);*/
			break;	
		}		
	}
		
	public void receNoPayload(byte type){
		String logMessage = null;
		switch(type){
		case 0:
			this.localstatus =  statustype.choke;
			logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" is choked by "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);
			break;
		case 1:
			this.localstatus =  statustype.unchoke;
			logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" is unchoked by "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);
			break;
		case 2:
			this.localinterest =  interest.interested;
			logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" received the 'interested' message from "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);
			break;
		case 3:
			this.localinterest =  interest.notinterested;
			logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" received the 'not interested' message from "+ peerinfo.getPeerId()+" .\n";
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);
			break;		
		case 8:
			this.peerstatus = statustype.complete;
			this.localinterest = interest.notinterested;
			break;
		}
	}
	
	synchronized public void sendHave(int pieceindex){		
		int length = 4+1+4;
		byte[] index = int2byte(pieceindex);
		byte[] seg1 = int2byte(length);
		
		System.arraycopy(seg1, 0, this.buffer, 0, 4);
		this.buffer[4] = (byte)4;
		System.arraycopy(index, 0, this.buffer, 5, 4);		
		this.bytenum = length;
		try {
			this.cout.write(this.buffer, 0, this.bytenum);
			this.cout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			clearBuffer();
			this.bytenum = 0;				
		}
		/*
		String logMessage;		
		logMessage = " send have piece "+ pieceindex +" to " + this.peerinfo.getPeerId()+"\n";
		this.writelog.writeLog(logMessage);
		System.out.println(logMessage);			
		*/
	}
	
	synchronized public void sendRequest(int pieceindex){		
		int length = 4+1+4;
		byte[] index = int2byte(pieceindex);
		byte[] seg1 = int2byte(length);
		
		System.arraycopy(seg1, 0, this.buffer, 0, 4);
		this.buffer[4] = (byte)6;
		System.arraycopy(index, 0, this.buffer, 5, 4);		
		this.bytenum = length;
		try {
			this.cout.write(this.buffer, 0, this.bytenum);
			this.cout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			clearBuffer();
			this.bytenum = 0;				
		}
		/*
		String logMessage;		
		logMessage = " send requst piece "+ pieceindex +" to " + this.peerinfo.getPeerId()+"\n";
		//this.writelog.writeLog(logMessage);
		System.out.println(logMessage);
		printBitfield();
		*/
		
	}
	
	public boolean getPeerComplete(){
		if(this.peerbitfield.cardinality() == this.piecenum)
			return true;
		else return false;
	}
	public void receHave(byte[] payload){
		if(payload == null){
			System.out.println("receHave exception: payload is null");
			return;
		}			
		int index = byte2int(payload);			
		this.peerbitfield.set(index);
		if(this.peerbitfield.cardinality() == this.piecenum){
			this.peerstatus = statustype.complete;	
		}
	
		String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
				" received the 'have' message from "+ peerinfo.getPeerId()+" for the piece "+ index+ ".\n";
		this.writelog.writeLog(logMessage);
		//System.out.println(logMessage);
		//this.writelog.writeLog("after receive have msg :");
		//printBitfield();
		
		if(this.localbitfield.get(index) == false)				
			this.sendNoPayload((byte)2);
	}	
	
	public void receRequest(byte[] payload){
		if(payload == null){
			System.out.println("receRequest exception: payload is null");
			return;
		}
		int index = byte2int(payload);
		/*String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
				" received the request message from "+ peerinfo.getPeerId()+" for the piece "+ index+ ".\n";
		this.writelog.writeLog("reveive request piece "+ index +" from " + this.peerinfo.getPeerId()+"\n");
		System.out.println(logMessage);
		*/
		sendPiece(index);
	}
	
	public void computeFileInfo(){
		int filesize = this.readconfig.getFileSize();
		int piecesize = this.readconfig.getPieceSize();
			
		if(filesize%piecesize ==0){
			this.piecenum = filesize/piecesize;
			this.lastpiecesize = this.readconfig.getPieceSize();
		}else {
			this.piecenum = filesize/piecesize+1;
			this.lastpiecesize = this.readconfig.getFileSize() - this.readconfig.getPieceSize()*(this.piecenum -1);			
		}
		this.peerbitfield = new BitSet(this.piecenum);		
		this.peerbitfield.set(0, this.piecenum, false);	
	}
	
	synchronized public void sendPiece(int pieceindex){
		this.bytenum = 0;
		clearBuffer();
		
		int size = 0;
		if(pieceindex == this.piecenum-1)
			size = this.lastpiecesize;
		else size = this.readconfig.getPieceSize();
		
		int length = 4+1+4+size;
		byte[] index = int2byte(pieceindex);
		byte[] seg1 = int2byte(length);
		byte[] payload = new byte[size];
		int offset = pieceindex * this.readconfig.getPieceSize();
		this.createconnect.readBuffer(payload, offset, size);
		
		System.arraycopy(seg1, 0, this.buffer, 0, 4);
		this.buffer[4] = 7;
		System.arraycopy(index, 0, this.buffer, 5, 4);		
		System.arraycopy(payload, 0, this.buffer, 9, size);
		this.bytenum = length;	
		try {
			this.cout.write(this.buffer, 0, this.bytenum);
			this.cout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			clearBuffer();
			this.bytenum = 0;				
		}
		
		/*
		String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
				" send " + peerinfo.getPeerId()+" for the piece "+ pieceindex+ ".\n";
		this.writelog.writeLog(logMessage);
		*/
	}	
	
	public void recePiece(byte[] payload, int psize){
		if(payload == null){
			System.out.println("recePiece exception: payload is null");
			return;
		}
		
		byte[] pieceindex = new byte[4];
		System.arraycopy(payload, 0, pieceindex, 0, 4);
		
		int index = byte2int(pieceindex);
		long offset = (long)index * this.readconfig.getPieceSize();
		int size = psize-4;
		this.createconnect.writeBuffer(payload, offset,size, index);
		this.setDownLoadRate(1);
		
		String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ this.localinfo.getPeerId()
				+" has download the piece "+ index + "  from "+ this.peerinfo.getPeerId()+" .\n";		
		this.writelog.writeLog(logMessage);	
		
		this.createconnect.informPeerHave(index);
			
		if(this.localbitfield.cardinality() == this.piecenum){
			this.localstatus = statustype.complete;
			logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ this.localinfo.getPeerId()
					+" has download the complete file.\n";		
			this.writelog.writeLog(logMessage);
			//System.out.println(logMessage);
		}						
		
		if(this.localbitfield.equals(this.peerbitfield))
			this.sendNoPayload((byte)3);  //not interested
				
		this.block = false;
	}
	
	public void setBlockStatus(boolean block){
		this.block = block;
	}
	
	public boolean getBlockStatus(){
		return this.block;
	}
	
	public void setPeerStatus(Connect.statustype status){
		this.peerstatus = status;
	}
	
	public statustype getPeerStatus(){
		return this.peerstatus;
	}
	
	public statustype getLocalStatus(){
		return this.localstatus;
	}
	
	public void setLocalStatus(Connect.statustype status){
		this.localstatus = status;
	}
	
	public interest getPeerInterest(){
		return this.peerinterest;
	}
	
	public interest getLocalInterest(){
		return this.localinterest;
	}
	
	public NewPeerInfo getPeerInfo(){
 		return this.peerinfo;
 	}
	
 	public void setDownLoadRate(int temp){
		if(temp==0)
			this.downloadrate = 0;
		else this.downloadrate ++;
	}
 	
 	public void StopConnect(){
 		/*try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}*/
 		
 		this.stop = true;
 		try {
 			this.recethread.join();
			this.cin.close();
			this.cout.close();
			this.csocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 		
 	}
 		
 	public void synInfo(ReadConfig  readconfig, WriteLog writelog, NewPeerInfo localinfo, 
 			CreateConnect createconnect, BitSet localbitfield, DateFormat dataformat,Calendar calendar ){
 		this.readconfig = readconfig;
		this.writelog = writelog;
		this.localinfo = localinfo;
		this.createconnect = createconnect;
		this.localbitfield = localbitfield;
		this.dataformat = dataformat;
		this.calendar = calendar;	
		
		computeFileInfo();
		int buffersize = this.readconfig.getPieceSize() + 4 +1 +4;
		this.buffer = new byte[buffersize];
		this.payload = new byte[buffersize];
		
		if(this.localinfo.getCompleteflag())
			this.localstatus = statustype.complete;
				
		this.recethread = new ReceiveMessage();
		this.recethread.start();
		if(this.connecttype)
			sendHandShake();
		
 	}
		
	public int compareTo(Connect obj) {
		if(this.downloadrate > obj.downloadrate)
			return -1;
		else if(this.downloadrate < obj.downloadrate)
			return 1;
		else{
			int ran = (int)(Math.random() * 100)%2;
			if(ran == 0)
				return -1;
			else return 1;
		}
	}
		
	public static byte[] int2byte(int intValue) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (intValue >> 8 * (3 - i) & 0xFF);
        }
        return b;
    }
    
    public static int byte2int(byte[] b) {
        int intValue = 0;	
        for (int i = 0; i < b.length; i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - i));
        }
        return intValue;
    }
}