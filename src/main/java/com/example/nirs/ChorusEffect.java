package com.example.nirs;


public class ChorusEffect implements Processable {

    private int          smplOnce;
    private CircularBuffer buffer;

    private int             delay[];
    private double   currentValue[];
    private final double     step[] = {0.1, 0.3, 0.5};
    private final int   baseDelay[] = {670, 920, 710};
    private final int  delayRange[] = {440, 510, 340};


    public ChorusEffect(int smplOnce) {
        this.smplOnce = smplOnce;
        buffer        = new CircularBuffer(smplOnce * 4, smplOnce, 2);
        currentValue  = new double[step.length];
        delay         = new int[step.length];
    }

    @Override
    public short[] process(short[] samples) {
        short[] result = new short[smplOnce * 2];
        for (int i = 0; i < step.length; i++) {
            currentValue[i] += step[i];
            if (currentValue[i] > Math.PI)
                currentValue[i] -= Math.PI;
            delay[i] = (int)(Math.sin(currentValue[i]) * delayRange[i]) + baseDelay[i];
        }

        buffer.putQueue(samples);
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
            for (int j = 0; j < step.length; j++)
                result[i] += buffer.get(i + delay[j]) / step.length;
        }

        return result;
    }
}
