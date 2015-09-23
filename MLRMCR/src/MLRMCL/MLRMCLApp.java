package MLRMCL;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MLRMCLApp {

    /*File Name*/
    public static String FileName;
    /*Matrix variables*/
    public static int dimensions;
    //public static int clusters;
    public static int numberKeys = 0;
    public static int clusterNumber = 1;
    public static final int thread_count = 50;
    /*Building Matrix*/
    public HashMap<String, Integer> stringToID = new HashMap<>();
    /*Build adjacent matrix*/
    public static Matrix<IntContainer> adjMatrix;
    public static Matrix<IntContainer> init_adjMatrix;
    /*Build transitive matrix*/
    public static Matrix<DoubleContainer> transMatrix;
    public static Matrix<DoubleContainer> buf_transMatrix;
    public static Matrix<DoubleContainer> canonicalTransMatrix;
    public static Matrix<DoubleContainer> conv;
    /*Each iteration after coasening takes long time: threads and multicore processing about regularize(), inflate(), prune(),normalize().*/
    public static MultiProcess multicore[] = new MultiProcess[thread_count];
    /*The number of thread(Processor) is equal to thread_count*/
    public static int counter[] = new int[thread_count];
    static long time1 = 0, time2 = 0;
    static int t_c1, t_c2, m;
    public static double sum[];
    public static double[] columnSum;
    public static int inflate;
    public static Matrix<DoubleContainer> matrix;

    /*Functions*/
    /*convert Hashmap to adjacent matrix*/
    public void convertToGraph(String input, HashMap<String, Integer> idMap) throws FileNotFoundException {
        // try (PrintWriter graph = new PrintWriter(new File("Graph.txt"))) {
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
        time2 = System.currentTimeMillis();
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
        System.out.println(FileName);
        try (Scanner inFile = new Scanner(new FileReader("./src/MLRMCL/resource/" + FileName))) {
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

        System.out.println("Number of edges: " + numberOfEdges + "  Number of vertices: " + dimensions);
        /*Container (memory issues)*/
        adjMatrix = new Matrix<>(IntContainer.class, dimensions, dimensions, new IntContainer(id));
        try (Scanner data_in = new Scanner(new FileReader("./src/MLRMCL/resource/" + FileName))) {
            time1 = System.currentTimeMillis();
            for (int i = 0; i < numberOfEdges; i++) {
                input = data_in.nextLine();
                /*get to matrix work*/
                convertToGraph(input, stringToID);
            }
        }
        init_adjMatrix = adjMatrix;
        constructTransitionMatrix();
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
    public static void addSelfLoop() {
        for (int i = 0; i < dimensions; i++) {
            adjMatrix.get(i).c[i] = 1;
        }
    }

    /*Construct the transition matrix for each node and edge 
     **Takes too much time hence also multithreaded*/
    public static void constructTransitionMatrix() {
        columnSum = new double[dimensions];
        transMatrix = new Matrix<>(DoubleContainer.class, dimensions, dimensions, new DoubleContainer(dimensions));
        t_c1 = t_c2 = 0;
        Divided_into_Thread();

        for (m = 0; m < thread_count; m++) {
            t_c2 = t_c1 + counter[m];
            multicore[m] = new MultiProcess(t_c1, t_c2) {
                public void run() {
                    for (int col = count1; col < count2; col++) {
                        for (int row = 0; row < dimensions; row++) {
                            columnSum[col] += adjMatrix.get(row).c[col];
                        }
                    }
                    /*Check the number and convert it into probability*/
                    for (int col = count1; col < count2; col++) {
                        for (int row = 0; row < dimensions; row++) {
                            transMatrix.get(row).c[col] = (double) adjMatrix.get(row).c[col] / columnSum[col];
                        }
                    }
                }
            };
            t_c1 = t_c1 + counter[m];
        }
        for (int k = 0; k < thread_count; k++) {
            multicore[k].start();
        }
        try {
            for (int k = 0; k < thread_count; k++) {
                multicore[k].join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*Convergence to a state where probability is steady such that every column value has the same 
     number. Returns a true/false value*/
    public static boolean isConvergence() {
        if (conv == null) {
            return false;
        }
        //int i = 0,  j = 0;
        for (int j = 0; j < dimensions; j++) {
            for (int i = 0; i < dimensions; i++) {
                /*The difference of new transmatrix and convmatrix is greater*/
                if (Math.abs(transMatrix.get(i).c[j] - conv.get(i).c[j]) > 2e-4) {
                    break;
                }
                if (i == dimensions - 1 && j == dimensions - 1) {
                    return true;
                }
            }
        }

        for (int j = 0; j < dimensions; j++) {
            for (int i = 0; i < dimensions; i++) {
                conv.get(i).c[j] = transMatrix.get(i).c[j];
            }
        }
        return false;
    }

    /*The inflation operator is responsible for both strengthening and weakening of current.
     (Strengthens strong currents, and weakens already weak currents).
     The inflation parameter, r (in this case; variable inflate), controls the extent of this
     strengthening / weakening. (In the end, this influences the granularity of clusters.)*/
    public static void inflate() {
        /*The inflation parameter affects cluster granularity*/
        inflate = 2;
        sum = new double[dimensions];
        /*divide into threads*/
        Divided_into_Thread();
        /* multiplication of two matrices of the same size can be defined by multiplying the corresponding entries 
         and this is known as the Hadamard product.*/
        t_c1 = 0;
        t_c2 = 0;
        for (m = 0; m < thread_count; m++) {
            t_c2 = t_c1 + counter[m];
            multicore[m] = new MultiProcess(t_c1, t_c2) {
                public void run() {
                    for (int j = count1; j < count2; j++) {
                        for (int i = 0; i < dimensions; i++) {
                            sum[j] += Math.pow(transMatrix.get(i).c[j], inflate);
                        }
                    }
                    for (int j = count1; j < count2; j++) {
                        for (int i = 0; i < dimensions; i++) {
                            transMatrix.get(i).c[j] = Math.pow(transMatrix.get(i).c[j], inflate) / sum[j];
                        }
                    }
                }
            };
            t_c1 = t_c1 + counter[m];
        }
        for (int k = 0; k < thread_count; k++) {
            multicore[k].start();
        }
        for (int k = 0; k < thread_count; k++) {
            try {
                multicore[k].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(MLRMCLApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /*Reduces iteration, improves speed: The threshold is one fourth of the average of all the entries in a column. 
     Any value less than that (0.25) will be set to 0 (Assuming they will reach there anyway*/
    public static void prune() {
        /*divide into threads*/
        Divided_into_Thread();
        t_c1 = 0;
        t_c2 = 0;
        for (m = 0; m < thread_count; m++) {
            t_c2 = t_c1 + counter[m];
            multicore[m] = new MultiProcess(t_c1, t_c2) {
                double csum = 0;
                /*Threads will join*/

                public void run() {
                    for (int j = count1; j < count2; j++) {
                        double avg;
                        avg = 0;
                        for (int k = 0; k < dimensions; k++) {
                            avg = avg + transMatrix.get(k).c[j];
                        }
                        avg = avg / dimensions;
                        double threshold = avg / 4;
                        for (int i = 0; i < dimensions; i++) {
                            if (transMatrix.get(i).c[j] < threshold) {
                                transMatrix.get(i).c[j] = 0;
                            }
                        }
                    }
                }
            };
            t_c1 = t_c1 + counter[m];
        }
        for (int k = 0; k < thread_count; k++) {
            multicore[k].start();
        }
        for (int k = 0; k < thread_count; k++) {
            try {
                multicore[k].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(MLRMCLApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        normalise();
    }

    /*Normalise the transitive matrix*/
    public static void normalise() {

        Divided_into_Thread();
        t_c1 = 0;
        t_c2 = 0;
        for (m = 0; m < thread_count; m++) {
            t_c2 = t_c1 + counter[m];
            multicore[m] = new MultiProcess(t_c1, t_c2) {
                double csum = 0;

                @Override
                public void run() {
                    for (int j = count1; j < count2; j++) {
                        csum = 0;
                        for (int k = 0; k < dimensions; k++) {
                            csum = csum + transMatrix.get(k).c[j];
                        }
                        for (int i = 0; i < dimensions; i++) {
                            transMatrix.get(i).c[j] = transMatrix.get(i).c[j] / csum;
                        }
                    }
                }
            };
            t_c1 = t_c1 + counter[m];
        }
        for (int k = 0; k < thread_count; k++) {
            multicore[k].start();
        }
        for (int k = 0; k < thread_count; k++) {
            try {
                multicore[k].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(MLRMCLApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /*Do away with fragementation problem of MCL: MCL uses adjacency structure at the start to initialize the flow and works 
     with the current flow matrix (transitive matrix). Nothing about the algorithm that prevents columns from diverging or differing widely 
     without any penalty. Thus, these columns are manifested as separate clusters when they converge*/
    public static Matrix<DoubleContainer> regularize() {
        matrix = new Matrix<>(DoubleContainer.class, dimensions, dimensions, new DoubleContainer(dimensions));
        Divided_into_Thread();
        t_c1 = 0;
        t_c2 = 0;
        /*No more large diverging to form separate clusters on convergence*/
        for (m = 0; m < thread_count; m++) {
            t_c2 = t_c1 + counter[m];
            multicore[m] = new MultiProcess(t_c1, t_c2) {
                public void run() {
                    /*Regularize = transitive matrix * initial canonical transition matrix Mg*/
                    for (int i = count1; i < count2; i++) {
                        for (int j = 0; j < dimensions; j++) {
                            matrix.get(i).c[j] = 0;
                            for (int k = 0; k < dimensions; k++) {
                                matrix.get(i).c[j] += transMatrix.get(i).c[k] * canonicalTransMatrix.get(k).c[j];// <== binding effect 
                            }
                        }
                    }
                }
            };
            t_c1 = t_c1 + counter[m];
        }
        for (int k = 0; k < thread_count; k++) {
            multicore[k].start();
        }
        for (int k = 0; k < thread_count; k++) {
            try {
                multicore[k].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(MLRMCLApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return matrix;
    }

    /*1st Stage : Matching*/
    private static ArrayList<Integer> maximalMatching() {
        /*Property: if any edge not in result is added, it is no longer a matching 
         result is maximal if it is not a proper subset of any other matchings */
        ArrayList<Integer> result = new ArrayList<>(); //*0<i<V result[result[i]] = i 
        /* if (0,3) is matched pair, result[0] = 3 and result[3] = 0*/
        for (int i = 0; i < dimensions; i++) {  //init to -1 values
            result.add(-1);
        }
        for (int i = 0; i < dimensions; i++) { //<== find pairs of connected vertices
            if (result.get(i) != -1) {
                continue;
            }
            int j;
            for (j = i + 1; j < dimensions; j++) {
                if (result.get(j) != -1) {
                    continue;
                }
                if (adjMatrix.get(i).c[j] == 1) {//<== matched
                    break;
                }
            }
            if (j < dimensions) {
                result.set(i, j);
                result.set(j, i);
            }
        }
        int check = 0;
        int size = result.size();
        for (int i = 0; i < dimensions; i++) {
            check = 0;
            for (int j = 0; j < size; j++) {
                if (i == result.get(j)) {
                    check++;
                }
            }
            /*result[i] = i*/
            if (check == 0) { // <== vertex i has no unmatched adjacent vertices( match itself)
                result.set(i, i);
            }
        }

        return result;
    }

    /*Curtailed RMCL with refinement. RMCL is run for the coarsest graph first */
    public void CurtailedRMCL(int r, int cLevel) throws IOException {
        inflate = r; //<== usually 2 (can play with this parameter
        if (cLevel > 0) {
            int oldDim = dimensions;
            Matrix<IntContainer> oldAdj = adjMatrix;
            List<Integer> match = maximalMatching();
            /* cmap[i] == cmap[match[i]] */
            List<Integer> cmap = new ArrayList<>(); //<== mapping information

            /*Second stage : Mapping 
             After matching, we then represent each pair of vertices as one vertex 
             for example cmap: 311344002 ==> vertices (0,3) is a matched pair so cmap[0] = cmap[3] = 3 
             This means vertices 0 and 3 will be a single vertex in the coarse graph*/
            for (int i = 0; i < oldDim; i++) {
                cmap.add(null);
            }
            dimensions = 0;
            for (int i = 0; i < oldDim; i++) {
                if (cmap.get(i) != null) {
                    continue;
                }
                if (match.get(i) == null) {
                    cmap.add(i, dimensions);
                } else {
                    cmap.add(i, dimensions);
                    cmap.add(match.get(i), dimensions);
                }
                dimensions++;
            }
            Integer[] nodemap1 = new Integer[dimensions];
            Integer[] nodemap2 = new Integer[dimensions];
            for (int i = 0; i < oldDim; i++) {
                int j = cmap.get(i);
                if (nodemap1[j] == null) {
                    nodemap1[j] = nodemap2[j] = i;
                } else {
                    nodemap2[j] = i;
                }
            }
            /*Most important part : coarsening 
             Mapping determines number of vertices.*/
            adjMatrix = new Matrix<>(IntContainer.class, dimensions, dimensions, new IntContainer(dimensions));
            /*Upper bound of edges of Graph 1 is the number of edges in G0*/
            for (int i = 0; i < dimensions; i++) {
                for (int j = i + 1; j < dimensions; j++) {
                    int a1 = nodemap1[i];
                    int a2 = nodemap2[i];
                    int b1 = nodemap1[j];
                    int b2 = nodemap2[j];
                    adjMatrix.get(i).c[j] = 0;
                    if (oldAdj.get(a1).c[b1] > 0) {
                        adjMatrix.get(i).c[j] += oldAdj.get(a1).c[b1];
                    }
                    if (b1 != b2 && oldAdj.get(a1).c[b2] > 0) {
                        adjMatrix.get(i).c[j] += oldAdj.get(a1).c[b2];

                    }
                    if (a1 != a2 && oldAdj.get(a2).c[b1] > 0) {
                        adjMatrix.get(i).c[j] += oldAdj.get(a2).c[b1];
                    }
                    if (a1 != a2 && b1 != b2 && oldAdj.get(a2).c[b2] > 0) {
                        adjMatrix.get(i).c[j] += oldAdj.get(a2).c[b2];
                    }
                    adjMatrix.get(j).c[i] = adjMatrix.get(i).c[j];
                }
            }
            /*Run R-MCL 2 times<== curtailed RMCL*/
            cRMcl();

            Matrix<DoubleContainer> matrix = new Matrix<>(DoubleContainer.class, oldDim, oldDim, new DoubleContainer(oldDim));
            for (int i = 0; i < dimensions; i++) {
                for (int j = 0; j < dimensions; j++) {
                    if (transMatrix.get(i).c[j] != 0) {
                        matrix.get(nodemap1[i]).c[nodemap1[j]] = transMatrix.get(i).c[j];
                        matrix.get(nodemap1[i]).c[nodemap2[j]] = transMatrix.get(i).c[j];
                        if (Objects.equals(nodemap1[i], nodemap2[i])) {
                            continue;
                        }
                        matrix.get(nodemap2[i]).c[nodemap1[j]] = 0;
                        matrix.get(nodemap2[i]).c[nodemap2[j]] = 0;
                    }
                }
            }
            /*Adjacency matrix of the coarse graph*/
            dimensions = oldDim;
            adjMatrix = oldAdj;
            transMatrix = matrix;
            normalise();
        }
        rMcl(); //<== Now lets run R-MCL to final graph until convergence
    }

    /*First iteration of R-MCL prevent getting same convergent flows*/
    public static void cRMcl() throws IOException {
        System.out.println("Program is creating a coarse graph." + "\n" + "Please Wait...");
        addSelfLoop();
        constructTransitionMatrix();
        canonicalTransMatrix = transMatrix;
        int iteration = 1;
        long t1 = System.currentTimeMillis();
        System.out.println("Iteration " + iteration);
        transMatrix = regularize();
        inflate();
        iteration++;
        prune();
        long t2 = System.currentTimeMillis() - t1;
        System.out.print("Finish convergence in: ");
        System.out.println(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(t2),
                TimeUnit.MILLISECONDS.toSeconds(t2)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(t2))
        ));

    }

    /*R-MCL on initial graph*/
    public void rMcl() throws IOException {
        /**/
        System.out.println("Program is running MLRMCL Algorithm...");
        conv = transMatrix;
        canonicalTransMatrix = transMatrix;

        long t1 = System.currentTimeMillis();
        while (!isConvergence()) {
            transMatrix = regularize();
            inflate();
            prune();
        }
        long t2 = System.currentTimeMillis() - t1;
        System.out.print("Finish convergence in: ");
        System.out.println(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(t2),
                TimeUnit.MILLISECONDS.toSeconds(t2)
                - TimeUnit.NANOSECONDS.toSeconds(TimeUnit.NANOSECONDS.toMinutes(t2))
        ));

        Integer clusterNumber = writeClusters(FileName);
        System.out.println("\n");
        System.out.println("Cluster Numbers :" + "The number of clusters are: " + clusterNumber);
        System.out.println("\n");
    }

    /*All nodes divided into each threads*/
    public static void Divided_into_Thread() {
        for (int k = 0; k < thread_count; k++) {
            counter[k] = 0;
        }

        int d = (int) dimensions / thread_count;
        int f = dimensions - d * thread_count;
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < thread_count; j++) {
                counter[j]++;
            }
        }
        for (int k = 0; k < f; k++) {

            counter[k]++;
        }
    }


    /*This function prints cluster results and write a hashmap to a file*/
    public int writeClusters(String fileName) throws IOException {
        /*Printing the clusters*/
        ArrayList<Integer> clusterElements = new ArrayList<>();
        TreeSet<String> checkSet = new TreeSet<>();
        Map<String, Integer> fileMap = new LinkedHashMap();
        readWriteHashMap rwHash = new readWriteHashMap();
        Vector<Integer> cluster = new Vector<>();
        System.out.println("************* Cluster Results ************");
        int clusterNumber = 1, numberKeys = 0;
        double[][] tran = new double[dimensions][dimensions];
        for (int i = 0; i < dimensions; i++) {
            System.arraycopy(transMatrix.get(i).c, 0, tran[i], 0, dimensions);
        }
        try (PrintWriter writer = new PrintWriter("./src/MLRMCL/ClusterResults/R-MCL and MCL test/" + fileName + "MLRMCLResults.txt")) {
            String str = new String();
            String write_str = null;
            for (int i = 0; i < dimensions; i++) {

                for (int j = 0; j < dimensions; j++) {
                    double check = transMatrix.get(i).c[j];
                    if (check != 0) {
                        clusterElements.add(j);
                    }
                }
                if (clusterElements.size() > 0) {
                    for (Integer e : clusterElements) {

                        for (String key : stringToID.keySet()) {
                            int value = stringToID.get(key);
                            if (value == e) {
                                if (!checkSet.contains(key)) {
                                    cluster.add(new Integer(key));
                                    fileMap.put(key, clusterNumber);
                                }
                                checkSet.add(key);
                                numberKeys++;
                            }
                        }
                    }

                    if (!cluster.isEmpty()) {
                        str += cluster.toString();
                        write_str = "Cluster Number " + clusterNumber + " :" + str;
                        System.out.println(write_str);
                        writer.println(write_str);
                        clusterNumber++;
                        str = "";
                    }
                    cluster.clear();

                }
                clusterElements.clear();
            }
        }
        /*write to hashmap*/
        rwHash.writeHashMap((LinkedHashMap) fileMap, FileName);
        return clusterNumber - 1;
    }
}
