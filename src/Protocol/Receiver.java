package Protocol;

import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPLayer;

public class Receiver extends AbstractApplication {
    private final IPLayer ip;
    public Receiver(IPHost host) {
        super(host, "receiver");
        ip= host.getIPLayer();
    }

    @Override
    public void start() throws Exception {
        SelectiveRepeatProtocol protocol = new SelectiveRepeatProtocol((IPHost) host);
        ip.addListener(SelectiveRepeatProtocol.IP_PROTO_SR, protocol);
    }

    @Override
    public void stop() {

    }
}
