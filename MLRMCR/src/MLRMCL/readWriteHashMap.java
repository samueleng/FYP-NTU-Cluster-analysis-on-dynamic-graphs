package MLRMCL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class readWriteHashMap { 
 
    public void writeHashMap(HashMap hashmap,String filename) throws IOException {
        File file = new File("./src/MLRMCL/HashMap/hm" + filename + ".txt");//your file
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (Object p : hashmap.keySet()) {
                bw.write(p + "," + hashmap.get(p));
                bw.newLine();
            } 
            bw.flush();
            bw.close();
        }

    }
    public void readHashMap() throws IOException {
        System.out.println("*******READING HASHMAP********");
        Map<String, String> map = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        BufferedReader in = new BufferedReader(new FileReader("results.txt"));
        BufferedReader in2 = new BufferedReader(new FileReader("results2.txt"));
        String line = "";
        while ((line = in.readLine()) != null) {
            String parts[] = line.split(",");

            map.put(parts[0], parts[1]);
        }
        in.close();

        String line2 = "";
        while ((line2 = in2.readLine()) != null) {
            String parts2[] = line2.split(",");

            map2.put(parts2[0], parts2[1]);
        }
        in2.close();
        compareHashSet(map, map2);
        System.out.println("*******END OF HASHMAP********");
    }//end of readHashMap method
 
    
    public void compareHashSet(Map<String, String> map, Map<String, String> map2) {
        HashMap<String, String> hMapNotInMap2 = new HashMap<>();
        HashMap<String, String> hMapNotInMap;
        hMapNotInMap = new HashMap<>();

        /*Checks what map2 has but map does not*/
        for (Entry<String, String> entry : map.entrySet()) {
            // Check if the current value is a key in the 2nd map
            if (!map2.containsKey(entry.getKey()) || !map2.containsKey(entry.getValue())) {
                // map2 doesn't have the key for this value. Add key-value in new map.
                hMapNotInMap2.put(entry.getKey(), entry.getValue());
            }
        }
        /*Checks what map has but map2 does not*/
        for (Entry<String, String> entry : map2.entrySet()) {
            // Check if the current value is a key in the 1st map
            if (!map.containsKey(entry.getKey()) || !map.containsKey(entry.getValue())) {
                // map doesn't have the key for this value. Add key-value in new map.
                hMapNotInMap.put(entry.getKey(), entry.getValue());
            }
        }

        System.out.println("*****New values in Map ******");
        System.out.println("Ordered by Values,ClusterNumber");
        System.out.println(hMapNotInMap2.toString());
        System.out.println("**************************************");
        System.out.println("*****New values in Map2******");
        System.out.println("Ordered by Values,ClusterNumber");
        System.out.println(hMapNotInMap.toString());
        System.out.println("**************************************");

    }//end of compareHashSet method 

}//end of class

