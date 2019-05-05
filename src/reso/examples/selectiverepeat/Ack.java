package reso.examples.selectiverepeat;

import reso.common.Message;

public class Ack implements Message {
    public int seqNum;
    @Override
    public int getByteLength() {
        return 0;
    }
}
