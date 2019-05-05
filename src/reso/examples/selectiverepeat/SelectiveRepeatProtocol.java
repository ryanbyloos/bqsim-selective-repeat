package reso.examples.selectiverepeat;

import reso.common.Message;
import reso.ip.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class SelectiveRepeatProtocol implements IPInterfaceListener {
    public static final int IP_PROTO_SR = Datagram.allocateProtocolNumber("SR");
    private final IPHost host;
    public IPAddress dst;
    private int sendBase = 0;
    private int receiveBase = 0;
    private int nextSeqNum = 0;
    private int windowSize = 8;
    private double RTO = 3;
    private double RTT = -1;
    private double SRTT = -1;
    private double depTime, arrTime;
    private SelectiveRepeatMessage[] window = new SelectiveRepeatMessage[windowSize];
    private Queue<SelectiveRepeatMessage> queue = new LinkedList<>();
    private Random rand = new Random();

//    private final int MSS = 536; // Default values found in the syllabus
//    private int ssThresh = 5360; // Default values found in the syllabus
//    private boolean slow_start=true;
//    private boolean congestion_avoidance=false;

    public SelectiveRepeatProtocol(IPHost host) {
        this.host = host;
    }

//    private void slow_start(){
//        windowSize =windowSize+MSS;
//        if(windowSize>ssthresh){
//            slow_start = false;
//            congestion_avoidance=true;
//        }
//        SelectiveRepeatMessage new_window[] = new SelectiveRepeatMessage[windowSize];
//        addAll(window, new_window);
//        window = new_window;
//    }
//
//    private void additive_increase(){
//        windowSize=windowSize+(MSS*(MSS/windowSize));
//        SelectiveRepeatMessage new_window[] = new SelectiveRepeatMessage[windowSize];
//        addAll(window, new_window);
//        window = new_window;
//    }
//
//    public static void addAll(SelectiveRepeatMessage[] src, SelectiveRepeatMessage[] dst){
//        for (int i = 0; i < src.length; i++) {
//            dst[i]=src[i];
//        }
//    }
//
//    public static void keepAll(SelectiveRepeatMessage[] src, SelectiveRepeatMessage[] dst){
//        for (int i = 0; i < dst.length ; i++) {
//            dst[i] = src[i];
//        }
//    }
//
//    private void multiplicative_decrease(){
//        ssthresh = windowSize/2;
//        windowSize = ssthresh;
//        congestion_avoidance=true;
//        SelectiveRepeatMessage new_window[] = new SelectiveRepeatMessage[windowSize];
//        keepAll(window, new_window);
//        window = new_window;
//    }

    private void transfer() throws Exception {
        System.out.println("nextSeqNum : " + nextSeqNum);
        System.out.println("sendBase : " + sendBase);
        while (nextSeqNum < sendBase + windowSize && queue.size() > 0) {
            SelectiveRepeatMessage message = queue.poll();
            message.seqNum = nextSeqNum;
            depTime = host.getNetwork().getScheduler().getCurrentTime();
            message.timer = new Timer(host.getNetwork().getScheduler(), RTO, this, message);
            message.timer.start();
            window[nextSeqNum % windowSize] = message;
            host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, message);
            nextSeqNum++;
        }
    }

    public void send(SelectiveRepeatMessage message) throws Exception {
        queue.add(message);
        transfer();
    }

    public void timeout(int sequenceNumber) throws Exception {
        System.out.println("timeout");
        RTO = RTO * 2;
        window[sequenceNumber % windowSize].timer.stop();
        window[sequenceNumber % windowSize].timer = new Timer(host.getNetwork().getScheduler(), RTO, this, window[sequenceNumber % windowSize]);
        window[sequenceNumber % windowSize].timer.start();
        host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, window[(sequenceNumber) % windowSize]);
        /*ssthresh = windowSize/2;
        windowSize = 1;
        slow_start=true;
        congestion_avoidance=false;
        window = new SelectiveRepeatMessage[windowSize];*/


    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
        Message message = datagram.getPayload();
        if (message instanceof SelectiveRepeatMessage) {
            SelectiveRepeatMessage rpkt = (SelectiveRepeatMessage) message;
            if (rand.nextInt(4) != 3) {
                System.out.println("SelectiveRepeatMessage" + (int) (host.getNetwork().getScheduler().getCurrentTime() * 1000) + "ms)" +
                        " host=" + host.name + ", dgram.src=" + datagram.src + ", dgram.dst=" +
                        datagram.dst + ", iif=" + src + ", data=" + rpkt.data + ", seqNum=" + rpkt.seqNum);
                int n = rpkt.seqNum;
                if (receiveBase <= n && n < receiveBase + windowSize) {
                    Ack ack = new Ack();
                    ack.seqNum = n;
                    host.getIPLayer().send(IPAddress.ANY, datagram.src, SelectiveRepeatProtocol.IP_PROTO_SR, ack);
                    int data = rpkt.data;
                    if (receiveBase == n) {
                        Receiver.dataList.add(data);
                        receiveBase++;
                        while (window[receiveBase % windowSize] != null) {
                            Receiver.dataList.add(window[receiveBase % windowSize].data);
                            window[receiveBase % windowSize] = null;
                            receiveBase++;
                        }
                    } else {
                        window[n % windowSize] = rpkt;
                    }
                } else if (receiveBase - windowSize <= n && n < receiveBase) {
                    Ack ack = new Ack();
                    ack.seqNum = n;
                    host.getIPLayer().send(IPAddress.ANY, datagram.src, SelectiveRepeatProtocol.IP_PROTO_SR, ack);
                }
            } else {
                System.out.println("Perte de packet numéro : " + rpkt.seqNum);
            }

        } else if (message instanceof Ack) {
            Ack rpkt = (Ack) message;
            if (rand.nextInt(4) != 3) {
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

                if (sendBase <= n && n < sendBase + windowSize) {
                    window[n % windowSize].timer.stop();
                    window[n % windowSize].acked = true;
                    if (n == sendBase) {
                        while (window[sendBase % windowSize].acked) {
                            window[sendBase % windowSize].acked = false;
                            sendBase++;
                        }
                    }
                }
                transfer();
            } else {
                System.out.println("Ack lost, seqNum : " + rpkt.seqNum);
            }


            /*if(slow_start){
                slow_start();
            }
            else if(congestion_avoidance){
                additive_increase();
            }*/
        }

    }
}
