package reso.examples.selectiverepeat;

import reso.common.AbstractTimer;
import reso.common.Message;

public class SelectiveRepeatMessage implements Message {
    public int seqNum;
    public double depTime;
    public int data;
    public boolean acked, sent;
    AbstractTimer timer;

    public SelectiveRepeatMessage(int data, int seqNum) {
        this.data = data;
        this.seqNum = seqNum;
        this.acked = false;
        this.sent = false;
    }

    @Override
    public int getByteLength() {
        return Integer.SIZE / 8;
    }

    @Override
    public String toString() {
        return ("" + seqNum);
    }

}
