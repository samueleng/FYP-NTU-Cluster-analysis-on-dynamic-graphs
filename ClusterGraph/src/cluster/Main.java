package cluster;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class Main extends Application {

    GraphUtils graphUtils = new GraphUtils();
    String[] args;

    LineChart<String, String> lineChart = null;

    int previousEdge = 0;
    String range, algo, previousColor;
    double threshold;

    boolean newGraphCreated = true;

    @Override
    public void start(Stage stage) throws IOException {

        //Define a VBox
        VBox vBox = new VBox();
        vBox.setSpacing(5.0);
        vBox.setPadding(new Insets(5, 5, 5, 5));

        //Application Title
        stage.setTitle("Timeline");

        //Choice of algorithm
        algo = graphUtils.getAlgo();

        //Time Interval range
        range = graphUtils.getRange();

        if (range.split(",").length > 1) {
            lineChart = graphUtils.getLineChart(range, algo, 0);
        } else {
            System.out.println("Cluster Timeline can't be plotted due to insufficient Time Intervals");
        }
        TextField textFieldThreshold = new TextField();
        TextField textFieldRange = new TextField();
        TextField textTraceNode = new TextField();

        //Create MenuBar
        MenuBar menuBar = new MenuBar();

        //Add Options Menu
        Menu menuOption = new Menu("Options"); 
        MenuItem snapShotMenuItem = new MenuItem("Take Snapshot");
        snapShotMenuItem.setOnAction((ActionEvent e) -> {
            try {
                //To create snapshot of graph
                WritableImage snapShot = lineChart.snapshot(new SnapshotParameters(), new WritableImage(800, 600));
                ImageIO.write(SwingFXUtils.fromFXImage(snapShot, null), "png", new File("ClusterTimeline " + new SimpleDateFormat("yyyy-MM-dd hhmm").format(new Date()) + ".png"));
                JOptionPane.showMessageDialog(null, "Snapshot created");
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        CheckMenuItem checkMenuNodeDetail = new CheckMenuItem("Node Details");
        checkMenuNodeDetail.setOnAction((ActionEvent e) -> {
            if (checkMenuNodeDetail.isSelected()) {
                ObservableList<XYChart.Series<String, String>> lineChartData = lineChart.getData();
                for (XYChart.Series<String, String> series : lineChartData) {
                    ObservableList<XYChart.Data<String, String>> seriesData = series.getData();
                    for (XYChart.Data<String, String> data : seriesData) {
                        DropShadow ds = new DropShadow(20, Color.DARKBLUE);
                        data.getNode().setOnMouseClicked((MouseEvent event) -> {
                            Popup popup = new Popup();
                            popup.setAutoFix(true);
                            popup.setAutoHide(true);
                            List<Integer> nodes = graphUtils.getNodes(data.getXValue(), data.getYValue());
                            HBox hBox = new HBox();
                            textTraceNode.setText(data.getXValue() + ", " + data.getYValue());
                            for (Integer node : nodes) {
                                Button button = new Button("" + node + " ");
                                button.setStyle("-fx-background-insets: 0, 0, 1, 2;");
                                button.setOnMouseClicked((MouseEvent t) -> {
                                    popup.hide();
                                    traceNode(button.getText());
                                    textTraceNode.setText(textTraceNode.getText() + ": " + button.getText());
                                });
                                hBox.getChildren().add(button);
                            }
                            popup.getContent().add(hBox);
                            popup.setX(event.getScreenX());
                            popup.setY(event.getScreenY());
                            popup.show(stage);
                            data.getNode().setEffect(ds);
                        });
                    }
                }
            } else {
                getNewLineChart(range, algo, threshold);
            }
            vBox.getChildren().remove(1);
            vBox.getChildren().add(1, lineChart);
        });
        menuOption.getItems().addAll(snapShotMenuItem, new SeparatorMenuItem(), checkMenuNodeDetail);

        //Add Algorithms Menu
        Menu menuAlgorithm = new Menu("_" + algo);
        //Create a ToggleGroup to select anyone of the Menu Item
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioMenuItem radioMenuMCL = new RadioMenuItem("MCL");
        radioMenuMCL.setToggleGroup(toggleGroup);
        radioMenuMCL.setOnAction((ActionEvent e) -> {
            if (!this.algo.equals("MCL")) {
                this.newGraphCreated = false;
                JOptionPane.showMessageDialog(null, "Please create new Graph to be able to adjust Threshold,Trace and Highlight nodes");
            } else {
                this.newGraphCreated = true;
            }
            menuAlgorithm.setText("_MCL");
        });
        radioMenuMCL.setToggleGroup(toggleGroup);

        RadioMenuItem radioMenuMLRMCL = new RadioMenuItem("MLRMCL");
        radioMenuMLRMCL.setToggleGroup(toggleGroup);
        radioMenuMLRMCL.setOnAction((ActionEvent e) -> {
            menuAlgorithm.setText("_MLRMCL");
            if (!this.algo.equals("MLRMCL")) {
                this.newGraphCreated = false;
                JOptionPane.showMessageDialog(null, "Please create new Graph to enable adjusting Threshold,Trace and Highlight nodes");
            } else {
                this.newGraphCreated = true;
            }
        });

        radioMenuMLRMCL.setToggleGroup(toggleGroup);
        RadioMenuItem radioMenuCW = new RadioMenuItem("CW");
        radioMenuCW.setToggleGroup(toggleGroup);
        radioMenuCW.setOnAction((ActionEvent e) -> {
            menuAlgorithm.setText("_CW");
            if (!this.algo.equals("CW")) {
                this.newGraphCreated = false;
                JOptionPane.showMessageDialog(null, "Please create new Graph to enable adjusting Threshold,Trace and Highlight nodes");
            } else {
                this.newGraphCreated = true;
            }
        });

        menuAlgorithm.getItems().addAll(radioMenuMCL, radioMenuMLRMCL, radioMenuCW);
        menuBar.getMenus().addAll(menuOption, menuAlgorithm);

        //TableView for display
        TableView tableView = graphUtils.getTableView();
        //Populate the tableView
        tableView.setItems(graphUtils.getTableData());

        tableView.setOnMouseClicked((MouseEvent e) -> {
            highlightEdge(tableView, lineChart);
            if (checkMenuNodeDetail.isSelected()) {
                vBox.getChildren().remove(1);
                vBox.getChildren().add(1, lineChart);
            }
        });
        textFieldThreshold.promptTextProperty().set("Enter Threshold value");

        Button btnThreshold = new Button("Threshold");  
        btnThreshold.setStyle("-fx-font: 12 arial; -fx-base: #009933;");
        btnThreshold.setOnMouseClicked((MouseEvent e) -> {
            double thresholdLimit = 0;
            try {
                thresholdLimit = Double.parseDouble(textFieldThreshold.getText());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Please enter numeric value only");
                textFieldThreshold.requestFocus();
            }
            if (thresholdLimit >= 0) {
                if (newGraphCreated) {
                    getNewLineChart(range, algo, thresholdLimit);
                    vBox.getChildren().remove(1);
                    vBox.getChildren().add(1, lineChart);
                    tableView.setItems(graphUtils.getTableData());
                } else {
                    JOptionPane.showMessageDialog(null, "Please create new Graph first");
                }

            }
        });

        //Take range as input
        textFieldRange.promptTextProperty().set("Ex. 20 50,51 100");
        textFieldRange.tooltipProperty().set(new Tooltip("The Time Intervals must be separated by comma(,) with no whitespace after comma"));

        //Create LineChart based on Input Range
        Button btnCreateChart = new Button("Create Chart"); 
        btnCreateChart.setStyle("-fx-font: 10 arial; -fx-base: #009933;");
        btnCreateChart.setOnMouseClicked((MouseEvent e) -> {
            String inputRange = textFieldRange.getText();
            String algo = menuAlgorithm.getText().substring(1);
            if (inputRange != null) {
                if (textFieldRange.getText().split(",").length > 1) {
                    System.out.println(menuAlgorithm.getText());
                    getNewLineChart(inputRange, algo, threshold);
                    newGraphCreated = true;
                    textFieldRange.setText("");
                    vBox.getChildren().remove(1);
                    vBox.getChildren().add(1, lineChart);
                    tableView.setItems(graphUtils.getTableData());
                    if (checkMenuNodeDetail.isSelected()) {
                        checkMenuNodeDetail.fire();
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Enter valid Range");
                }

            } else {
                textFieldRange.setText("");
                textFieldRange.requestFocus();
                JOptionPane.showMessageDialog(null, "Please select Specify Range");
            }
        });

        //Reset Button
        Button btnReset = new Button("Reset"); 
        btnReset.setStyle("-fx-font: 12 arial; -fx-base: #FF6600;");
        btnReset.setOnMouseClicked((MouseEvent e) -> {
            resetGraph();
            textTraceNode.setText("");
            vBox.getChildren().remove(1);
            vBox.getChildren().add(1, lineChart);
            tableView.setItems(graphUtils.getTableData());
        });

        //Take input to Trace Node
        textTraceNode.promptTextProperty().set("Ex.55");
        textTraceNode.tooltipProperty().set(new Tooltip("Node to Trace in the LineChart"));

        //Button to start Node Trace
        Button btnTraceNode = new Button("Trace"); 
        btnTraceNode.setStyle("-fx-font: 12 arial; -fx-base: #009933;");
        btnTraceNode.setOnMouseClicked((MouseEvent e) -> {
            //To undo the Highlighting done by Node Trace
            String node = textTraceNode.getText().trim();
            if (node != null) {
                traceNode(textTraceNode.getText().trim());
                vBox.getChildren().remove(1);
                vBox.getChildren().add(1, lineChart);
            } else {
                textTraceNode.setText("");
                textTraceNode.requestFocus();
                JOptionPane.showMessageDialog(null, "Please enter node number");
            }
        });
        HBox hBox = new HBox(5);
        hBox.getChildren().addAll(textFieldThreshold, btnThreshold, textFieldRange, btnCreateChart, textTraceNode, btnTraceNode, btnReset);
        vBox.getChildren().addAll(menuBar, lineChart, tableView, hBox);

        //Adding VBox to the scene
        final Scene scene = new Scene(vBox, 750, 700);
        scene.getStylesheets().add(getClass().getResource("styles/global.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void getNewLineChart(String range, String algorithm, double threshold) {
        previousEdge = 0;
        previousColor = "-fx-stroke:black";
        this.range = range;
        this.threshold = threshold;
        this.algo = algorithm;
        lineChart = graphUtils.getLineChart(range, algo, threshold);
    }

    private void highlightEdge(TableView tableView, LineChart<String, String> lineChart) {
        ObservableList<XYChart.Series<String, String>> data = lineChart.getData();
        data.get(previousEdge).getNode().setStyle(previousColor);
        //To retain previos color if highlighted by different color incase of Node trace
        previousColor = data.get(tableView.getSelectionModel().getSelectedIndex()).getNode().getStyle();
        data.get(tableView.getSelectionModel().getSelectedIndex()).getNode().setStyle("-fx-stroke:red;");
        previousEdge = tableView.getSelectionModel().getSelectedIndex();
    }

    private void setAlgo(String alogrithm) {
        this.algo = alogrithm;
    }

    private void resetGraph() {
        ObservableList<XYChart.Series<String, String>> list = lineChart.getData();
        for (XYChart.Series<String, String> series : list) {
            series.getNode().setEffect(null);
            series.getNode().setStyle("-fx-stroke:black");
            for (XYChart.Data<String, String> seriesData : series.getData()) {
                seriesData.getNode().setEffect(null);
            }
        }
        previousColor = "-fx-stroke:black";
        previousEdge = 0;
    }

    private void traceNode(String node) {
        resetGraph();
        DropShadow ds = new DropShadow(20, Color.DARKGREEN);
        List<Integer> nodeTrace = graphUtils.getNodeTrace(node.trim());
        ObservableList<XYChart.Series<String, String>> data = lineChart.getData();
        if (!nodeTrace.isEmpty()) {
            nodeTrace.stream().forEach((index) -> {
                data.get(index).getNode().setEffect(ds);
                data.get(index).getNode().setStyle("-fx-stroke:green;");
            });
        } else {
            JOptionPane.showMessageDialog(null, "No trace found");
        }
        List<Cluster> clusterTrace = graphUtils.getClusterTrace(node);
        if (!clusterTrace.isEmpty()) {
            for (Cluster cluster : clusterTrace) {
                for (XYChart.Series<String, String> series : data) {
                    for (XYChart.Data<String, String> seriesData : series.getData()) {
                        if (seriesData.getXValue().substring(1).equals(cluster.getInterval()) && seriesData.getYValue().substring(1).equals(cluster.getClusterNumber().toString())) {
                            seriesData.getNode().setEffect(ds);
                        }
                    }
                }
            }
        }
    }
}
