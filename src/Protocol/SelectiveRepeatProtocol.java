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
    public int send_base = 0;
    public int next_seq_num = 0;
    public int recv_base = 0;
    public int sequenceNumber = 0;
    private int windowSize = 8;
    public SelectiveRepeatMessage[] window = new SelectiveRepeatMessage[windowSize];
    public Queue<SelectiveRepeatMessage> queue = new LinkedList<>();
    Random rand = new Random();


    public SelectiveRepeatProtocol(IPHost host) {
        this.host = host;
    }
    private void transfer() throws  Exception{
        while(next_seq_num < send_base + windowSize && queue.size() > 0){
            SelectiveRepeatMessage messageToSend = queue.poll();
            messageToSend.sequenceNumber = sequenceNumber;
            messageToSend.timer = new Timer(host.getNetwork().getScheduler(), 5, this, messageToSend);
            messageToSend.timer.start();
            window[sequenceNumber%windowSize] = messageToSend;
            host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, messageToSend);
            next_seq_num++;
            sequenceNumber++;
        }
    }
    public void send(SelectiveRepeatMessage message) throws Exception{
        System.out.println(host);
        System.out.println("send_base : "+send_base);
        System.out.println("next_seq_num : "+next_seq_num);
        System.out.println("recv_base : "+recv_base);
        queue.add(message);
        transfer();
    }
    public void timeout(int sequenceNumber) throws Exception{
        System.out.println("sequenceNumber : " + sequenceNumber);
        window[sequenceNumber%windowSize].timer.start();
        //window[(next_seq_num-send_base)%windowSize].timer.start();
        //host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, window[(next_seq_num-send_base)%windowSize]);
        host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, window[(sequenceNumber)%windowSize]);

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
            System.out.println("Ack" + (int) (host.getNetwork().getScheduler().getCurrentTime()*1000) + "ms)" +
                    " host=" + host.name + ", dgram.src=" + datagram.src + ", dgram.dst=" +
                    datagram.dst + ", iif=" + src + ", AckSequenceNumber=" + rpkt.sequenceNumber + ", ");
            int n = rpkt.sequenceNumber;
            if(send_base<= n && n<send_base+windowSize){
                window[n%windowSize].timer.stop();
                window[n%windowSize].acked=true;
                if(n == send_base){
                    while (window[send_base%windowSize].acked){
                        send_base++;
                    }
                }
            }
            transfer();
        }

    }
}
