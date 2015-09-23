package MCLAlgorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class MCLApp {

    /*File Name*/
    private String FileName;
    /*Matrix variables*/
    private int dimensions;

    /*Building Matrix*/
    private HashMap<String, Integer> stringToID = new HashMap<>();

    /*Build adjacent matrix*/
    private Matrix<IntContainer> adjMatrix;
    /*Build transitive matrix*/
    private Matrix<DoubleContainer> transMatrix;

    /*The expansion operator is responsible for allowing
     flow to connect different regions of the graph. */
    public static int power = 2; //expansion power of 2 as default
    /*The inflation parameter affects cluster granularity*/
    public static double inflate = 2; //inflation of 2 (squaring)  

    /*HashMap init*/
    static readWriteHashMap rwHash = new readWriteHashMap();

    /*Functions*/
    /*convert Hashmap to adjacent matrix*/
    public void convertToGraph(String input, HashMap<String, Integer> idMap) {
        int node1;
        int node2;
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
        System.out.println("Edge between: " + node1 + " and: " + node2);
    }

    public void readData(String fileName) throws FileNotFoundException, IOException {
        TreeSet<Integer> tempBuffer = new TreeSet<>();
        String input;
        String current_line;
        int id = 0;
        FileName = fileName;
        int numberOfEdges = 0;
        boolean empty = true;
        /*Read datasets from the folder (nodes)*/
//        try (BufferedReader inFile = new BufferedReader(new FileReader("/home/deepak/freelance/nodes/" + FileName))) {

        try (BufferedReader inFile = new BufferedReader(new FileReader(new File(Utils.getFilePath() + "/MCLAlgorithm/resource/" + FileName)))) {
            while ((current_line = inFile.readLine()) != null) {
                empty = false;
                StringTokenizer items;
                numberOfEdges++;

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
                    tempBuffer.add(stringToID.get(token));
                }
            }
        }
        /*empty file*/
        if (empty) {
            numberOfEdges = 1;
        }
        /*calculate number of nodes*/
        dimensions = tempBuffer.size();
        System.out.println("Number of edges: " + numberOfEdges);
        System.out.println("Number of entries: " + dimensions);
        /*Container (memory issues)*/
        adjMatrix = new Matrix(IntContainer.class, dimensions, dimensions, i -> new IntContainer(i));
        transMatrix = new Matrix<>(DoubleContainer.class, dimensions, dimensions, i -> new DoubleContainer(i));
//        try (BufferedReader data_in = new BufferedReader(new FileReader("/home/deepak/freelance/nodes/" + FileName))) {
        try (BufferedReader data_in = new BufferedReader(new FileReader(new File(Utils.getFilePath() + "/MCLAlgorithm/resource/" + FileName)))) {
            for (int i = 0; i < numberOfEdges; i++) {
                if ((input = data_in.readLine()) != null) {
//                    input = data_in.readLine();
                    /*get to matrix work*/
                    convertToGraph(input, stringToID);
                }
            }
        }

    }
    /*For validating the matrix*/

    public void printMatrix(Matrix<DoubleContainer> matrix) {
        for (int i = 0; i < dimensions; i++) {
            System.out.println();
            for (int j = 0; j < dimensions; j++) {
                System.out.print(matrix.get(i).c[j] + " ");
            }
        }
        System.out.println();
    }

    public void printIntMatrix(Matrix<IntContainer> matrix) {
        for (int i = 0; i < dimensions; i++) {
            System.out.println();
            for (int j = 0; j < dimensions; j++) {
                System.out.print(matrix.get(i).c[j] + " ");
            }
        }
        System.out.println();
    }

    /*Small simple path loops can complicate things.
     There is a strong effect that odd powers of expansion obtain
     their mass from simple paths of odd length, and likewise for
     even.
     Adds a dependence to the transition probabilities on the
     parity of the simple path lengths.
     The addition of self looping edges on each node
     resolves this.
     Adds a small path of length 1, so the mass does not only
     appear during odd powers of the matrix. 
     */
    public void addSelfLoop() {
        for (int i = 0; i < dimensions; i++) {
            for (int j = 0; j < dimensions; j++) {
                adjMatrix.get(i).c[i] = 1;
            }
        }
    }

    /*Construct the transition matrix for each node and edge*/
    public void constructTransitionMatrix() {
        double columnSum[] = new double[dimensions];

        /*column sums to one. Each column has a digit to identify the number of nodes connected to it (degree)*/
        for (int row = 0; row < dimensions; row++) {
            for (int col = 0; col < dimensions; col++) {
                columnSum[col] += adjMatrix.get(row).c[col];
            }
        }
        /*Check the number and convert it into probability*/
        for (int row = 0; row < dimensions; row++) {
            for (int col = 0; col < dimensions; col++) {
                transMatrix.get(row).c[col] = (double) adjMatrix.get(row).c[col] / columnSum[col];
            }
        }
    }

    /*Convergence to a state where probability is steady such that every column value has the same 
     number. Returns a true/false value*/
    public boolean checkConvergence() {
        double prev = -1;
        /*Use the previous variable (prev) to check if after inflation/expansion if the value of columns are the same 
         if it is not the same, return false and continue to run while loop*/
        for (int j = 0; j < dimensions; j++) {
            for (int i = 0; i < dimensions; i++) {
                if (transMatrix.get(i).c[j] != 0) {
                    prev = transMatrix.get(i).c[j];
                    break;
                }
            }
            for (int i = 0; i < dimensions; i++) {
                if (transMatrix.get(i).c[j] != 0) {
                    if (transMatrix.get(i).c[j] != prev) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /* Expansion coincides with taking the power of a stochastic matrix using the normal matrix product (i.e. matrix squaring) 
     Expansion corresponds to computing random walks of higher length, which means random walks with many steps.  
     It associates new probabilities with all pairs of nodes, where one node is the point of departure and the other is the destination.  
     Since higher length paths are more common within clusters than between different clusters, the probabilities associated with node pairs  
     lying in the same cluster will, in general, be relatively large as there are many ways of going from one to the other. */
    public Matrix<DoubleContainer> expand() {
        Matrix<DoubleContainer> matrix = new Matrix<>(DoubleContainer.class, dimensions, dimensions, d -> new DoubleContainer(d));
        int p = power;
        while (p > 1) {
            for (int i = 0; i < dimensions; i++) {
                for (int j = 0; j < dimensions; j++) {
                    for (int k = 0; k < dimensions; k++) {
                        matrix.get(i).c[j] += transMatrix.get(i).c[k] * transMatrix.get(k).c[j];
                    }
                }
            }
            p--;
        }
        return matrix;
    }

    /*The inflation operator is responsible for both strengthening and weakening of current.
     (Strengthens strong currents, and weakens already weak currents).
     The inflation parameter, r (in this case; variable inflate), controls the extent of this
     strengthening / weakening. (In the end, this influences the granularity of clusters.)*/
    public void inflate() {
        double[] sum = new double[dimensions];
        /* multiplication of two matrices of the same size can be defined by multiplying the corresponding entries 
         and this is known as the Hadamard product.*/
        for (int j = 0; j < dimensions; j++) {
            for (int i = 0; i < dimensions; i++) {
                sum[j] += Math.pow(transMatrix.get(i).c[j], inflate);
            }
        }
        for (int j = 0; j < dimensions; j++) {
            for (int i = 0; i < dimensions; i++) {
                transMatrix.get(i).c[j] = Math.pow(transMatrix.get(i).c[j], inflate) / sum[j];
            }
        }
    }

    /*Reduces iteration, improves speed: The threshold is one fourth of the average of all the entries in a column. 
     Any value less than that (0.25) will be set to 0 (Assuming they will reach there anyway*/
    public void prune() {
        Matrix<DoubleContainer> matrix = new Matrix<>(DoubleContainer.class, dimensions, dimensions, d -> new DoubleContainer(d));
        for (int i = 0; i < dimensions; i++) {
            for (int j = 0; j < dimensions; j++) {
                matrix.get(i).c[j] = transMatrix.get(i).c[j];
            }
        }
        for (int j = 0; j < dimensions; j++) {
            double avg, mx;
            avg = 0;
            mx = -1.0;
            for (int k = 0; k < dimensions; k++) {
                avg = avg + matrix.get(k).c[j];
                mx = max(mx, matrix.get(k).c[j]);
            }
            avg = avg / (dimensions - 1);
            double threshold = avg / 4;
            for (int i = 0; i < dimensions; i++) {
                if (transMatrix.get(i).c[j] < threshold) {
                    transMatrix.get(i).c[j] = 0;
                }
            }
        }
        /*after pruning, renormalise the matrix*/
        normalise();
    }

    /*Normalise the transitive matrix*/
    public void normalise() {
        double csum;
        for (int j = 0; j < dimensions; j++) {
            csum = 0;
            for (int k = 0; k < dimensions; k++) {
                csum = csum + transMatrix.get(k).c[j];
            }
            for (int i = 0; i < dimensions; i++) {
                transMatrix.get(i).c[j] = transMatrix.get(i).c[j] / csum;
            }
        }
    }

    /*Implementation of the Markov clustering algorithm
     Expand 
     Inflate  
     Pruning
     Normalize the matrix
     Do the above until convergence
     Interpret resulting matrix to discover clusters.*/
    public void mcl() throws IOException {
        int iteration = 1;
        System.out.println("Iteration " + iteration);
        transMatrix = expand();
        inflate();
        iteration++;
        normalise();
        long t1 = System.nanoTime();
        /*Main function calling*/
        while (!checkConvergence()) {
            System.out.println("Iteration " + iteration);
            transMatrix = expand();
            inflate();
            iteration++;
            prune();
        }
        long t2 = System.nanoTime() - t1;
        System.out.print("Finish convergence in: ");
        System.out.println(String.format("%d min, %d sec",
                TimeUnit.NANOSECONDS.toMinutes(t2),
                TimeUnit.NANOSECONDS.toSeconds(t2)
                - TimeUnit.NANOSECONDS.toSeconds(TimeUnit.NANOSECONDS.toMinutes(t2))
        ));
        System.out.println("The number of clusters are: " + findClusters());
        writeClusters(FileName);
    }

    /*This function prints cluster results and write a hashmap to a file*/
    public void writeClusters(String data) throws IOException {
        /*Printing the clusters*/
        ArrayList<Integer> clusterElements = new ArrayList<>();
        TreeSet<String> checkSet = new TreeSet<>();
        Map<String, Integer> fileMap = new LinkedHashMap<>();
        System.out.println("************* Cluster Results ************");
//        String line;
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("/home/deepak/freelance/nodes/ClusterResults/R-MCL and MCL test/" + FileName + "MCLResults.txt"))) {
        try (PrintWriter writer = new PrintWriter(new File(Utils.getFilePath() + "/MCLAlgorithm/ClusterResults/R-MCL and MCL test/" + FileName + "MCLResults.txt"))) {
            int clusterNumber = 0, clusters = 0, numberKeys = 0;
            for (int i = 0; i < dimensions; i++) {

                for (int j = 0; j < dimensions; j++) {
                    double check = transMatrix.get(i).c[j];
                    if (check != 0) {
                        clusterElements.add(j);
                    }
                }

                if (clusterElements.size() > 0) {
                    clusters = 0;
                    Set<String> keySet = new TreeSet<>();
                    for (Integer e : clusterElements) {
                        for (String key : stringToID.keySet()) {
                            int value = stringToID.get(key);
                            if (value == e) {
                                if (!checkSet.contains(key)) {
                                    keySet.add(key);
//                                    System.out.print(key + ",");
                                    fileMap.put(key, clusterNumber);
                                }

                                checkSet.add(key);
                                numberKeys++;
                            }
                        }
                    }
                    if (!keySet.isEmpty()) {
                        writer.println("Cluster Number " + (++clusterNumber) + ":" + keySet);
                        System.out.println("Cluster Number " + clusterNumber + ":" + keySet);
                    }
                }
                clusters++;
                clusterElements.clear();
            }

            /*write to hashmap*/
            rwHash.writeHashMap((LinkedHashMap) fileMap);
        }
    }

    /*find the clusters*/
    public int findClusters() throws IOException {
        /*Intepret the file from /nodes/ClusterResults/*/
//        File f = new File("/home/deepak/freelance/nodes/ClusterResults/" + "clusterResults.txt");
        File f = new File(Utils.getFilePath() + "/MCLAlgorithm/ClusterResults/clusterResults.txt");
        if (!f.exists()) {
            f.createNewFile();
        }

        FileOutputStream fs = new FileOutputStream(f);
        int count;
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fs))) {
            count = 0;
            for (int i = 0; i < dimensions; i++) {
                for (int j = 0; j < dimensions; j++) {
                    if (transMatrix.get(i).c[j] != 0) {
                        count++;
                        break;
                    }
                }
                for (int j = 0; j < dimensions; j++) {
                    if (transMatrix.get(i).c[j] != 0) {
                        out.write(Integer.toString(j) + "\t" + Integer.toString(count) + "\n");
                    }
                }
            }
        }
        return count;
    }

}
