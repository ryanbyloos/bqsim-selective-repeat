package Protocol;
import reso.common.AbstractApplication;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPLayer;

public class Sender extends AbstractApplication
{

    private final IPLayer ip;
    private final IPAddress dst;
    private final int data;

    public Sender(IPHost host, IPAddress dst, int data) {
        super(host, "sender");
        this.dst= dst;
        this.data= data;
        ip= host.getIPLayer();
    }

    public void start() throws Exception {
        SelectiveRepeatProtocol protocol = new SelectiveRepeatProtocol((IPHost) host);
        ip.addListener(SelectiveRepeatProtocol.IP_PROTO_SR, protocol);
        protocol.dst = dst;
        protocol.send(new SelectiveRepeatMessage(data));
    }

    public void stop() {}

}