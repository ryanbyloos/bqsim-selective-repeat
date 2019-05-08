package reso.examples.selectiverepeat;

import reso.common.AbstractTimer;
import reso.common.Message;

public class SelectiveRepeatMessage implements Message {
    public int seqNum;
    public int data;
    AbstractTimer timer;

    public SelectiveRepeatMessage(int data, int seqNum) {
        this.data = data;
        this.seqNum = seqNum;
    }

    @Override
    public int getByteLength() {
        return Integer.SIZE / data;
    }

    @Override
    public String toString(){
        return (""+seqNum);
    }
}
