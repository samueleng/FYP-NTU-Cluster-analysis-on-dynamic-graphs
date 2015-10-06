package cluster;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ZoomEvent;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import static org.mentaregex.Regex.match;

public class GraphUtils {

    String clusterLog;
    List<String> logs = new ArrayList<>();
    List<String> xCategories = new ArrayList<>();
    List<String> yCategories = new ArrayList<>();
    
    List<Map<Cluster, List<Cluster>>> clusterDetailsList = new ArrayList<>();
    static DecimalFormat df = new DecimalFormat("#.##");
    //List to store records
    ObservableList<Record> recordList = FXCollections.observableArrayList();

    public class Record 
    {
        private final SimpleStringProperty interval;
        private final SimpleStringProperty parentToChild;
        private final SimpleStringProperty parentNodes;
        private final SimpleStringProperty overlappingNodes;
        private final SimpleStringProperty threshold;

        public Record(String interval, String parentChild, String totalNodes, String commonNodes, String threshold) {
            this.interval = new SimpleStringProperty(interval);
            this.parentToChild = new SimpleStringProperty(parentChild);
            this.parentNodes = new SimpleStringProperty(totalNodes);
            this.overlappingNodes = new SimpleStringProperty(commonNodes);
            this.threshold = new SimpleStringProperty(threshold);
        }

        public String getInterval() {
            return interval.get();
        }

        public String getParentToChild() {
            return parentToChild.get();
        }

        public String getParentNodes() {
            return parentNodes.get();
        }

        public String getOverlappingNodes() {
            return overlappingNodes.get();
        }

        public String getThreshold() {
            return threshold.get();
        }
    }

    public static void main(String[] args) {
        System.out.println("Running ResultReader");
        GraphUtils rr = new GraphUtils();
        List<Map<Integer, List<Integer>>> read = rr.read("0 100,100 200,200 300,300 400,400 500", "MCL", 0);
        rr.readNodes("0 100,100 200,200 300,300 400,400 500", "MCL");
        System.out.println(rr.getNodeTrace("367"));
        System.out.println(rr.getClusterTrace("367"));
    }

    /**
     *
     * @param range Range over which the nodes are read from the dataset file
     * @param algo Selecting result files as per the Algorithm
     */
    private void readNodes(String range, String algo) {
        clusterDetailsList.clear();
        int count, start = 0;
        count = range.split(",").length;
        while (count != 1) {
            String t1 = range.split(",")[start];
            String t2 = range.split(",")[start + 1];
            Map<Cluster, List<Cluster>> map = new LinkedHashMap<>();
            String fileName = "detail_data" + t1 + "_" + t2 + algo + "Results.txt";
            try (Scanner in = new Scanner(getClass().getClassLoader().getResourceAsStream("cluster/resources/" + fileName))) {
                boolean newCluster = true;
                Cluster parentCluster = null, childCluster;
                List<Cluster> clusterList = new ArrayList<>();
                while (in.hasNext()) {
                    String line = in.nextLine();
                    Pattern pattern = Pattern.compile("(\\s\\d+)");
                    String[] match = match(line, "(?<=\\[)([^\\]]+)(?=\\])");
                    if (newCluster) {
                        String[] split = match[0].split(",");
                        List<Integer> nodes = new ArrayList<>();
                        for (String match1 : split) {
                            nodes.add(Integer.valueOf(match1.trim()));
                        }
                        Matcher m = pattern.matcher(line);
                        m.find();
                        parentCluster = new Cluster(new Integer(m.group().trim()), nodes, t1);
                        newCluster = false;
                    } else if (!line.contains("-")) {
                        String[] split = match[0].split(",");
                        List<Integer> nodes = new ArrayList<>();
                        for (String match1 : split) {
                            nodes.add(Integer.valueOf(match1.trim()));
                        }
                        Matcher m = pattern.matcher(line);
                        m.find();
                        childCluster = new Cluster(new Integer(m.group().trim()), nodes, t2);
                        clusterList.add(childCluster);
                        in.nextLine();
                    }
                    if (line.contains("-")) {
                        newCluster = true;
                        List<Cluster> temp = new ArrayList<>();
                        temp.addAll(clusterList);
                        map.put(parentCluster, temp);
                        clusterList.clear();
                    }
                }
            }
            clusterDetailsList.add(map);
            start++;
            count--;
        }
    }

    /**
     *
     * @param range Range over which the nodes are read from the dataset file
     * @param algo Selecting result files per algorithm
     * @param limit Threshold limit
     * @return List of Map containing cluster groupings
     */
    public List<Map<Integer, List<Integer>>> read(String range, String algo, double limit) {
        List<Map<Integer, List<Integer>>> list = new ArrayList<>();

        int count, start = 0;
        @SuppressWarnings("UnusedAssignment")
        int totalParentNodes = 0, commonNodes = 0;
        count = range.split(",").length;
        while (count != 1) {
            String t1 = range.split(",")[start];
            String t2 = range.split(",")[start + 1];
            Map<Integer, List<Integer>> mapping = new LinkedHashMap<>();
            String fileName = "data" + t1 + "_" + t2 + algo + "Results.txt";
            try (Scanner in = new Scanner(getClass().getClassLoader().getResourceAsStream("cluster/resources/" + fileName))) {
                Pattern pattern = Pattern.compile("(\\s\\d+)");
                boolean newCluster = true;
                ArrayList<Integer> clusters = new ArrayList<>();
                Integer parentCluster = 0, childCluster;
                while (in.hasNext()) {
                    String line = in.nextLine();
                    String[] matches = match(line, "/(\\d+)/g"); // => ["11", "22"]
                    if (newCluster) {
                        totalParentNodes = Integer.parseInt(matches[3].trim());
                        Matcher m = pattern.matcher(line);
                        newCluster = false;
                        m.find();
                        parentCluster = new Integer(m.group().trim());
                    } else if (!line.contains("-")) {
                        Matcher m = pattern.matcher(line);
                        m.find();
                        try {
                            commonNodes = Integer.parseInt(matches[3].trim());
                            childCluster = new Integer(m.group().trim());
                        } catch (IllegalStateException ex) {
                            continue;
                        }
                        clusterLog = parentCluster + "==>" + childCluster + "\t";
                        double threshold = getThreshold(totalParentNodes, commonNodes);
                        if (threshold >= limit) {
                            recordList.add(new Record(t1 + "-" + t2, "" + parentCluster + "==>" + childCluster, "" + totalParentNodes, "" + commonNodes, "" + threshold));
                            clusters.add(childCluster);
                        }
                    }
                    if (line.contains("-")) {
                        if (!clusters.isEmpty()) {
                            ArrayList<Integer> temp = new ArrayList<>();
                            temp.addAll(clusters);
                            mapping.put(parentCluster, temp);
                            clusters.clear();
                        }
                        newCluster = true;
                    }
                }
                in.close();
                GraphUtils.writeLogs(logs, fileName);
                logs.clear();
            }
            list.add(mapping);
            start++;
            count--;
        }
        return list;
    }

    /**
     *
     * @return TableView to display grouping of Clusters as per input Range
     */
    public TableView getTableView() {
        //Creating a TableView to display overlapping nodes
        TableView tableView = new TableView();
        tableView.setStyle("-fx-font: 12 arial; -fx-base: #009900;");
        //Creating TableColumns for TableView
        TableColumn interval = new TableColumn("Interval");
        interval.setCellValueFactory(new PropertyValueFactory<>("interval"));
        interval.setMinWidth(60);

        TableColumn parentToChild = new TableColumn("Parent==>Child");
        parentToChild.setCellValueFactory(new PropertyValueFactory<>("parentToChild"));
        parentToChild.setMinWidth(60);

        TableColumn parentNodes = new TableColumn("Total Parent Nodes");
        parentNodes.setCellValueFactory(new PropertyValueFactory<>("parentNodes"));
        parentNodes.setMinWidth(60);

        TableColumn overlappingNodes = new TableColumn("Overlapping Nodes");
        overlappingNodes.setCellValueFactory(new PropertyValueFactory<>("overlappingNodes"));
        overlappingNodes.setMinWidth(60);

        TableColumn threshold = new TableColumn("Threshold(%)");
        threshold.setCellValueFactory(new PropertyValueFactory<>("threshold"));
        threshold.setMinWidth(60);

        //Add record list
        tableView.setEditable(false);
        tableView.getColumns().addAll(interval, parentToChild, parentNodes, overlappingNodes, threshold);
        return tableView;
    }

    /**
     *
     * @return Observable list that is displayed in TableView
     */
    public ObservableList<Record> getTableData() {
        return recordList;
    }

    /**
     *
     * @param totalParentNodes Total number of nodes in Parent Cluster
     * @param overlappingNodes Total number of nodes present in Child Cluster
     * from Parent Cluster
     * @return Percentage of overlappingNodes in totalParentNodes
     */
    private double getThreshold(double totalParentNodes, double overlappingNodes) {
        double threshold = Double.valueOf(df.format((overlappingNodes / totalParentNodes) * 100.0));
        clusterLog += totalParentNodes + "    " + overlappingNodes + "    " + threshold;
        logs.add(clusterLog);
        clusterLog = "";
        return threshold;
    }

    /**
     *
     * @return
     */
    public static String getFilePath() {
        File file = new File("");
        String absolutePath = file.getAbsolutePath();
        return absolutePath + "/src";
    }

    /**
     *
     * @param logs List of Log String to be stored
     * @param fileName File in which logs are maintained
     */
    public static void writeLogs(List<String> logs, String fileName) {
        try {
            try (PrintWriter printer = new PrintWriter(new File(getFilePath() + "/cluster/log/overlappingNodes-" + fileName))) {
                printer.println("parentCluster ==> childCluster" + "    " + "totalParentNodes" + "    " + "overlappingChildNodes" + "    " + "Threshold(%)");
                logs.stream().forEach(printer::println);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param t1 Start of Time Interval
     * @param t2 End of Time Interval
     * @param fileName File from which data is read
     * @return Graph of nodes lying between interval t1 and t2
     */
    public Graph getDynamicGraph(int t1, int t2, String fileName) {
        Graph graph = new MultiGraph("Dynamic Graph", false, true);
        int source, destination, time, duration;

        // generate the graph on the client side
        String style = "node{fill-mode:plain;fill-color:#567;size:6px;}";
        graph.addAttribute("stylesheet", style);
        graph.addAttribute("ui.antialias", true);
        graph.addAttribute("layout.stabilization-limit", 0);
        System.out.println("Making graph for interval " + t1 + " to " + t2);
        Scanner src = new Scanner(getClass().getClassLoader().getResourceAsStream("cluster/resources/" + fileName));
        while (src.hasNext()) {
            String[] line = src.nextLine().split("\\s+");
            System.out.println(Arrays.asList(line));
            source = Integer.parseInt(line[0]);
            destination = Integer.parseInt(line[1]);
            time = Integer.parseInt(line[2]);
            duration = Integer.parseInt(line[3]);
            if (time >= t1 && (time + duration) <= t2) {
                graph.addEdge(source + "-" + destination, source + "", destination + "");
            }
            if (time > t2) {
                break;
            }
        }
        return graph;
    }

    /**
     *
     * @return
     */
    public String getAlgo() {
        Scanner src = new Scanner(System.in);
        String algo = null;
        int algoChoice;
        do {
            System.out.println("Select the clustering Algorithm of your choice");
            System.out.println("1. MCL");
            System.out.println("2. MLRMCL");
            System.out.println("3. ChineseWhispers");
            algoChoice = src.nextInt();
            switch (algoChoice) {
                case 1:
                    algo = "MCL";
                    break;
                case 2:
                    algo = "MLRMCL";
                    break;
                case 3:
                    algo = "CW";
                    break;
                default:
                    System.out.println("Invalid choice try again");
            }
        } while (algoChoice < 1 || algoChoice > 3);
        return algo;
    }

    /**
     *
     * @return
     */
    public String getRange() {
        Scanner src = new Scanner(System.in);
        String range;
        int count;
        do {
            System.out.println("Enter value of compared cluster's Time Intervals to create the Cluster Timeline");
            System.out.println("The Time Intervals must be separated by comma(,) with no whitespace after comma");
            System.out.println("Ex. 20 50,51 100");
            range = src.nextLine();
            count = range.split(",").length;
            if (count <= 1) {
                System.out.println("Please enter more than one Time Interval of Compare Cluster to create the Cluster Timeline");
            }
        } while (count <= 1);
        return range;
    }

    /**
     *
     * @param range Range of Time Interval on which Graph is to be drawn
     * @param algo Dataset set of Algorithm to be read
     * @param threshold Limiting value to display nodes
     * @return A LineChart depicting relationship between various clusters over
     * the interval
     */
    public LineChart getLineChart(String range, String algo, double threshold) {
        recordList.clear();
        xCategories.clear();
        yCategories.clear();
        int maxClusterNumber = Integer.MIN_VALUE;
        int start = 0;
        
        //defining the axes
        CategoryAxis xAxis = new CategoryAxis();
        CategoryAxis yAxis = new CategoryAxis();
        xAxis.setLabel("Time Interval");
        yAxis.setLabel("Cluster Number");

        //creating the chart
        LineChart<String, String> lineChart = new LineChart<>(xAxis, yAxis);
        
        lineChart.setTitle("Clusters Timeline");   
        lineChart.setStyle("-fx-base: #3366CC;");
        lineChart.setPrefHeight(750);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setOnZoom((ZoomEvent e) -> {
            double zoomFactor = e.getZoomFactor();
            System.out.println("ZOOM DATA " + zoomFactor);
        });
        
        List<Map<Integer, List<Integer>>> clusterMap = read(range, algo, threshold);
        readNodes(range, algo);

        for (Map<Integer, List<Integer>> clusterMap1 : clusterMap) {
            if (!clusterMap1.isEmpty()) {
                Set<Integer> keySet = clusterMap1.keySet();
                Integer max = Collections.max(keySet);
                if (maxClusterNumber < max) maxClusterNumber = max;
                
                for (Map.Entry<Integer, List<Integer>> entrySet : clusterMap1.entrySet()) {
                    List<Integer> value = entrySet.getValue();
                    Integer max1 = Collections.max(value);
                    if (maxClusterNumber < max1) {
                        maxClusterNumber = max1;
                    }
                }
            }
        }

        //Set Y-Axis categories
        for (int i = 1; i <= maxClusterNumber; i++) {
            yCategories.add("C" + i);
        }
        yAxis.setCategories(FXCollections.observableList(yCategories));

        //Set X-Axis categories
        String[] split = range.split(",");
        for (String split1 : split) {
            xCategories.add("T" + split1);
        }
        xAxis.setCategories(FXCollections.observableList(xCategories));

        for (Map<Integer, List<Integer>> map : clusterMap) {
            if (!map.isEmpty()) {
                for (Map.Entry<Integer, List<Integer>> entrySet : map.entrySet()) {
                    Integer key = entrySet.getKey();
                    List<Integer> value = entrySet.getValue();
                    for (Integer value1 : value) {
                        XYChart.Series series = new XYChart.Series();
                        series.getData().add(new XYChart.Data("T" + range.split(",")[start], "C" + (key)));
                        series.getData().add(new XYChart.Data("T" + range.split(",")[(start + 1)], "C" + (value1)));
                        lineChart.getData().addAll(series);
                    }
                }
            }
            start++;
        }
        return lineChart;
    }
    
    // this function is the get the x Category count 
    public int getXCategoryCount(){
        return xCategories.size();
    }
    
    // this function is the get the y Category count
    public int getYCategoryCount(){
        return yCategories.size();
    }

    /**
     *
     * @param node Node to be traced
     * @return List of index position that corresponds to node in the LineChart
     */
    public List<Integer> getNodeTrace(String node) {
        List<Integer> nodeIndexList = new ArrayList<>();
        for (Map<Cluster, List<Cluster>> clusterDetailsList1 : clusterDetailsList) {
            for (Map.Entry<Cluster, List<Cluster>> entrySet : clusterDetailsList1.entrySet()) {
                Cluster key = entrySet.getKey();
                List<Cluster> value = entrySet.getValue();
                boolean inParent = key.getNodes().contains(Integer.valueOf(node.trim()));
                if (inParent) {
                    boolean inChild;
                    for (Cluster value1 : value) {
                        inChild = value1.getNodes().contains(Integer.valueOf(node.trim()));
                        if (inChild) {
                            Integer index = getNodeIndex(key, value1);
                            if (index != null) {
                                nodeIndexList.add(index);
                            }
                        }
                    }
                }
            }
        }
        return nodeIndexList;
    }

    List<Cluster> getClusterTrace(String node) {
        List<Cluster> clusterList = new ArrayList<>();
        for (Map<Cluster, List<Cluster>> clusterDetailsList1 : clusterDetailsList) {
            for (Map.Entry<Cluster, List<Cluster>> entrySet : clusterDetailsList1.entrySet()) {
                Cluster key = entrySet.getKey();
                List<Cluster> value = entrySet.getValue();
                boolean inParent = key.getNodes().contains(Integer.valueOf(node.trim()));
                if (inParent) {
                    clusterList.add(key);
                } else {
                    boolean inChild;
                    for (Cluster value1 : value) {
                        inChild = value1.getNodes().contains(Integer.valueOf(node.trim()));
                        if (inChild) {
                            clusterList.add(value1);
                        }
                    }
                }
            }
        }
        return clusterList;
    }

    /**
     *
     * @param parent Parent Cluster
     * @param child Child Cluster
     * @return Position at which the clusterNumber of Parent and Child matches
     * the recordList of TableView
     */
    private Integer getNodeIndex(Cluster parent, Cluster child) {
        int index = 0;
        boolean found = false;
        for (Record record : recordList) {
            if ((record.getInterval().equals(parent.getInterval() + "-" + child.getInterval())) && (record.getParentToChild().equals(parent.getClusterNumber() + "==>" + child.getClusterNumber()))) {
                found = true;
                break;
            }
            index++;
        }
        //To check whether the node is present in the last entry of the table
        if (index <= recordList.size() && found) {
            return index;
        } else {
            return null;
        }
    }

    List<Integer> getNodes(String xValue, String yValue) {
        String interval = xValue.substring(1, xValue.length());
        Integer clusterNumber = Integer.valueOf(yValue.substring(1, yValue.length()));
        List<Integer> nodes = new ArrayList<>();
        for (Map<Cluster, List<Cluster>> map : clusterDetailsList) {
            for (Map.Entry<Cluster, List<Cluster>> entrySet : map.entrySet()) {
                Cluster key = entrySet.getKey();
                List<Cluster> value = entrySet.getValue();
                if ((key.getInterval().equals(interval)) && (Objects.equals(key.getClusterNumber(), clusterNumber))) {
                    return key.getNodes();
                }
                
                for (Cluster cluster : value) {
                    if ((cluster.getInterval().equals(interval)) && (Objects.equals(cluster.getClusterNumber(), clusterNumber))) {
                        return cluster.getNodes();
                    }
                }
            }
        }
        return null;
    }
    
    Cluster getCluster(String xValue, String yValue){
        String interval = xValue.substring(1, xValue.length());
        Integer clusterNumber = Integer.valueOf(yValue.substring(1, yValue.length()));
        List<Integer> nodes = new ArrayList<>();
        for (Map<Cluster, List<Cluster>> map : clusterDetailsList) {
            for (Map.Entry<Cluster, List<Cluster>> entrySet : map.entrySet()) {
                Cluster key = entrySet.getKey();
                List<Cluster> value = entrySet.getValue();
                if ((key.getInterval().equals(interval)) && (Objects.equals(key.getClusterNumber(), clusterNumber))) {
                    return key;
                }
                
                for (Cluster cluster : value) {
                    if ((cluster.getInterval().equals(interval)) && (Objects.equals(cluster.getClusterNumber(), clusterNumber))) {
                        return cluster;
                    }
                }
            }
        }
        return null;
    }
}
