package com.example.nirs;

public class CircularBuffer {

    private int samplesOnce; // Number of samples in one channel
    private int BUFF_SIZE;
    private short[] buffer;
    private int head;
    private int tail;
    private int range;

    private boolean isOccupied = false;
    private int      numThreadsWaiting;
    private int      maxThreadsWaiting; // Maximum number of threads waiting in waiting room

    public CircularBuffer(int size, int smplOnce, int numChannels) {
        samplesOnce = smplOnce * numChannels;
        BUFF_SIZE   = size * numChannels;
        buffer = new short[BUFF_SIZE];
        head   = 0;
        tail   = 0;
        range  = 0;

        numThreadsWaiting = 0;
        maxThreadsWaiting = 0;
    }

    public CircularBuffer(int size, int smplOnce, int numChannels, int maxThreads) {
        this(size, smplOnce, numChannels);

        maxThreadsWaiting = maxThreads;
    }

    public synchronized boolean put(short[] source) {
        if (isOccupied)
            waitingRoom(true);

        occupy();

        if (source.length != samplesOnce) {
            System.out.println("Wrong num of samples");
            waitingRoom(false);
            return false;
        }
        if (range >= BUFF_SIZE - samplesOnce) {
            waitingRoom(false);
            return false;
        }

        System.arraycopy(source, 0, buffer, head, samplesOnce);
        head = (head + samplesOnce) % BUFF_SIZE;

        range += samplesOnce;
        waitingRoom(false);

        return true;
    }

    public synchronized boolean pull(short[] target) {
        if (isOccupied)
            waitingRoom(true);

        occupy();

        if (target.length != samplesOnce) {
            System.out.println("Wrong num of samples");
            waitingRoom(false);
            return false;
        }

        if (range <= 0) {
            waitingRoom(false);
            return false;
        }

        System.arraycopy(buffer, tail, target, 0, samplesOnce);
        tail = (tail + samplesOnce) % BUFF_SIZE;

        range -= samplesOnce;
        waitingRoom(false);

        return true;
    }

    public synchronized short[] getBuffer() {
        if (isOccupied)
            waitingRoom(true);

        occupy();

        short[] buff = new short[buffer.length];
        System.arraycopy(buffer, head, buff, 0, BUFF_SIZE - head);
        System.arraycopy(buffer, 0, buff, BUFF_SIZE - head, head);

        waitingRoom(false);

        return buff;
    }

    /** Methods for filtering **/
    public void putQueue(final short[] src) {
        System.arraycopy(src, 0, buffer, head, samplesOnce);
        head = (head + samplesOnce) % BUFF_SIZE;
    }

    public short get(int index) {
        return buffer[(index + head + 1) % BUFF_SIZE];
    }

    private synchronized void occupy() {
        if (isOccupied)
            waitingRoom(true);

        isOccupied = true;
    }

    private synchronized void waitingRoom(boolean goingWait) {
        try {
            if (goingWait) {
                if (numThreadsWaiting >= maxThreadsWaiting)
                    return;
                numThreadsWaiting++;
                wait();
                numThreadsWaiting--;
            }
            else {
                isOccupied = false;
                notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
