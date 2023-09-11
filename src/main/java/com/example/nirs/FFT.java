package com.example.nirs;

import java.lang.Math;

public class FFT {

    private static final double twoPi = Math.PI * 2;

    private static int  BUFF_SIZE;
    private CircularBuffer buffer;
    private double[]       result;

    private boolean   isEvaluated;

    public FFT(int sampleOnce) {
        BUFF_SIZE = sampleOnce * 2;
        buffer = new CircularBuffer(BUFF_SIZE, sampleOnce, 2);
        result = new double[BUFF_SIZE * 2];
        isEvaluated = true;
    }

    public void put(final short[] sample) {
        buffer.putQueue(sample);
    }

    public void evaluate() {

        short[] input = buffer.getBuffer();
        int size = 8192;
        this.result = new double[size];
        int i, j, n, m, Mmax, Istp;
        double Tmpr, Tmpi, Wtmp, Theta;
        double Wpr, Wpi, Wr, Wi;
        double[] Tmvl;

        n = size * 2;
        Tmvl = new double[n];
        for (i = 0; i < n; i+=2) {
            Tmvl[i] = 0;
            Tmvl[i+1] = input[i/2];
        }

        i = 1; j = 1;
        while (i < n) {
            if (j > i) {
                Tmpr = Tmvl[i]; Tmvl[i] = Tmvl[j];
                Tmvl[j] = Tmpr; Tmpr = Tmvl[i+1];
                Tmvl[i+1] = Tmvl[j+1]; Tmvl[j+1] = Tmpr;
            }
            i = i + 2; m = size;
            while ((m >= 2) && (j > m)) {
                j = j - m; m = m >> 1;
            }
            j = j + m;
        }

        Mmax = 2;
        while (n > Mmax) {
            Theta = -twoPi / Mmax;
            Wpi = Math.sin(Theta);
            Wtmp = Math.sin(Theta / 2);
            Wpr = Wtmp * Wtmp * 2;
            Istp = Mmax * 2; Wr = 1; Wi = 0; m = 1;

            while (m < Mmax) {
                i = m; m = m + 2;
                Tmpr = Wr; Tmpi = Wi;
                Wr = Wr - Tmpr * Wpr - Tmpi * Wpi;
                Wi = Wi + Tmpr * Wpi - Tmpi * Wpr;

                while (i < n) {
                    j = i + Mmax;
                    Tmpr = Wr * Tmvl[j] - Wi * Tmvl[j-1];
                    Tmpi = Wi * Tmvl[j] + Wr * Tmvl[j-1];
                    Tmvl[j] = Tmvl[i] - Tmpr;
                    Tmvl[j-1] = Tmvl[i-1] - Tmpi;
                    Tmvl[i] = Tmvl[i] + Tmpr;
                    Tmvl[i-1] = Tmvl[i-1] + Tmpi;
                    i = i + Istp;
                }
            }
            Mmax = Istp;
        }

        for (i = 0; i < size; i++) {
            j = i * 2;
            result[i] = 2 * Math.sqrt(Math.pow(Tmvl[j],2) + Math.pow(Tmvl[j+1],2)) / size;
        }
    }

    public double[] getResult() {
        return result;
    }

    public boolean isEvaluated() {
        return isEvaluated;
    }

    public void setEvaluated(boolean evaluated) {
        isEvaluated = evaluated;
    }

    public static int getBuffSize() {
        return BUFF_SIZE;
    }
}
