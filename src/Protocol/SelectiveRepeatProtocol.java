package Protocol;

import reso.ip.*;

public class SelectiveRepeatProtocol implements IPInterfaceListener {
    public static final int IP_PROTO_SR= Datagram.allocateProtocolNumber("SR");
    private final IPHost host;
    public IPAddress dst;
    public int send_base = 0;
    public int next_seq_num = 0;
    public int recv_base = 0;
    public int sequenceNumber = 0;
    private final int n = 8;
    public SelectiveRepeatMessage[] window = new SelectiveRepeatMessage[n];


    public SelectiveRepeatProtocol(IPHost host) {
        this.host = host;
    }
    public void send(SelectiveRepeatMessage message) throws Exception{
        if(next_seq_num < send_base + n){
            message.timer = new Timer(host.getNetwork().scheduler, 70);
            message.sequenceNumber = sequenceNumber;
            message.timer.start();
            host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, message);
            sequenceNumber++;
        }
    }
    public void timeout(int sequenceNumber) throws Exception{
        window[next_seq_num-send_base].timer.start();
        host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, window[next_seq_num-send_base]);
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {

    }
}
