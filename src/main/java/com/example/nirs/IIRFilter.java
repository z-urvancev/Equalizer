package com.example.nirs;

public class IIRFilter implements Processable {

    private int      order;
    private double[] coefs;

    private double[] coefsFeedback;

    private int          smplOnce;
    private CircularBuffer bufferInput;

    private CircularBuffer bufferFeedback;

    public IIRFilter(int order, double[] coefs, int smplOnce) {
        this.smplOnce = smplOnce;
        this.order = order;
        this.bufferInput = new CircularBuffer(smplOnce * 4, smplOnce, 2);
        this.coefs = new double[order + 1];
        System.arraycopy(coefs, 0, this.coefs, 0, coefs.length);
    }

    @Override
    public short[] process(short[] samples) {
        bufferInput.putQueue(samples);
        return filtering();
    }

    private short[] filtering() {
        double[] mult = new double[2 * smplOnce]; // 2 channels

        for (int i = 0; i < mult.length; i++) {
            for (int j = 0; j < coefs.length; j++) {
                mult[i] += bufferInput.get(i + j * 2) * coefs[j];
            }
            for (int j = 1; j < coefsFeedback.length; j++) {
                mult[i] -= bufferFeedback.get(i + j * 2) * coefsFeedback[j];
            }
        }
        
        short[] res = new short[mult.length];
        for (int i = 0; i < res.length; i++)
            res[i] = (short) mult[i];

        bufferFeedback.putQueue(res);
        return res;
    }
}
