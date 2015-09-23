package MLRMCL;

import java.io.IOException;

public class runHash {  
    
    static readWriteHashMap rwHash = new readWriteHashMap(); 
    public static String dataFilePath = "results.txt"; 
    public static String dataFilePath2 = "results2.txt";  
    public static String dataFilePath3 = "data.txt"; 
    public static void main(String[] args) throws IOException{ 
      
            rwHash.readHashMap();
    } 
    
}
