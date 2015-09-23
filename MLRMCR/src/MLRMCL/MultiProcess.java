package MLRMCL;

public class MultiProcess extends Thread{
     int count1 = 0, count2 = 0;
     public MultiProcess(int t1, int t2){
    	 count1 = t1; count2 = t2;
     }
}
