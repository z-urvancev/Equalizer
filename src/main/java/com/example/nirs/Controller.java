package com.example.nirs;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class Controller {

    private boolean isPlaying = false;

    private AudioPlayer aPlayer;

    // Threads
    private Thread audioThread;
    private Thread chartThread;

    @FXML private Slider slider1;
    @FXML private Slider slider2;
    @FXML private Slider slider3;
    @FXML private Slider slider4;
    @FXML private Slider slider5;
    @FXML private Slider slider6;
    private     Slider[] sliders;

    @FXML private Slider clippingSlider;

    @FXML private Button playStopButton;
    @FXML private Button closeButton;
    @FXML private Button resetButton;

    @FXML private CheckBox equalizerEnable;
    @FXML private CheckBox chorusEnable;
    @FXML private CheckBox clippingEnable;
    @FXML private CheckBox distortionEnable;
    @FXML private CheckBox graphicsEnable;

    @FXML private Label musicTitle;

    @FXML private NumberAxis iXAxis, iYAxis, oXAxis, oYAxis;
    @FXML private LineChart<Number, Number>  inputChart, outputChart;
    private XYChart.Data<Number, Number>[] iData1, iData2, oData1, oData2;

    @FXML
    public void initialize() {
        initCharts();
        initSliders();
    }

    @FXML
    public void playStop(ActionEvent e) {
        if (aPlayer != null) {
            if (!isPlaying) {
                isPlaying = true;
                playStopButton.setText("Stop");

                aPlayer.play();
            } else {
                isPlaying = false;
                playStopButton.setText("Play");

                aPlayer.stop();
            }
        }
    }

    @FXML
    public void open(ActionEvent e) {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose the Song");
            fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("AudioFiles", "*.wav"));
            File selected = fc.showOpenDialog(new Stage());

            if (selected == null) return;
            musicTitle.setText(selected.getName());

            aPlayer = new AudioPlayer(selected);

            audioThread = new Thread(()->{
                aPlayer.work();
            });
            audioThread.start();

            chartThread = new Thread(()->{
                aPlayer.chartWork(iData1, iData2, oData1, oData2);
            });
            chartThread.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void resetEqualizer(ActionEvent e) {
        for (int i = 0; i < aPlayer.getNumOfBands(); i++)
            sliders[i].setValue(sliders[i].getMax());
    }

    @FXML
    public void checkBoxEqualizer(ActionEvent e) {
        aPlayer.setEnableEqualizer(equalizerEnable.isSelected());
    }

    @FXML
    public void checkBoxChorus(ActionEvent e) {
        aPlayer.setEnableChorus(chorusEnable.isSelected());
    }

    @FXML
    public void checkBoxClipping(ActionEvent e) {
        aPlayer.setEnableClipping(clippingEnable.isSelected());
    }

    @FXML
    public void checkBoxDistortion(ActionEvent e) {
        aPlayer.setEnableDistortion(distortionEnable.isSelected());
    }

    @FXML
    public void checkBoxGraphics(ActionEvent e) {
        aPlayer.setEnableGraphics(graphicsEnable.isSelected());
        if (!graphicsEnable.isSelected()) {
            for (int i = 0; i < iData1.length; i++) {
                iData1[i].setYValue(0);
                iData2[i].setYValue(0);
                oData1[i].setYValue(0);
                oData2[i].setYValue(0);
            }
        }
    }

    @FXML
    public void closeClick(ActionEvent e) {
        if(aPlayer != null) {
            aPlayer.endWork();
        }

        System.exit(0);
    }

    private void initCharts() {

        int size = 2048;

        /** 2 channels for input and 2 channels for output **/
        XYChart.Series<Number, Number> inputData1  = new XYChart.Series<>();
        XYChart.Series<Number, Number> inputData2  = new XYChart.Series<>();
        XYChart.Series<Number, Number> outputData1 = new XYChart.Series<>();
        XYChart.Series<Number, Number> outputData2 = new XYChart.Series<>();

        iData1 = new XYChart.Data[size];
        iData2 = new XYChart.Data[size];
        oData1 = new XYChart.Data[size];
        oData2 = new XYChart.Data[size];

        for (int i = 0; i < size; i++) {
            iData1[i] = new XYChart.Data<>(((double)44100) * i / size - 22050, (double)0);
            iData2[i] = new XYChart.Data<>(((double)44100) * i / size - 22050, (double)0);
            oData1[i] = new XYChart.Data<>(((double)44100) * i / size - 22050, (double)0);
            oData2[i] = new XYChart.Data<>(((double)44100) * i / size - 22050, (double)0);

            inputData1.getData().add(iData1[i]);
            inputData2.getData().add(iData2[i]);
            outputData1.getData().add(oData1[i]);
            outputData2.getData().add(oData2[i]);
        }

        inputChart.getData().addAll(inputData1, inputData2);
        outputChart.getData().addAll(outputData1, outputData2);

        inputChart.setCreateSymbols(false);
        outputChart.setCreateSymbols(false);
        inputChart.setAnimated(false);
        outputChart.setAnimated(false);

        inputChart.setTitle("INPUT");
        outputChart.setTitle("OUTPUT");

        iYAxis.setLowerBound(-0.3);
        iYAxis.setUpperBound(1.1);
        iYAxis.setAnimated(false);
        iYAxis.setAutoRanging(false);
        oYAxis.setLowerBound(-0.3);
        oYAxis.setUpperBound(1.1);
        oYAxis.setAutoRanging(false);
        oYAxis.setAnimated(false);

        iXAxis.setAutoRanging(false);
        iXAxis.setTickUnit(5000);
        iXAxis.setLowerBound(-25000);
        iXAxis.setUpperBound(25000);
        oXAxis.setAutoRanging(false);
        oXAxis.setTickUnit(5000);
        oXAxis.setLowerBound(-25000);
        oXAxis.setUpperBound(25000);
    }

    private void initSliders() {
        slider1.setValue(slider1.getMax());
        slider2.setValue(slider2.getMax());
        slider3.setValue(slider3.getMax());
        slider4.setValue(slider4.getMax());
        slider5.setValue(slider5.getMax());
        slider6.setValue(slider6.getMax());

        sliders = new Slider[]{
                slider1, slider2, slider3, slider4,
                slider5, slider6
        };

        for (int i = 0; i < Equalizer.getNumOfFilters(); i++) {
            int finalI = i;
            sliders[i].valueProperty().addListener(new ChangeListener<Number>() {
                private final int index = finalI;

                @Override
                public void changed(ObservableValue<? extends Number> observableValue,
                                    Number olaValue, Number newValue) {
                    double value = Math.pow(10, (-70 + newValue.doubleValue() / sliders[index].getMax() * 70) / 20);
                    if (aPlayer != null)
                        aPlayer.setGain(index, value);
                }
            });
        }

        clippingSlider.setValue(clippingSlider.getMax());

        clippingSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue,
                                Number oldValue, Number newValue) {
                if (aPlayer != null)
                    aPlayer.setBound((short)(Short.MAX_VALUE * newValue.shortValue() / 200));
            }
        });
    }

    private void initCheckboxes() {
        equalizerEnable.setSelected(true);
        chorusEnable.setSelected(false);
        graphicsEnable.setSelected(false);
    }
}
