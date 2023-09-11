package com.example.nirs;

import com.example.nirs.coefs.Coefs;

public class Equalizer implements Processable, ISampleQueue {

    /** Enum for threads handling **/
    private enum THREAD {
        EQUALIZER,
        FILTER
    }

    private static final int NUM_OF_FILTERS = 6;
    private static final int THREAD_FILTERS = 2; // Number of filters in one thread

    private int          smplOnce;
    private BandFilter[]  filters;

    private CircularBuffer buffer;
    private short[]    samplesRes;

    private double[]         gain;

    private static final double minGain = Math.pow(10, -70.0 / 20);

    private int    numCalculated; // Number of filters completed calculations
    private boolean      endWork; // Flag: 1 - complete work, 0 - continue work

    public Equalizer(int smplOnce) {

        this.smplOnce = smplOnce;

        filters    = new BandFilter[NUM_OF_FILTERS];
        filters[0] = new BandFilter(this, Coefs.filt1.length - 1, Coefs.filt1, smplOnce);
        filters[1] = new BandFilter(this, Coefs.filt2.length - 1, Coefs.filt2, smplOnce);
        filters[2] = new BandFilter(this, Coefs.filt3.length - 1, Coefs.filt3, smplOnce);
        filters[3] = new BandFilter(this, Coefs.filt4.length - 1, Coefs.filt4, smplOnce);
        filters[4] = new BandFilter(this, Coefs.filt5.length - 1, Coefs.filt5, smplOnce);
        filters[5] = new BandFilter(this, Coefs.filt6.length - 1, Coefs.filt6, smplOnce);

        buffer     = new CircularBuffer(smplOnce * 4, smplOnce, 2);
        samplesRes = new short[NUM_OF_FILTERS * 2 * smplOnce];

        gain       = new double[NUM_OF_FILTERS];
        for (int i = 0; i < NUM_OF_FILTERS; i++)
            gain[i] = 1;

        endWork          = false;
        numCalculated    = 0;

        for (int i = 0; i < NUM_OF_FILTERS / THREAD_FILTERS; i++) {
            int index = i;
            new Thread(()->{
                filterWork(index);
            }).start();
        }
    }

    private void filterWork(int index) {
        BandFilter[] filters = new BandFilter[THREAD_FILTERS];
        for (int i = 0; i < THREAD_FILTERS; i++)
            filters[i] = this.filters[index + i * NUM_OF_FILTERS / THREAD_FILTERS];

        short[][]  smpl = new short[THREAD_FILTERS][2 * smplOnce];

        int[]  memIndex = new int[THREAD_FILTERS];
        for (int i = 0; i < THREAD_FILTERS; i++)
            memIndex[i] = (index + i * NUM_OF_FILTERS / THREAD_FILTERS) * smplOnce * 2;

        while (true) {
            if (endWork) return;

            for (int i = 0; i < THREAD_FILTERS; i++) {
                smpl[i] = filters[i].filtering();
                System.arraycopy(smpl[i], 0, samplesRes, memIndex[i], smpl[i].length);
            }

            report(THREAD.FILTER);
        }
    }

    @Override
    public short[] process(short[] sample) {

        buffer.putQueue(sample);
        report(THREAD.EQUALIZER);

        short[] smpl = new short[2 * smplOnce];
        int memIndex = 0; // Filters results addresses

        for (int i = 0; i < NUM_OF_FILTERS; i++) {
            memIndex = i * smplOnce * 2;
            for (int j = 0; j < smpl.length; j++)
                smpl[j] += samplesRes[memIndex + j] * gain[i];
        }

        return smpl;
    }

    public void close() {
        endWork = true;
    }

    private void report(THREAD who) {
        try {
            switch (who) {

                case EQUALIZER:

                    numCalculated = 0;
                    filtersRoom(THREAD.EQUALIZER);
                    equalizerRoom(THREAD.EQUALIZER);

                    break;

                case FILTER:

                    incNumCalc();
                    if (numCalculated == NUM_OF_FILTERS / THREAD_FILTERS)
                        equalizerRoom(THREAD.FILTER);

                    filtersRoom(THREAD.FILTER);

                    break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setGain(int index, double value) {
        if (value > 1) {
            gain[index] = 1;
            System.out.println(
                    "Error: get wrong slider value.\n" +
                    "Gained: " + value + '\n' +
                    "Replaced with: " + 1);
            return;
        }

        if (value < minGain) {
            gain[index] = minGain;
            System.out.println(
                    "Error: get wrong slider value.\n" +
                    "Gained: " + value + '\n' +
                    "Replaced with: " + minGain);
            return;
        }

        gain[index] = value;
    }

    public static int getNumOfFilters() {
        return NUM_OF_FILTERS;
    }

    /** Functions for handling work time of threads of equalizer and its filters **/
    private synchronized void filtersRoom(THREAD who) throws InterruptedException {
        switch (who) {
            case EQUALIZER -> notifyAll();
            case    FILTER -> wait();
        }
    }

    private synchronized void equalizerRoom(THREAD who) throws InterruptedException {
        switch (who) {
            case EQUALIZER -> wait();
            case    FILTER -> notify();
        }
    }

    @Override
    public short getSample(int index) {
        return buffer.get(index);
    }

    private synchronized void incNumCalc() {
        numCalculated++;
    }
}
