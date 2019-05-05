package reso.examples.selectiverepeat;

import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPLayer;

import java.util.ArrayList;

public class Receiver extends AbstractApplication {
    public static ArrayList<Integer> dataList = new ArrayList<>();
    private final IPLayer ip;

    public Receiver(IPHost host) {
        super(host, "receiver");
        ip = host.getIPLayer();
    }

    @Override
    public void start() {
        SelectiveRepeatProtocol protocol = new SelectiveRepeatProtocol((IPHost) host);
        ip.addListener(SelectiveRepeatProtocol.IP_PROTO_SR, protocol);
    }

    @Override
    public void stop() {

    }
}
