package Protocol;

import reso.common.AbstractTimer;
import reso.common.Message;

public class SelectiveRepeatMessage implements Message {
    public int seqNum;
    public boolean acked;
    public int data;
    AbstractTimer timer;

    public SelectiveRepeatMessage(int data) {
        this.data = data;
    }

    @Override
    public int getByteLength() {
        return Integer.SIZE / data;
    }
}
