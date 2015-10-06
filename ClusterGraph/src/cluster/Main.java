package cluster;

import cluster.GraphUtils.Record;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
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
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class Main extends Application {

    GraphUtils graphUtils = new GraphUtils();
    String[] args;

    LineChart<String, String> lineChart = null;
    List<Popup> m_labelNodePopupList = new ArrayList<>();
    List<Popup> m_labelEdgePopupList = new ArrayList<>();

    Map<Integer, String> m_styleMap = new HashMap<>();

    Popup m_LabelPopup = null;
    HBox m_LabelhBox = null;
    Label m_detailLabel = null;

    int previousEdge = 0;
    String range, algo, previousColor;
    double threshold;

    boolean startFlag = false;
    boolean newGraphCreated = true;
    static Stage globalStage = null;

    static double CHART_WIDTH = 0;
    static double CHART_HEIGHT = 0;

    @Override
    public void start(Stage stage) throws IOException {

        globalStage = stage;
        //Define a VBox
        VBox vBox = new VBox();
        vBox.setSpacing(5.0);
        vBox.setPadding(new Insets(5, 5, 5, 5));
        stage.getIcons().add(new Image("cluster/styles/icon-cluster.png"));
        //Application Title
        stage.setTitle("A Dynamic Graph Clustering Visual Application"); 
        

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
                //JOptionPane.showMessageDialog(null, "Snapshot created"); 
                ShowMessage("Snapshot created");
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        CheckMenuItem checkMenuNodeDetail = new CheckMenuItem("Node Details"); 
        SetDisplayDetailToNode();
        checkMenuNodeDetail.setOnAction((ActionEvent e) -> {
            if (checkMenuNodeDetail.isSelected()) {
                SetDetailLabelToEdge();
                ObservableList<XYChart.Series<String, String>> lineChartData = lineChart.getData();
                for (XYChart.Series<String, String> series : lineChartData) {
                    ObservableList<XYChart.Data<String, String>> seriesData = series.getData();
                    for (XYChart.Data<String, String> data : seriesData) {
                        DropShadow ds = new DropShadow(20, Color.DARKBLUE);
                        data.getNode().setOnMouseEntered((MouseEvent event) -> {
                            DisplayTheDetailLabelOnNode(data);
                            m_LabelPopup.show(stage);
                        });

                        data.getNode().setOnMouseExited((MouseEvent event) -> {
                            for (Popup m_popup : m_labelNodePopupList) {
                                m_popup.hide();
                            }
                        });

                        data.getNode().setOnMouseClicked((MouseEvent event) -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                for (XYChart.Series<String, String> m_series : lineChartData) {
                                    ObservableList<XYChart.Data<String, String>> m_seriesData = m_series.getData();

                                    for (XYChart.Data<String, String> m_checkData : m_seriesData) {
                                        if (m_checkData.getXValue().equals(data.getXValue()) && m_checkData.getYValue().equals(data.getYValue())) {
                                            for (XYChart.Data<String, String> m_data : m_seriesData) {
                                                DisplayTheDetailLabelOnNode(m_data);
                                                m_LabelPopup.show(stage);
                                            }
                                        }
                                    }
                                }
                            } else if (event.getButton() == MouseButton.SECONDARY) {
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
                            }
                        });
                    }
                }
            } else {
                getNewLineChart(range, algo, threshold);
                RemoveEdgeDetailLabel();
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
                //JOptionPane.showMessageDialog(null, "Please create new Graph to be able to adjust Threshold,Trace and Highlight nodes");
                 ShowMessage("Please create new MCL Graph to be able to adjust Threshold,Trace and Highlight nodes");
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
               // JOptionPane.showMessageDialog(null, "Please create new Graph to enable adjusting Threshold,Trace and Highlight nodes"); 
                ShowMessage("Please create new MLRMCL Graph to be able to adjust Threshold,Trace and Highlight nodes");
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
               // JOptionPane.showMessageDialog(null, "Please create new Graph to enable adjusting Threshold,Trace and Highlight nodes"); 
                ShowMessage("Please create new CW Graph to be able to adjust Threshold,Trace and Highlight nodes");
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
                //JOptionPane.showMessageDialog(null, "Please enter numeric value only"); 
                ShowMessage("Please enter numeric value only");
                textFieldThreshold.requestFocus();
            }
            if (thresholdLimit >= 0) {
                if (newGraphCreated) {
                    getNewLineChart(range, algo, thresholdLimit);
                    vBox.getChildren().remove(1);
                    vBox.getChildren().add(1, lineChart);
                    tableView.setItems(graphUtils.getTableData());
                } else {
                    //JOptionPane.showMessageDialog(null, "Please create new Graph first"); 
                    ShowMessage("Please create new Graph first");
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
                    //JOptionPane.showMessageDialog(null, "Enter valid Range"); 
                    ShowMessage("Enter valid Range");
                }

            } else {
                textFieldRange.setText("");
                textFieldRange.requestFocus();
                //JOptionPane.showMessageDialog(null, "Please select Specify Range"); 
                ShowMessage("Please select Specify Range");
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
                //JOptionPane.showMessageDialog(null, "Please enter node number"); 
                ShowMessage("Please enter node number");
            }
        });

        HBox hBox = new HBox(5);
        hBox.getChildren().addAll(textFieldThreshold, btnThreshold, textFieldRange, btnCreateChart, textTraceNode, btnTraceNode, btnReset);
        
        
        
        vBox.getChildren().addAll(menuBar, lineChart, tableView, hBox);

        //Adding VBox to the scene
        final Scene scene = new Scene(vBox, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("styles/global.css").toExternalForm());

        setSymbolSizeofLineGraph();
        setLineThicknessofLineGraph();
        
        stage.setOnHidden((WindowEvent event)->{
            RemoveEdgeDetailLabel();
        });
        
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setScene(scene);
        stage.show();
    }

    private void SetDisplayDetailToNode() {
        ObservableList<XYChart.Series<String, String>> lineChartData = lineChart.getData();
        for (XYChart.Series<String, String> series : lineChartData) {
            ObservableList<XYChart.Data<String, String>> seriesData = series.getData();
            for (XYChart.Data<String, String> data : seriesData) {
                DropShadow ds = new DropShadow(20, Color.DARKBLUE);
                data.getNode().setOnMouseEntered((MouseEvent event) -> {
                    DisplayTheDetailLabelOnNode(data);
                    m_LabelPopup.show(globalStage);
                });

                data.getNode().setOnMouseExited((MouseEvent event) -> {
                    for (Popup m_popup : m_labelNodePopupList) {
                        m_popup.hide();
                    }
                });

                data.getNode().setOnMouseClicked((MouseEvent event) -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        for (XYChart.Series<String, String> m_series : lineChartData) {
                            ObservableList<XYChart.Data<String, String>> m_seriesData = m_series.getData();

                            for (XYChart.Data<String, String> m_checkData : m_seriesData) {
                                if (m_checkData.getXValue().equals(data.getXValue()) && m_checkData.getYValue().equals(data.getYValue())) {
                                    for (XYChart.Data<String, String> m_data : m_seriesData) {
                                        DisplayTheDetailLabelOnNode(m_data);
                                        m_LabelPopup.show(globalStage);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private void DisplayTheDetailLabelOnNode(XYChart.Data<String, String> data) {
        m_LabelPopup = new Popup();
        m_labelNodePopupList.add(m_LabelPopup);

        m_LabelPopup.setAutoFix(true);
        m_LabelPopup.setAutoHide(true);
        Cluster m_selectedCluster = graphUtils.getCluster(data.getXValue(), data.getYValue());
        String detailData = getDetailInfoOfClusterNode(m_selectedCluster);

        m_detailLabel = new Label(detailData);
        DropShadow ds = new DropShadow(20, Color.DARKGREEN);
        m_detailLabel.setStyle("-fx-background-color: linear-gradient(to bottom, derive(green,90%),derive(green,10%));");
        m_detailLabel.setEffect(ds);

        m_LabelhBox = new HBox();
        m_LabelhBox.getChildren().add(m_detailLabel);

        m_LabelPopup.getContent().add(m_LabelhBox);
        m_LabelPopup.setX(getNodeScreenPoint(data).getX());
        m_LabelPopup.setY(getNodeScreenPoint(data).getY());
    }

    private void SetDetailLabelToEdge() {
        ObservableList<XYChart.Series<String, String>> lineChartData = lineChart.getData();
        int index = 0;
        for (XYChart.Series<String, String> m_series : lineChartData) {
            DisplayTheDetailLabelOnEdge(m_series, index);
            index++;
        }
    }

    private void DisplayTheDetailLabelOnEdge(XYChart.Series<String, String> m_series, int index) {
        m_LabelPopup = new Popup();
        m_labelEdgePopupList.add(m_LabelPopup);

        m_LabelPopup.setAutoFix(true);
        m_LabelPopup.setAutoHide(false); 
        
        
        String detailInfo = getDetailInfoOfEdge(index);
        m_detailLabel = new Label(detailInfo);
        
        DropShadow ds = new DropShadow(5, Color.DARKGREEN);
        m_detailLabel.setStyle("-fx-background-color: linear-gradient(to bottom, derive(gold,90%),derive(gold,10%)); -fx-font: 12px");
        m_detailLabel.setEffect(ds);
        
        String m_oldStyle = m_series.getNode().getStyle();
        
        m_detailLabel.setOnMouseClicked((MouseEvent event)->{
            m_series.getNode().setStyle("-fx-stroke:red; -fx-stroke-width:6;");
        });
        
        m_detailLabel.setOnMouseExited((MouseEvent event)->{
            m_series.getNode().setStyle(m_oldStyle);
        });
        
        m_LabelhBox = new HBox();
        m_LabelhBox.getChildren().add(m_detailLabel);

        m_LabelPopup.getContent().add(m_LabelhBox);
        m_LabelPopup.setX(getEdgeScreenPoint(m_series, index).getX());
        m_LabelPopup.setY(getEdgeScreenPoint(m_series, index).getY());
        m_LabelPopup.setWidth(m_detailLabel.getWidth());
        m_LabelPopup.setHeight(m_detailLabel.getHeight());
        
        m_LabelPopup.show(globalStage);
    }

    private Point2D getEdgeScreenPoint(XYChart.Series<String, String> m_series, int index) {
        Point2D startPos = getNodeScreenPoint(m_series.getData().get(0));
        Point2D endPos = getNodeScreenPoint(m_series.getData().get(1));

        double unit = 0;
        if (index % 2 == 0) {
            unit = 0.6;
        } else {
            unit = 0.3;
        }
        
        unit = 0.3;
        double resultX = startPos.getX() + (endPos.getX() - startPos.getX()) * unit;
        double resultY = startPos.getY() + (endPos.getY() - startPos.getY()) * unit;
        Point2D result = new Point2D(resultX, resultY);
        return result;
    }

    private Point2D getNodeScreenPoint(XYChart.Data<String, String> dest) {
        double height = lineChart.getHeight();
        double width = lineChart.getWidth();

        if (height == 0 || width == 0) {
            height = CHART_HEIGHT;
            width = CHART_WIDTH;
        } else {
            CHART_HEIGHT = height;
            CHART_WIDTH = width;
        }

        int m_xCount = graphUtils.getXCategoryCount();
        int m_yCount = graphUtils.getYCategoryCount();

        double m_xUnit = (width - 40) / ((m_xCount - 1) * 2 + 2);
        double m_yUnit = height / ((m_yCount - 1) * 2 + 2);

        double m_destX = lineChart.getXAxis().toNumericValue(dest.getXValue());
        double m_destY = lineChart.getYAxis().toNumericValue(dest.getYValue());

        double destXpos = m_xUnit * (m_destX * 2 + 1) + 20;
        double destYpos = (height - m_yUnit * (m_destY * 2 + 1));
        Point2D m_point = new Point2D(destXpos, destYpos);
        return m_point;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String getDetailInfoOfClusterNode(Cluster cluster) {
        String m_detailData = new String();
        m_detailData = "ClusterNumber: " + cluster.getClusterNumber() + "\n";
        m_detailData += "Total Nodes: " + cluster.getNodes().size();

        return m_detailData;
    }

    private String getDetailInfoOfEdge(int index) {
        ObservableList<Record> recordList = graphUtils.getTableData();

        String m_detailData = new String();
        m_detailData += " " + recordList.get(index).getOverlappingNodes() + " \n";
        return m_detailData;
    }

    private void setSymbolSizeofLineGraph() {
        ObservableList<XYChart.Series<String, String>> m_lineChartData = lineChart.getData();

        for (XYChart.Series<String, String> series : m_lineChartData) {
            ObservableList<XYChart.Data<String, String>> seriesData = series.getData();

            for (XYChart.Data<String, String> data : seriesData) {
                int m_size = getNodeSize(data);

                if (m_size <= 3) {
                    m_size += 10;
                } else if (m_size >= 50) {
                    m_size -= 10;
                }

                data.getNode().setStyle(" -fx-background-radius: " + m_size + ";\n"
                        + " -fx-padding: " + m_size + ";\n");
            }
        }

    }

    private int getNodeSize(XYChart.Data<String, String> data) {
        Cluster m_cluster = graphUtils.getCluster(data.getXValue(), data.getYValue());
        return m_cluster.getNodes().size();
    }

    private void setLineThicknessofLineGraph() {
        ObservableList<XYChart.Series<String, String>> data = lineChart.getData();
        int index = 0;
        ObservableList<Record> m_recordList = graphUtils.getTableData();
        for (Record m_recodeItem : m_recordList) {
            String m_overLappingNodes = m_recodeItem.getOverlappingNodes();
            data.get(index).getNode().setStyle("-fx-stroke-width:" + m_overLappingNodes + ";");
            index++;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void getNewLineChart(String range, String algorithm, double threshold) {
        previousEdge = 0;
        this.range = range;
        this.threshold = threshold;
        this.algo = algorithm;
        lineChart = graphUtils.getLineChart(range, algo, threshold);
        setSymbolSizeofLineGraph();
        setLineThicknessofLineGraph();
        SetDisplayDetailToNode();
        RemoveEdgeDetailLabel();
        //SetDetailLabelToEdge();
    }

    private void RemoveEdgeDetailLabel() {
        for (Popup m_popup : m_labelEdgePopupList) {
            m_popup.hide();
            //m_labelEdgePopupList.remove(m_popup);
        }
    }

    private void highlightEdge(TableView tableView, LineChart<String, String> lineChart) {
        ObservableList<XYChart.Series<String, String>> data = lineChart.getData();
        if (startFlag) {
            data.get(previousEdge).getNode().setStyle(previousColor);
        } else {
            startFlag = true;
        }

        //To retain previos color if highlighted by different color incase of Node trace
        int index = tableView.getSelectionModel().getSelectedIndex();
        previousColor = data.get(index).getNode().getStyle();
        data.get(index).getNode().setStyle("-fx-stroke:red; -fx-stroke-width:8;");
        previousEdge = tableView.getSelectionModel().getSelectedIndex();
    }

    private void resetGraph() {
        ObservableList<XYChart.Series<String, String>> list = lineChart.getData();
        startFlag = false;
        for (XYChart.Series<String, String> series : list) {
            series.getNode().setEffect(null);
            for (XYChart.Data<String, String> seriesData : series.getData()) {
                seriesData.getNode().setEffect(null);
            }
        }

        previousEdge = 0;
        setSymbolSizeofLineGraph();
        setLineThicknessofLineGraph();
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
            //        JOptionPane.showMessageDialog(null, "No trace found");
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
    private void ShowMessage(String message) {
    EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, message);
        }
    });
}
}
