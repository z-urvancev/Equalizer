package com.example.nirs;

public class DistortionEffect implements Processable {

    private short maxAmplitude;
    private short minAmplitude;
    private double gain;

    private short upperBound;
    private short lowerBound;

    public DistortionEffect(double gain) {
        this.gain = gain;
        upperBound = Short.MAX_VALUE;
        lowerBound = Short.MIN_VALUE;
    }

    //    public DistortionEffect () {
//        super();
//        this.coef = 20.0;
//    }
    @Override
    public short[] process(short[] samples) {
        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            double sample = samples[i];
            double distortedSample = sample * gain;

            if (distortedSample > upperBound) {
                distortedSample = upperBound;
            } else if (distortedSample < lowerBound) {
                distortedSample = lowerBound;
            }

            result[i] = (short) distortedSample;
        }
        return result;
    }

//    @Override
//    public short[] process(short[] samples) {
//        short[] result = new short[samples.length];
//        for (int i = 0; i < samples.length; i++) {
//
//            if (samples[i] > upperBound)
//                result[i] = upperBound;
//            else if (samples[i] < lowerBound)
//                result[i] = lowerBound;
//            else
//                result[i] = samples[i];
//        }
//        return result;
//    }

    public void setDistortionGain(double gain) {
        this.gain = gain;
    }

    public void setLowerBound(short lowerBound) {
        this.lowerBound = (short)-Math.abs(lowerBound);
    }

    public void setUpperBound(short upperBound) {
        this.upperBound = (short)Math.abs(upperBound);
    }


    public void setBound(short bound) {
        setLowerBound(bound);
        setUpperBound(bound);
    }

}