package reso.examples.selectiverepeat;

import reso.common.AbstractApplication;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPLayer;

public class Sender extends AbstractApplication {

    private final IPLayer ip;
    private final IPAddress dst;
    private int data;

    public Sender(IPHost host, IPAddress dst, int data) {
        super(host, "sender");
        this.dst = dst;
        this.data = data;
        ip = host.getIPLayer();
    }

    public void start() throws Exception {
        SelectiveRepeatProtocol protocol = new SelectiveRepeatProtocol((IPHost) host);
        ip.addListener(SelectiveRepeatProtocol.IP_PROTO_SR, protocol);
        protocol.dst = dst;
        // Sending 20 messages
        for (int i = 0; i < 20; i++) {
            protocol.send(new SelectiveRepeatMessage(data));
        }
    }


    public void stop() {
    }

}