package com.example.nirs;

public class BandFilter {

    private ISampleQueue handler;

    private int      order;
    private double[] coefs;
    private int   smplOnce;

    public BandFilter(ISampleQueue handler, int order, double[] coefs, int smplOnce) {
        this.handler = handler;
        this.smplOnce = smplOnce;
        this.order = order;
        this.coefs = new double[order + 1];
        System.arraycopy(coefs, 0, this.coefs, 0, coefs.length);
    }

    public short[] filtering() {
        double[] mult = new double[2 * smplOnce]; // 2 channels

        for (int i = 0; i < mult.length; i++) {
            for (int j = 0; j < coefs.length; j++) {
                mult[i] += handler.getSample(i + j * 2) * coefs[j];
            }
        }

        short[] res = new short[mult.length];
        for (int i = 0; i < res.length; i++)
            res[i] = (short) mult[i];

        return res;
    }
}
