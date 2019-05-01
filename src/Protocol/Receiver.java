package Protocol;

import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPLayer;

import java.util.ArrayList;

public class Receiver extends AbstractApplication {
    private final IPLayer ip;
    public static ArrayList<Integer> datas = new ArrayList<>();
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
