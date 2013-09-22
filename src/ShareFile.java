import java.io.File;
import java.io.RandomAccessFile;


public class ShareFile {
	private File file = null;
	private File dir = null;
	private RandomAccessFile wfile = null;
			
	public ShareFile(String filename, int localid){
		String path = System.getProperty("user.dir");
		this.dir = new File(path+"/peer_"+ localid);
		if(!this.dir.exists()){
			this.dir.mkdir();
		}
		this.file = new File(path+"/peer_"+ localid+"/"+filename);
		if(!this.file.exists())
		{	
			try{
				this.file.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{			
			this.wfile = new RandomAccessFile(this.file, "rwd");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void readFile(byte[] content, int offset){
		int rv =0;
		try{
			this.wfile.seek(offset);
			rv = this.wfile.read(content);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(rv <= 0)
				System.out.println("readfile problem occurs!rv = "+ rv);
		}
	}
	
	synchronized public void writeFile(byte[] content,int size, long offset){
		try{
			this.wfile.seek(offset);
			this.wfile.write(content, 4, size);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void close(){
		try{
			this.wfile.close();
			this.file=null;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
