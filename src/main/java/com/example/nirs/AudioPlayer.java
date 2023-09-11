package com.example.nirs;

import javafx.scene.chart.XYChart;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handles all audio performance
 * **/
public class AudioPlayer {

    private SourceDataLine   sdl;
    private AudioInputStream ais;
    private AudioFormat   format;

    /**
     * Possible SAMPLE_ONCE values:
     * 4096
     * 8192
     * 16384
     * 32768
     * 65536
     * **/

    /** Size of circular buffer **/
    private static final int SAMPLES_ONCE = 16384;
    private static final int    BUFF_SIZE = SAMPLES_ONCE * 4;
    private CircularBuffer buffer;
    private CircularBuffer equalizerResult;

    /** FFT **/
    private FFT  inputSignal;
    private FFT outputSignal;

    private Processable equalizer;
    private Processable chorus;
    private Processable clipping;

    private Processable distortion;

    private boolean enableEqualizer = true;
    private boolean enableChorus    = false;
    private boolean enableClipping  = false;
    private boolean enableDistortion = false;
    private boolean enableGraphics  = false;

    private boolean paused = true;
    private boolean ended  = false;

    private Thread equalizerThread;

    public AudioPlayer(File musicFile) {
        try {
            if (musicFile != null) {
                ais = AudioSystem.getAudioInputStream(musicFile);
                sdl = AudioSystem.getSourceDataLine(format);
                sdl.flush();
                format = ais.getFormat();
            }

            inputSignal  = new FFT(SAMPLES_ONCE);
            outputSignal = new FFT(SAMPLES_ONCE);

            equalizer = new Equalizer(SAMPLES_ONCE);
            chorus    = new ChorusEffect(SAMPLES_ONCE);
            clipping  = new ClippingEffect();
            distortion = new DistortionEffect(5);

            buffer = new CircularBuffer(BUFF_SIZE, SAMPLES_ONCE, 2, 2);
            equalizerResult = new CircularBuffer(SAMPLES_ONCE * 4, SAMPLES_ONCE, 2, 1);

            equalizerThread = new Thread(this::equalizerWork);
            equalizerThread.start();

        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.out.println(e.getMessage());
        }
    }

    public void work() {
        try {

            sdl.open(format);
            sdl.start();
            paused = true;

            byte[]   readBytes = new  byte[SAMPLES_ONCE * 4];
            short[]     sample = new short[SAMPLES_ONCE * 2];
            boolean putSuccess = true;
            int     readStatus = 0;

            while (true) {
                if (paused) pause();
                if (ended) {
                    close();
                    if (equalizer != null)
                        ((Equalizer)equalizer).close();

                    return;
                }

                if (putSuccess) {
                    readStatus = ais.read(readBytes, 0, SAMPLES_ONCE * 4);
                    sample = makeSamplesFromBytes(readBytes);
                }

                if (readStatus == -1) {
                    endWork();
                    pause();
                    break;
                }

                inputSignal.put(sample);
                if (inputSignal.isEvaluated())
                    inputSignal.setEvaluated(false);

                putSuccess = buffer.put(sample);

                if (equalizerResult.pull(sample)) {

//                    if (enableEqualizer)
//                        sample = equalizer.evaluate(sample);
//                    else
//                        equalizer.evaluate(sample);

                    if (enableChorus)
                        sample = chorus.process(sample);
                    else
                        chorus.process(sample);

                    if (enableClipping)
                        sample = clipping.process(sample);

                    if (enableDistortion)
                        sample = distortion.process(sample);


                    outputSignal.put(sample);
                    if (outputSignal.isEvaluated())
                        outputSignal.setEvaluated(false);

                    sdl.write(makeBytesFromSamples(sample), 0, SAMPLES_ONCE * 4);
                }
            }

            sdl.drain();
            sdl.close();

        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        paused = false;
    }

    public void stop() {
        paused = true;
    }

    private void pause() {
        if (paused) {
            sdl.flush();

            while (true) {
                try {
                    if (!paused) break;
                    Thread.sleep(50);
                    if (ended) return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        if (this.ais != null) {
            try {
                this.ais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.sdl != null) {
            this.sdl.close();
        }
    }

    private void equalizerWork() {
        short[] samples = new short[SAMPLES_ONCE * 2];

        while (true) {
            if (ended) return;

            if (buffer.pull(samples)) {
                if (enableEqualizer)
                    equalizerResult.put(equalizer.process(samples));
                else
                    equalizerResult.put(samples);
            }
        }
    }

    public void chartWork(XYChart.Data<Number, Number>[] iData1,
                          XYChart.Data<Number, Number>[] iData2,
                          XYChart.Data<Number, Number>[] oData1,
                          XYChart.Data<Number, Number>[] oData2) {
        try {
            while (true) {

                if (enableGraphics) {
                    if (!inputSignal.isEvaluated()) {
                        inputSignal.evaluate();
                        chart(iData1, iData2, inputSignal);
                        inputSignal.setEvaluated(true);
                    }

                    if (!outputSignal.isEvaluated()) {
                        outputSignal.evaluate();
                        chart(oData1, oData2, outputSignal);
                        outputSignal.setEvaluated(true);
                    }
                }

                Thread.sleep(100);
                if (ended) return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void chart(XYChart.Data<Number, Number>[] data1,
                       XYChart.Data<Number, Number>[] data2, FFT fft) {

        double[] result = fft.getResult();
        int size = result.length / 4;

        for (int i = 0; i < size; i++) {
            data1[i].setYValue((Math.log10(result[i * 2 + size    ]) - 1) / 2);
            data2[i].setYValue((Math.log10(result[i * 2 + size + 1]) - 1) / 2);
        }
    }


    private short[] makeSamplesFromBytes(byte[] src) {
        short[] buff = new short[src.length / 2];
        for (int i = 0; i < buff.length; i++)
            buff[i] = ByteBuffer.wrap(src, i * 2, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();

        return buff;
    }

    private byte[] makeBytesFromSamples(short[] src) {
        byte[] buff = new byte[src.length * 2];
        for (int i = 0; i < src.length; i++) {
            buff[2*i  ] = (byte) (src[i]);
            buff[2*i+1] = (byte) (src[i] >>> 8);
        }

        return buff;
    }

    public void endWork() {
        ended = true;
    }

    public void setGain(int index, double value) {
        ((Equalizer)equalizer).setGain(index, value);
    }

    public void setBound(short bound) {
        ((ClippingEffect)clipping).setBound(bound);
        ((DistortionEffect)distortion).setBound(bound);
    }

    public int getNumOfBands() {
        return Equalizer.getNumOfFilters();
    }

    public static int getSamplesOnce() {
        return SAMPLES_ONCE;
    }

    public void setEnableEqualizer(boolean enableEqualizer) {
        this.enableEqualizer = enableEqualizer;
    }

    public void setEnableChorus(boolean enableChorus) {
        this.enableChorus = enableChorus;
    }

    public void setEnableClipping(boolean enableClipping) {
        this.enableClipping = enableClipping;
    }


    public void setEnableDistortion(boolean enableDistortion) {
        this.enableDistortion = enableDistortion;
    }

    public void setEnableGraphics(boolean enableGraphics) {
        this.enableGraphics = enableGraphics;
    }
}
