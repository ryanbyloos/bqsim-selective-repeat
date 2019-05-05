package Protocol;

import reso.common.Message;
import reso.ip.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class SelectiveRepeatProtocol implements IPInterfaceListener {
    public static final int IP_PROTO_SR= Datagram.allocateProtocolNumber("SR");
    private final IPHost host;
    public IPAddress dst;
    private final int MSS = 536;
    private int ssthresh = 5360;
    private int send_base = 0;
    private int next_seq_num = 0;
    private int recv_base = 0;
    private int windowSize = 8;
    private double RTO = 3;
    private double SRTT = -1;
    private double R;
    private double dep_time;
    private double arrival_time;
    private double RTTVAR = -1;
    private float alpha=0.125f;
    private float beta=0.25f;
    private boolean slow_start=true;
    private boolean congestion_avoidance=false;
    private SelectiveRepeatMessage[] window = new SelectiveRepeatMessage[windowSize];
    private Queue<SelectiveRepeatMessage> queue = new LinkedList<>();
    private Random rand = new Random();


    public SelectiveRepeatProtocol(IPHost host) {
        this.host = host;

    }

    private void slow_start(){
        windowSize =windowSize+MSS;
        if(windowSize>ssthresh){
            slow_start = false;
            congestion_avoidance=true;
        }
        SelectiveRepeatMessage new_window[] = new SelectiveRepeatMessage[windowSize];
        addAll(window, new_window);
        window = new_window;
    }

    private void additive_increase(){
        windowSize=windowSize+(MSS*(MSS/windowSize));
        SelectiveRepeatMessage new_window[] = new SelectiveRepeatMessage[windowSize];
        addAll(window, new_window);
        window = new_window;
    }

    public static void addAll(SelectiveRepeatMessage[] src, SelectiveRepeatMessage[] dst){
        for (int i = 0; i < src.length; i++) {
            dst[i]=src[i];
        }
    }

    public static void keepAll(SelectiveRepeatMessage[] src, SelectiveRepeatMessage[] dst){
        for (int i = 0; i < dst.length ; i++) {
            dst[i] = src[i];
        }
    }

    private void multiplicative_decrease(){
        ssthresh = windowSize/2;
        windowSize = ssthresh;
        congestion_avoidance=true;
        SelectiveRepeatMessage new_window[] = new SelectiveRepeatMessage[windowSize];
        keepAll(window, new_window);
        window = new_window;
    }

    private void transfer() throws  Exception{
        System.out.println("nextSeqNum : "+next_seq_num);
        System.out.println("send_base : " + send_base);
        while(next_seq_num < send_base + windowSize && queue.size() > 0){
            SelectiveRepeatMessage messageToSend = queue.poll();
            messageToSend.sequenceNumber = next_seq_num;
            dep_time = host.getNetwork().getScheduler().getCurrentTime();
            messageToSend.timer = new Timer(host.getNetwork().getScheduler(), RTO, this, messageToSend);
            messageToSend.timer.start();
            window[next_seq_num%windowSize] = messageToSend;
            host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, messageToSend);
            next_seq_num++;
        }
    }

    public void send(SelectiveRepeatMessage message) throws Exception{
        queue.add(message);
        transfer();
    }

    public void timeout(int sequenceNumber) throws Exception{
        System.out.println("timeout");
        RTO= RTO*2;
        window[sequenceNumber%windowSize].timer.stop();
        window[sequenceNumber%windowSize].timer = new Timer(host.getNetwork().getScheduler(), RTO, this, window[sequenceNumber%windowSize]);
        window[sequenceNumber%windowSize].timer.start();
        host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, window[(sequenceNumber)%windowSize]);
        /*ssthresh = windowSize/2;
        windowSize = 1;
        slow_start=true;
        congestion_avoidance=false;
        window = new SelectiveRepeatMessage[windowSize];*/


    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception{
        Message message = datagram.getPayload();
        if(message instanceof SelectiveRepeatMessage){
            SelectiveRepeatMessage rpkt = (SelectiveRepeatMessage) message;
            if(rand.nextInt(4) != 3){
                System.out.println("SelectiveRepeatMessage" + (int) (host.getNetwork().getScheduler().getCurrentTime()*1000) + "ms)" +
                        " host=" + host.name + ", dgram.src=" + datagram.src + ", dgram.dst=" +
                        datagram.dst + ", iif=" + src + ", data=" + rpkt.data + ", sequenceNumber=" + rpkt.sequenceNumber);
                int n = rpkt.sequenceNumber;
                if(recv_base <= n && n < recv_base+windowSize){
                    Ack ack = new Ack();
                    ack.sequenceNumber = n;
                    host.getIPLayer().send(IPAddress.ANY, datagram.src, SelectiveRepeatProtocol.IP_PROTO_SR, ack);
                    int data = rpkt.data;
                    if(recv_base == n){
                        Receiver.datas.add(data);
                        recv_base++;
                        while (window[recv_base%windowSize] != null){
                            Receiver.datas.add(window[recv_base%windowSize].data);
                            window[recv_base%windowSize] = null;
                            recv_base++;
                        }
                    }
                    else{
                        window[n%windowSize]=rpkt;
                    }
                }
                else if(recv_base - windowSize <= n && n<recv_base){
                    Ack ack= new Ack();
                    ack.sequenceNumber = n ;
                    host.getIPLayer().send(IPAddress.ANY, datagram.src, SelectiveRepeatProtocol.IP_PROTO_SR, ack);
                }
            }
            else {
                System.out.println("Perte de packet numéro : " + rpkt.sequenceNumber);
            }

        }
        else if(message instanceof Ack){
            Ack rpkt = (Ack) message;
            if(rand.nextInt(4)!=3){
                arrival_time = (host.getNetwork().scheduler.getCurrentTime());
                R = arrival_time-dep_time;
                if(SRTT == -1) SRTT = R;
                else SRTT = (1-alpha)* SRTT + alpha*(R);
                if(RTTVAR == -1) RTTVAR = R/2;
                else RTTVAR=(1-beta)*RTTVAR+beta*(Math.abs(SRTT -R));
                RTO = SRTT +4*RTTVAR;
                System.out.println("Ack" +  (int)(host.getNetwork().getScheduler().getCurrentTime())*1000 + "ms)" +
                        " host=" + host.name + ", dgram.src=" + datagram.src + ", dgram.dst=" +
                        datagram.dst + ", iif=" + src + ", AckSequenceNumber=" + rpkt.sequenceNumber + ", ");
                int n = rpkt.sequenceNumber;

                if(send_base <= n && n<send_base+windowSize){
                    window[n%windowSize].timer.stop();
                    window[n%windowSize].acked=true;
                    if(n == send_base){
                        while (window[send_base%windowSize].acked){
                            window[send_base%windowSize].acked = false;
                            send_base++;
                        }
                    }
                }
                transfer();
            }
            else {
                System.out.println("perte d'ack n ° : " + rpkt.sequenceNumber);
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
