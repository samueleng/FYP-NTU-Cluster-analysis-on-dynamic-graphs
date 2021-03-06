
package chinesewhispers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static Map<String, List<Integer>> readCluster(String fileName) throws FileNotFoundException {
        Map<String, List<Integer>> map = new HashMap<>();
        Scanner src = new Scanner(new File(getFilePath() + "/chinesewhispers/result/ClusterResults/" + fileName));
        while (src.hasNext()) {
            String[] line = src.nextLine().split("\\t+");
            String key = "Cluster Number " + line[0] + " :";
            String temp = line[2];
            String[] value = temp.split(",");
            List<Integer> list = new ArrayList<>();
            for (String value1 : value) {
                if (value != null && !value1.equals("")) {
                    list.add(new Integer(value1.trim()));
                }
            }
            map.put(key, list);
        }
        return map;
    }

    static void compareCluster(String t1, String t2) throws FileNotFoundException {
        Map<String, List<Integer>> parentCluster = new HashMap<>();
        Map<String, List<Integer>> childCluster = new HashMap<>();

        //Cluster Result file at time t1
        parentCluster = readCluster("data" + t1 + "CWResults.txt");

        //Cluster Result file at time t2
        childCluster = readCluster("data" + t2 + "CWResults.txt");

        String fileName = "data" + t1 + "_" + t2 + "CWResults.txt";
        try (PrintWriter printer = new PrintWriter(new File(getFilePath() + "/chinesewhispers/result/CompareResults/detail_" + fileName)); PrintWriter writer = new PrintWriter(new File(getFilePath() + "/chinesewhispers/result/CompareResults/" + fileName))) {
            for (Map.Entry<String, List<Integer>> parebtEntrySet : parentCluster.entrySet()) {
                boolean matchFound = false;
                String parentKey = parebtEntrySet.getKey();
                List<Integer> parentValue = parebtEntrySet.getValue();

                //Ternary operator to display "Node" or "Nodes"
                String out = parentKey + "(t:" + t1 + "): Total : " + parentValue.size() + " Nodes" + " ===> ";
                for (Map.Entry<String, List<Integer>> childEntrySet : childCluster.entrySet()) {
                    String childKey = childEntrySet.getKey();
                    List<Integer> childValue = childEntrySet.getValue();
                    if (childValue.containsAll(parentValue)) {
                        matchFound = true;
                        //So that the text isn't printed for a parentCluster having no common elements in any of the childClusters
                        if (out != null) {
                            System.out.println(out);
                            writer.println(out);
                            out = null;
                        }
                        System.out.print(childKey + "(t:" + t2 + "): " + parentValue.size() + " Nodes" + " from (t:" + t1 + ") ");

                        //Write in result file
                        writer.print(childKey + "(t:" + t2 + "): " + parentValue.size() + " Nodes" + " from (t:" + t1 + ") ");

                        //write in detailed result file
                        printer.println(parentKey + " (t:" + t1 + "): " + parentValue + " ===> ");
                        printer.println(childKey + " (t:" + t2 + "): " + childValue);
                        if (childValue.size() > parentValue.size()) {
                            System.out.println("Child size bigger");
                            List<Integer> temp = new ArrayList<>();
                            temp.addAll(childValue);
                            temp.removeAll(parentValue);

                            System.out.print(temp.size() > 1 ? (temp.size() + " New Nodes") : (temp.size() + " New Node"));
                            System.out.println(" | Total: " + childValue.size() + " Nodes");

                            //Write in result file
                            writer.println(temp.size() + " New Nodes | Total: " + childValue.size() + " Nodes");
                            printer.println("New Nodes: " + temp);
                        }
                        else{
                            writer.println();
                        }
                        System.out.println();
                    } else if (!Collections.disjoint(parentValue, childValue)) {
                        matchFound = true;
                        //So that the text isn't printed for a parentCluster having no common elements in any of the childClusters
                        if (out != null) {
                            System.out.println(out);
                            writer.println(out);
                            printer.println(parentKey + " (t:" + t1 + "): " + parentValue + " ===> ");
                            out = null;
                        }
                        List<Integer> temp = new ArrayList<>();
                        temp.addAll(childValue);
                        temp.removeAll(parentValue);
                        System.out.print(childKey + "(t:" + t2 + "): " + intersection(parentValue, childValue).size() + " Nodes from (t:" + t1 + ") ");
                        System.out.print(temp.size() > 1 ? (temp.size() + " New Nodes") : (temp.size() + " New Node"));
                        System.out.println(" | Total: " + childValue.size() + " Nodes");

                        //Write in result file
                        writer.println(childKey + "(t:" + t2 + "): " + intersection(parentValue, childValue).size() + " Nodes from (t:" + t1 + ") " + temp.size() + " New Nodes | Total: " + childValue.size() + " Nodes");

                        //write in detailed result file
                        printer.println(childKey + " (t:" + t2 + "): " + childValue);
                        printer.println("New Nodes: " + temp);
                    }
                }
                if (matchFound) {
                    System.out.println("");
                    writer.println("-------------------------------");
                    printer.println("-------------------------------");
                }

            }
        }
    }

    public static void readDynamicData(String input, String fileNumber) {
        String fileName = input.split("\\s+")[0];
        int start, end;
        start = Integer.parseInt(input.split("\\s+")[1]);
        end = Integer.parseInt(input.split("\\s+")[2]);
        //swap values of start and end
        if (end < start) {
            System.out.println("start: " + start);
            System.out.println("end: " + end);
            end += start;
            start = end - start;
            end -= start;
        }
        Scanner src = new Scanner(CW.class.getResourceAsStream("/chinesewhispers/dataset/" + fileName));
        try (PrintWriter writer = new PrintWriter(new File(getFilePath() + "/chinesewhispers/resource/data" + fileNumber))) {
            boolean firstLiine = true;
            while (src.hasNext()) {
                String[] line = src.nextLine().split("\\s+");
                int t = Integer.parseInt(line[2]);
                int d = Integer.parseInt(line[3]);
                if ((t + d) > end) {
                    break;
                }
                if (t >= start && (t + d) <= end) {
                    if (!firstLiine) {
                        writer.println();
                    }
                    writer.print(line[0] + "," + line[1]);
                    firstLiine = false;
                }
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ChineseWhispers.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean fileExists(String fileName) {
        return new File(getFilePath() + "/chinesewhispers/result/ClusterResults/data" + fileName + "CWResults.txt").exists();
    }

    /**
     *
     * @return Returns path of working directory
     */
    public static String getFilePath() {
        File file = new File("");
        return file.getAbsolutePath() + "/src";
    }

    public static String getFileName(String file) {
        return file.substring(0, file.length() - 4);
    }

    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<>();

        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
}
