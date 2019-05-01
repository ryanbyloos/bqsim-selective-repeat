package Protocol;

import reso.common.AbstractTimer;
import reso.common.Message;

public class SelectiveRepeatMessage implements Message {
    public int sequenceNumber;
    AbstractTimer timer;
    public boolean acked;
    public int data;

    public SelectiveRepeatMessage(int data) {
        this.data = data;
    }
    @Override
    public int getByteLength() {
        return Integer.SIZE / 8;
    }
}
