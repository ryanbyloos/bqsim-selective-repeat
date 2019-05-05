package reso.examples.selectiverepeat;

import reso.common.AbstractTimer;
import reso.scheduler.AbstractScheduler;

public class Timer extends AbstractTimer {
    private SelectiveRepeatProtocol protocol;
    private SelectiveRepeatMessage message;

    public Timer(AbstractScheduler scheduler, double interval, SelectiveRepeatProtocol protocol, SelectiveRepeatMessage message) {
        super(scheduler, interval, true);
        this.protocol = protocol;
        this.message = message;
    }

    @Override
    protected void run() throws Exception {
        protocol.timeout(message.seqNum);
    }

}
