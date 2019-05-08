package reso.examples.selectiverepeat;

import reso.common.Message;
import reso.ip.*;

import java.util.*;

public class SelectiveRepeatProtocol implements IPInterfaceListener {
    public static final int IP_PROTO_SR = Datagram.allocateProtocolNumber("SR");
    private final IPHost host;
    public IPAddress dst;
    private int sendBase = 0;
    private int receiveBase = 0;
    private int nextSeqNum = 0;
    private int cwnd = 8;
    private double RTO = 3;
    private double RTT = -1;
    private double SRTT = -1;
    private double depTime, arrTime;

    private Queue<SelectiveRepeatMessage> queue = new LinkedList<>(); // Les messages à envoyer
    private ArrayList<SelectiveRepeatMessage> window = new ArrayList<>(); // Les messages reçus, à acquitter

    private Random rand = new Random();

    // Variables pour la gestion de congestion.
    private int MSS = 536;
    private int ssthresh = 5360;

    public SelectiveRepeatProtocol(IPHost host) {
        this.host = host;
    }

    private void transfer() throws Exception {
        for (int i = 0; i < queue.size(); i++) {
            SelectiveRepeatMessage message = queue.poll();
            if (message.seqNum == nextSeqNum) {
                depTime = host.getNetwork().getScheduler().getCurrentTime();
                message.timer = new Timer(host.getNetwork().getScheduler(), RTO, this, message);
                message.timer.start();
                host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, message);
                nextSeqNum++;
            } else
                queue.add(message);
        }
    }

    public void send(SelectiveRepeatMessage message) throws Exception {
        queue.add(message);
        transfer();
    }

    public void timeout(SelectiveRepeatMessage message) throws Exception {
        System.out.println("TIMEOUT "+message.seqNum);
        RTO = RTO * 2;
        message.timer.stop();
        message.timer = new Timer(host.getNetwork().getScheduler(), RTO, this, message);
        message.timer.start();
        host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, message);
        ssthresh = cwnd / 2;
        cwnd = MSS;
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
        Message message = datagram.getPayload();
        if (message instanceof SelectiveRepeatMessage) {
            System.out.print(host.name+": ");
            System.out.println(Arrays.toString(window.toArray()));
            SelectiveRepeatMessage rpkt = (SelectiveRepeatMessage) message;
            if (rand.nextInt(4) != 3) {
                rpkt.timer.stop();
                System.out.println("SelectiveRepeatMessage" + (int) (host.getNetwork().getScheduler().getCurrentTime() * 1000) + "ms)" +
                        " host=" + host.name + ", dgram.src=" + datagram.src + ", dgram.dst=" +
                        datagram.dst + ", iif=" + src + ", data=" + rpkt.data + ", seqNum=" + rpkt.seqNum);
                int n = rpkt.seqNum;
                Ack ack = new Ack();
                ack.seqNum = n;
                host.getIPLayer().send(IPAddress.ANY, datagram.src, SelectiveRepeatProtocol.IP_PROTO_SR, ack);
                if (receiveBase <= n && n < receiveBase + cwnd) {
                    int data = rpkt.data;
                    if (receiveBase == n) {
                        Receiver.dataList.add(data);
                        receiveBase++;
                        for (SelectiveRepeatMessage m : window) {
                            if (m.seqNum == receiveBase){
                                Receiver.dataList.add(m.data);
                                window.remove(m);
                                receiveBase++;
                            }
                        }
                    } else {
                        window.add(rpkt);
                    }
                }
            } else {
                System.out.println("Perte de packet numéro : " + rpkt.seqNum);
            }

        } else if (message instanceof Ack) {
            Ack rpkt = (Ack) message;
            arrTime = (host.getNetwork().scheduler.getCurrentTime());
            double r = arrTime - depTime;
            float alpha = 0.125f;
            float beta = 0.25f;
            SRTT = (SRTT == -1) ? r : (1 - alpha) * SRTT + alpha * (r);
            RTT = (RTT == -1) ? r / 2 : (1 - beta) * RTT + beta * (Math.abs(SRTT - r));
            RTO = SRTT + 4 * RTT;
            System.out.println("Ack" + (int) (host.getNetwork().getScheduler().getCurrentTime()) * 1000 + "ms)" +
                    " host=" + host.name + ", dgram.src=" + datagram.src + ", dgram.dst=" +
                    datagram.dst + ", iif=" + src + ", AckSequenceNumber=" + rpkt.seqNum + ", ");
            int n = rpkt.seqNum;
            if (sendBase <= n && n < sendBase + cwnd) {
                for (SelectiveRepeatMessage m: window) {
                    if (m.seqNum == sendBase){
                        sendBase++;
                    }
                }

                if (cwnd <= ssthresh)    //slow-start
                    cwnd = cwnd + MSS;
                else                    //congestion avoidance
                    cwnd = cwnd + MSS * (MSS / cwnd);
            }
            transfer();
        }
    }
}
