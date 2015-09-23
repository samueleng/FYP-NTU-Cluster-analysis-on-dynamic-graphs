package chinesewhispers;

import de.uni_leipzig.asv.toolbox.ChineseWhispers.algorithm.ChineseWhispers;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CW {

    /*File Name*/
    public static String FileName;
    /*Matrix variables*/
    public static int size = 0;
    public static int dimensions;

    /*Building Matrix*/
    public static int clusters;
    public static int numberKeys = 0;
    public  HashMap<String, Integer> stringToID = new HashMap<>();
    public TreeSet<Integer> node_index = new TreeSet<>();
    public TreeSet<Integer> node_label = new TreeSet<>();

    public HashMap<Integer, Integer> edge = new HashMap<>();
    public Vector<Integer> node_info1 = new Vector<>();
    public Vector<Integer> node_info2 = new Vector<>();
    public static int numberOfEdges;
    public static int clusterNumber = 1;

    /*Build adjacent matrix*/
    public static Matrix<IntContainer> adjMatrix;
    /*Build transitive matrix*/
    public static Matrix<DoubleContainer> transMatrix;

    /*HashMap init*/
    static long time1 = 0, time2 = 0;
    static int count = 0;
    static String Node_File_Path = "";
    static String Edge_File_Path = "";
    static String save_file_path = "";

    /*Functions*/
    /*convert Hashmap to adjacent matrix*/
    public static void convertToGraph(String input, HashMap<String, Integer> idMap) {
        int node1, node2;
        StringTokenizer tk;

        /*This tk is for those files with comma delimiter*/
        tk = new StringTokenizer(input, ",");
        /*This tk is for those files with tab delimiter*/
        //tk = new StringTokenizer(input); 

        node1 = idMap.get(tk.nextToken());
        node2 = idMap.get(tk.nextToken());

        /*Assign corresponding role/column*/
        adjMatrix.get(node1).c[node2] = 1;
        adjMatrix.get(node2).c[node1] = 1;
        time2 = System.currentTimeMillis();
        String graph_str = "Edge between: " + node1 + " and: " + node2 + "  time: " + (time2 - time1) + "  duration : 1";
        System.out.println(graph_str);
        try (PrintWriter graph = new PrintWriter(new File(Utils.getFilePath() + "/chinesewhispers/resource/Graph.txt"))) {
            graph.write(graph_str + "\n");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CW.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*Read node file*/
    public void readData(String fileName) throws FileNotFoundException, IOException {
        String input;
        String current_line;
        int id = 1;
        FileName = fileName;
        numberOfEdges = 0;
        boolean empty = true;

        /*Read datasets from the folder (nodes)*/
        try (Scanner inFile = new Scanner(new File(Utils.getFilePath() + "/chinesewhispers/resource/" + fileName))) {
            while (inFile.hasNext()) {
                current_line = inFile.nextLine();
                empty = false;
                numberOfEdges++;
                StringTokenizer items;

                /*This tk is for those files with tab delimiter*/
                //items = new StringTokenizer(current_line, " "); 
                /*This tk is for those files with comma delimiter*/
                items = new StringTokenizer(current_line, ",");

                /*assign value/key in hashmap*/
                while (items.hasMoreTokens()) {
                    String token = items.nextToken();
                    if (!stringToID.containsKey(token)) {
                        stringToID.put(token, id);
                        id++;
                    }
                    node_index.add(stringToID.get(token));
                    node_label.add(new Integer(token));
                }
                count++;
            }
        }
        /*empty file*/
        if (empty) {
            numberOfEdges = 1;
        }

        /*calculate number of nodes*/
        dimensions = node_index.size();
        Node_File_Path = Utils.getFilePath() + "/chinesewhispers/Node/Node" + count + ".txt";
        PrintWriter Node = new PrintWriter(Node_File_Path);
        int index = 0, label = 0;
        while (!node_index.isEmpty()) {
            index = node_index.first();
            label = node_label.first();
            Node.write(index + "\t" + label + "\n");
            Node.flush();
            node_index.remove(index);
            node_label.remove(label);
        }
        System.out.println("Number of edges: " + numberOfEdges + "  Number of vertices: " + dimensions);
        /*Container (memory issues)*/
        adjMatrix = new Matrix<>(dimensions + 1, dimensions + 1, new IntContainer(id));
        try (Scanner data_in = new Scanner(new File(Utils.getFilePath() + "/chinesewhispers/resource/" + FileName))) {
            time1 = System.currentTimeMillis();
            for (int i = 1; i <= numberOfEdges; i++) {
                input = data_in.nextLine();
                /*get to matrix work*/
                convertToGraph(input, stringToID);
            }
        }
        for (int i = 1; i < dimensions + 1; i++) {
            for (int j = 1; j < dimensions + 1; j++) {
                if (adjMatrix.get(i).c[j] == 1) {
                    node_info1.add(i);
                    node_info2.add(j);
                }
            }
        }
    }
    /*Build Edge File*/

    public void Builder_Edge_File() throws FileNotFoundException, IOException {
        Vector<Integer> buf_vect = new Vector<>();
        HashMap<Integer, Integer> node_map = new HashMap<>();
        Edge_File_Path = Utils.getFilePath() + "/chinesewhispers/Edge/Edge" + count + ".txt";
        PrintWriter Edge = new PrintWriter(new File(Edge_File_Path));
        for (int i = 0; i < node_info1.size(); i++) {
            buf_vect.add(i);
            node_map.put(i, node_info2.get(i));
        }
        Sort_Vector(node_info1, buf_vect);
        for (int i = 0; i < node_info1.size(); i++) {
            /*Default weight between Edges is 1*/
            /*Can change this value*/
            Edge.write(node_info1.get(i) + "\t" + node_map.get(buf_vect.get(i)) + "\t" + 1 + "\n");
            Edge.flush();
        }
    }

    /*sort vector based on elements*/
    public static void Sort_Vector(Vector<Integer> vect1, Vector<Integer> vect2) {
        int temp1 = 0, temp2 = 0;
        for (int i = 0; i < vect1.size() - 1; i++) {
            for (int j = i + 1; j < vect1.size(); j++) {
                temp1 = vect1.get(i);
                temp2 = vect2.get(i);
                if (vect1.elementAt(i) > vect1.elementAt(j)) {
                    vect1.setElementAt(vect1.get(j), i);
                    vect1.setElementAt(temp1, j);
                    vect2.setElementAt(vect2.get(j), i);
                    vect2.setElementAt(temp2, j);
                }
            }
        }
    }

    public void ChineseWhispers() throws Exception {

        System.out.println("\n");
        System.out.println("===****Run ChineseWhisper****===");

        /*Run Chinese Whisper Algorithm*/
        ChineseWhispers cw = new ChineseWhispers();
        cw.reNumber(Node_File_Path, Edge_File_Path);
        cw.setCWGraph(Node_File_Path, Edge_File_Path);

        /*SetParameter 
         NOTE: Not yet experimented on*/
        /*This is default value
         * min_weight = 0
         * mutation = constant
         * update = continuous
         * the number of iteration = 20*/
        cw.setCWParameters(0, "top", "nolog", 0, "constant", 0, "continuous", 20, false);
        cw.run();

        /*format: Cluster number, number of nodes, { elements[numberofnodes] }*/
        save_file_path = Utils.getFilePath() + "/chinesewhispers/result/ClusterResults/" + FileName + "CWResults.txt";
        cw.writeFile(false, false, false, save_file_path);
        cw.writeClusters(save_file_path);
    }

    /*Display the Result*/
    public void Display_Result() throws Exception {
        String line_str = null;
        StringTokenizer tk;
        int cluster_num = 0;
        String cluster = null;
        System.out.println("====****Result of ChineseWhispeers Algorithm****====");
        Scanner cluster_line = new Scanner(new File(save_file_path));
        while (cluster_line.hasNext()) {
            line_str = cluster_line.nextLine();
            tk = new StringTokenizer(line_str, "\t");
            cluster_num = new Integer(tk.nextToken());
            tk.nextToken();
            cluster = tk.nextToken();
            System.out.println("Cluster Number " + cluster_num + ": " + "{" + cluster + "}");
        }
    }

}
