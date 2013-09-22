import java.io.BufferedReader;
import java.io.FileReader;


public class ReadConfig{
	private int NumberOfPreferredNeighbours;
	private int UnchokingInterval;
	private int OptimisticUnchokingInterval;
	private String FileName;
	private int FileSize;
	private int PieceSize;
	
	public ReadConfig() {
		String st=null;
		String[] parameter=new String[6];
		try{
			BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
		    
		    int i=0;
			while((st = in.readLine()) != null) {
				String[] tokens = st.split(" ");
				parameter[i++] = tokens[1];
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		this.setNumberOfPreferredNeighbours(Integer.parseInt(parameter[0]));
		this.setUnchokingInterval(Integer.parseInt(parameter[1]));
		this.setOptimisticUnchokingInterval(Integer.parseInt(parameter[2]));
		this.setFileName(parameter[3]);
		this.setFileSize(Integer.parseInt(parameter[4]));
		this.setPieceSize(Integer.parseInt(parameter[5]));
		
	}

	
	public void setNumberOfPreferredNeighbours(int numberOfPreferredNeighbours) {
		this.NumberOfPreferredNeighbours = numberOfPreferredNeighbours;
	}
	public int getNumberOfPreferredNeighbours() {
		return this.NumberOfPreferredNeighbours;
	}
	
	public void setUnchokingInterval(int unchokingInterval) {
		this.UnchokingInterval = unchokingInterval;
	}
	public int getUnchokingInterval() {
		return this.UnchokingInterval;
	}

	public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval) {
		this.OptimisticUnchokingInterval = optimisticUnchokingInterval;
	}
	public int getOptimisticUnchokingInterval() {
		return this.OptimisticUnchokingInterval;
	}

	public void setFileName(String fileName) {
		this.FileName = fileName;
	}
	public String getFileName() {
		return this.FileName;
	}
	
	public void setFileSize(int fileSize) {
		this.FileSize = fileSize;
	}
	
	public int getFileSize() {
		return this.FileSize;
	}

	public void setPieceSize(int pieceSize) {
		this.PieceSize = pieceSize;
	}
	public int getPieceSize() {
		return this.PieceSize;
	}
		
	public void showContent(){
		System.out.println("this.NumberOfPreferredNeighbours  "+this.getNumberOfPreferredNeighbours());
		System.out.println("this.UnchokingInterval  "+this.getUnchokingInterval());
		System.out.println("this.OptimisticUnchokingInterval  "+this.getOptimisticUnchokingInterval());
		System.out.println("this.FileName  "+this.getFileName());
		System.out.println("this.FileSize  "+this.getFileSize());
		System.out.println("this.PieceSize  "+this.getPieceSize());
	}
}
