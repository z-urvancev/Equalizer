package com.example.nirs;

public class Filter implements Processable {

    private int      order;
    private double[] coefs;

    private int          smplOnce;
    private CircularBuffer buffer;

    public Filter(int order, double[] coefs, int smplOnce) {
        this.smplOnce = smplOnce;
        this.order = order;
        this.buffer = new CircularBuffer(smplOnce * 4, smplOnce, 2);
        this.coefs = new double[order + 1];
        System.arraycopy(coefs, 0, this.coefs, 0, coefs.length);
    }

    @Override
    public short[] process(short[] samples) {
        buffer.putQueue(samples);
        return filtering();
    }

    private short[] filtering() {
        double[] mult = new double[2 * smplOnce]; // 2 channels

        for (int i = 0; i < mult.length; i++) {
            for (int j = 0; j < coefs.length; j++) {
                mult[i] += buffer.get(i + j * 2) * coefs[j];
            }
        }

        short[] res = new short[mult.length];
        for (int i = 0; i < res.length; i++)
            res[i] = (short) mult[i];

        return res;
    }
}
