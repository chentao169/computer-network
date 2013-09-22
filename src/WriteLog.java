import java.io.File;
import java.io.FileWriter;

public class WriteLog {
	private File file = null;
	private FileWriter wlog = null;
	
	public WriteLog(int peerid){
		this.file = new File("log_peer_"+peerid+".log");
		if(!this.file.exists())
		{	
			try{
				this.file.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			this.file.delete();
			try{
				this.file.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		try{
			this.wlog = new FileWriter(this.file);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	synchronized public void writeLog (String info){			
		try{
			this.wlog.write(info);
			this.wlog.flush();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void close(){
		try{
			this.wlog.close();
			this.file=null;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
