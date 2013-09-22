import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class CreateConnect {
	private ShareFile sharefile = null;
	private WriteLog writelog = null;
	private ReadConfig  readconfig;
	
	private int localid = 0;
	private NewPeerInfo localinfo = null;
	private CreateClient createclient;
	private CreateServer createserver;
	private Connect[] connect;
	private Connect optneighbor;	
	private volatile BitSet localbitfield;
	private boolean notstop;  //true-- not stop; false-- stop
	
	private int connectcount;
	private Timer timer1;
	private Timer timer2;
	private DateFormat dataformat;
	private Calendar calendar;
	private BitSet requestmap;  //false--not request, true--request
	private RequestSchedule requestschedule;
	private int piecenum;   // piece number
	
	public CreateConnect(int localid){
		this.connectcount = 0;
		this.notstop = true;
		this.localid = localid;
		this.readconfig = new ReadConfig();
		this.sharefile = new ShareFile(this.readconfig.getFileName(), localid);
		this.writelog = new WriteLog(localid);	
		this.dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.calendar = Calendar.getInstance();			
		
		this.createclient =  new CreateClient(this.localid);
		this.localinfo = createclient.getLocalInfo();
		this.createserver = new CreateServer(this.localid,this.localinfo, this.createclient.getPeerInfoVector());
		this.createserver.start();
		this.createclient.start();			
	}	
	
	public void getConnect(){
		int i=0;
		this.connectcount = this.createclient.getConnect().size() + this.createserver.getConnect().size();
		if(connectcount ==0){
			System.out.println("wrong connect number!");
			return;
		}
		//else System.out.println("connect number = "+ connectcount);
		
		this.connect = new Connect[connectcount];
		for(Connect temp: this.createclient.getConnect())
			this.connect[i++] = temp;
		
		for(Connect temp: this.createserver.getConnect())
			this.connect[i++] = temp;	
	}
	
	public void work(){
		try{
			this.createclient.join();
			this.createserver.join();
		}catch(Exception e){
			e.printStackTrace();			
		}
		getConnect();
		if(this.connectcount==0) return;
		synInfo();
		
		timer1 = new Timer(); 
		timer2 = new Timer();
		timer1.schedule(new SchedulePreNeighbor(), 10, this.readconfig.getUnchokingInterval()*1000);
		timer2.schedule(new ScheduleOptiNeighbor(), 10, this.readconfig.getOptimisticUnchokingInterval()*1000);
		this.requestschedule = new RequestSchedule();
		this.requestschedule.setPriority(10);
		this.requestschedule.start();		
	}
	
	public void stopconnect(){
		for(int i=0; i< this.connectcount; i++){
			if(connect[i].getPeerStatus() != Connect.statustype.complete)
				return;
		}				
		System.out.println("all peers have complete file");
		for(int i=0; i< this.connectcount; i++)
			connect[i].StopConnect();	
		notstop = false;
							
	}
	
	public class RequestSchedule extends Thread{			
		public void run(){
			while(true){
				if(localbitfield.cardinality() == piecenum){
					stopconnect();
					if(notstop==false){						
						timer1.cancel();
						timer2.cancel();
						writelog.close();
						sharefile.close();	
						System.out.println("end program");
						break;
					}		
				}else{
					for(int i=0; i<connectcount; i++){						
						while(connect[i].getLocalStatus() == Connect.statustype.unchoke && 
								connect[i].getPeerInterest()== Connect.interest.interested && connect[i].getBlockStatus()==false){
							int ran = (int)(Math.random() * 100000)%piecenum;
							if(requestmap.get(ran)==false && connect[i].getPeerFieldStatus(ran)){
								requestmap.set(ran);
								connect[i].setBlockStatus(true);
								connect[i].sendRequest(ran);
								break;
							}								
						}							
					}	
				}
			}
		}
	}
	
	public class SchedulePreNeighbor extends TimerTask{
		int neighbor;	
		BitSet  chokemap; //false--uncheck, true--check
		
		public SchedulePreNeighbor(){
			neighbor = readconfig.getNumberOfPreferredNeighbours();	
			chokemap = new BitSet(connectcount);
		}
		
		public void run() {					
			chokemap.set(0, connectcount, false);
			int count = 0;	
			StringBuffer logMessage = new StringBuffer(dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" has the preferred neighbors ");			
			String temp = new String(logMessage);
			
			if(localbitfield.cardinality() == piecenum){							
				while(count < neighbor){
					int ran = (int)(Math.random() * 100)%connectcount;					
					if(chokemap.get(ran)== false){
						chokemap.set(ran);
						//System.out.println("ran = "+ ran);
						if(connect[ran].getLocalInterest() == Connect.interest.interested){
							count++;
							logMessage.append(connect[ran].getPeerInfo().getPeerId()+ ",");
							if(connect[ran].getPeerStatus() == Connect.statustype.choke){
								
								connect[ran].sendNoPayload((byte)1);
							}
						}else{
							if(connect[ran].getPeerStatus() == Connect.statustype.unchoke){
								connect[ran].sendNoPayload((byte)0);
							}
						}
					}
					if(chokemap.cardinality()==connectcount)
						break;
				}		
			}else{
				java.util.Arrays.sort(connect);
				int i=0;
				while(count < neighbor && i < connectcount){
					if(connect[i].getLocalInterest() == Connect.interest.interested){
						count++;
						logMessage.append(connect[i].getPeerInfo().getPeerId()+ ",");
						if(connect[i].getPeerStatus() == Connect.statustype.choke)
							connect[i].sendNoPayload((byte)1);
					}else{
						if(connect[i].getPeerStatus() == Connect.statustype.unchoke)
							connect[i].sendNoPayload((byte)0);
					}
					connect[i].setDownLoadRate(0);
					i++;
					
				}
				for(; i < connectcount; i++){
					connect[i].setDownLoadRate(0);
					if(connect[i].getPeerStatus() == Connect.statustype.unchoke && connect[i]!= optneighbor)
						connect[i].sendNoPayload((byte)0);
				}
			}
					
			if(!temp.equals(new String(logMessage))) {
				logMessage.replace(logMessage.length()-1, logMessage.length(),".\n");
				writelog.writeLog(new String(logMessage));	
			}						
		}		
	}	
		
	public class ScheduleOptiNeighbor extends TimerTask{
		BitSet chokemap; //false--choke, true--unchoke
		
		public ScheduleOptiNeighbor(){
			chokemap = new BitSet(connectcount);
		}
		@Override
		public void run() {
			int count = 0;
			chokemap.set(0, connectcount, false);
			optneighbor = null;
			while(count < 1){
				int i = (int)(Math.random() * 100)%connectcount;
				//System.out.println("i = "+ i);
				if(chokemap.get(i) == false){
					chokemap.set(i);
					if( connect[i].getLocalInterest() == Connect.interest.interested && 
							connect[i].getPeerStatus() == Connect.statustype.choke){
						count++;
						optneighbor = connect[i];
						connect[i].sendNoPayload((byte)1);
					}
				}
				if(chokemap.cardinality()==connectcount){
					optneighbor = null;
					break;
				}												
			}
			if(optneighbor != null){
				String logMessage = dataformat.format(calendar.getTime())+ ": Peer "+ localinfo.getPeerId()+
					" has the optimistically unchoked neighbors "+ optneighbor.getPeerInfo().getPeerId()+" .\n";
				writelog.writeLog(logMessage);
			}
		}
	}
	
	public void synInfo(){
		int filesize = this.readconfig.getFileSize();
		int piecesize = this.readconfig.getPieceSize();
		int length = 0;	
		
		if(filesize%piecesize == 0)
			length = filesize/piecesize;
		else length = filesize/piecesize+1;
		
		this.piecenum = length;
		this.localbitfield = new BitSet(length);
		this.requestmap = new BitSet(length);
		if(!this.localinfo.getCompleteflag()){
			this.localbitfield.set(0, length, false);
			this.requestmap.set(0, length, false);
		}else this.localbitfield.set(0, length, true);
		
		for(int i =0; i< this.connectcount ; i++)
			this.connect[i].synInfo(this.readconfig,this.writelog, this.localinfo, 
					this, this.localbitfield,this.dataformat, this.calendar);
	}
	
	synchronized public void informPeerHave(int pieceindex){	
		this.localbitfield.set(pieceindex);
		for(int i =0; i< this.connectcount; i++){
			this.connect[i].sendHave(pieceindex);
			if(this.localbitfield.cardinality() == this.piecenum){
				this.connect[i].setLocalStatus(Connect.statustype.complete);
				//this.connect[i].sendNoPayload((byte)3);  //not interested
				this.connect[i].sendNoPayload((byte)8);
			}
		}
				
	}
	
	public void writeBuffer(byte[] payload, long offset, int size, int index){
		this.sharefile.writeFile(payload,size, offset);
		this.localbitfield.set(index);
	}
	
	public void readBuffer(byte[] payload, int offset, int size){
		this.sharefile.readFile(payload, offset);
	}
		
}
