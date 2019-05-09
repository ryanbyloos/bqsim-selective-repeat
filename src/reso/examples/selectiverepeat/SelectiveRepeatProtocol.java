package reso.examples.selectiverepeat;

import reso.common.Message;
import reso.ip.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class SelectiveRepeatProtocol implements IPInterfaceListener {
    public static final int IP_PROTO_SR = Datagram.allocateProtocolNumber("SR");
    private final IPHost host;
    private final double MSS = 3;
    public IPAddress dst;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private int sendBase = 0;
    private int receiveBase = 0;
    private int nextSeqNum = 0;
    private int duplicate = 0;
    private double cwnd = 8;
    private double RTO = 3;
    private double RTTVAR = -1;
    private double SRTT = -1;
    private float alpha = 0.125f;
    private float beta = 0.25f;
    private double arrTime;
    private boolean congestionAvoidance = false; // Slow-start
    private ArrayList<SelectiveRepeatMessage> queue = new ArrayList<>(); // Les messages à envoyer
    private ArrayList<SelectiveRepeatMessage> messages = new ArrayList<>();
    private Random rand = new Random();
    private double ssthresh = 100;

    public SelectiveRepeatProtocol(IPHost host) {
        this.host = host;
    }

    private void refresh() throws Exception {
        for (SelectiveRepeatMessage message : queue) {
            if (nextSeqNum < sendBase + cwnd) {
                if (message.seqNum <= nextSeqNum && !message.sent) {
                    message.depTime = host.getNetwork().getScheduler().getCurrentTime();
                    message.timer = new Timer(host.getNetwork().getScheduler(), RTO, this, message);
                    message.timer.start();
                    System.out.println("Sent : " + message.seqNum);
                    host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, message);
                    messages.add(message);
                    nextSeqNum++;
                    message.sent = true;
                }
            }
        }
        for (SelectiveRepeatMessage message : messages) {
            if (message.acked) {
                if (message.seqNum == sendBase) {
                    sendBase++;
                    System.out.println("Received : " + message.seqNum);
                }
            }
            if (message.seqNum == receiveBase) {
                Receiver.dataList.add(message.data);
                receiveBase++;
            }
        }
    }

    public void send(SelectiveRepeatMessage message) throws Exception {
        queue.add(message);
        refresh();
    }

    public void timeout(SelectiveRepeatMessage message) throws Exception {
        RTO *= 2;
        message.timer.stop();
        message.timer = new Timer(host.getNetwork().getScheduler(), RTO, this, message);
        message.timer.start();
        message.depTime = host.getNetwork().scheduler.getCurrentTime();
        System.out.println("Timeout, resent : " + message.seqNum);
        host.getIPLayer().send(IPAddress.ANY, dst, SelectiveRepeatProtocol.IP_PROTO_SR, message);
        cwnd = MSS;
        ssthresh = cwnd / 2;
        congestionAvoidance = false; // Slow-start
        Demo.printWriter.println(decimalFormat.format(host.getNetwork().getScheduler().getCurrentTime()) + "    " + decimalFormat.format(cwnd));
        refresh();
    }

    @Override
    public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
        Message message = datagram.getPayload();
        if (message instanceof SelectiveRepeatMessage) {
            SelectiveRepeatMessage m = (SelectiveRepeatMessage) message;
            if (rand.nextInt(8) != 3) {
                int n = m.seqNum;
                Ack ack = new Ack();
                ack.seqNum = n;
                ack.expected = receiveBase;
                ack.depTime = m.depTime;
                if (receiveBase <= n && n < receiveBase + cwnd) {
                    if (receiveBase == n) {
                        Receiver.dataList.add(m.data);
                        receiveBase++;
                        ack.expected = receiveBase;
                    } else {
                        messages.add(m);
                    }
                    refresh();
                }
                host.getIPLayer().send(IPAddress.ANY, datagram.src, SelectiveRepeatProtocol.IP_PROTO_SR, ack);
            }
        } else if (message instanceof Ack) {

            Ack ack = (Ack) message;

            arrTime = (host.getNetwork().scheduler.getCurrentTime());
            double r = arrTime - ack.depTime;
            SRTT = (SRTT == -1) ? r : (1 - alpha) * SRTT + alpha * (r);
            RTTVAR = (RTTVAR == -1) ? r / 2 : (1 - beta) * RTTVAR + beta * (Math.abs(SRTT - r));
            RTO = SRTT + 4 * RTTVAR;

            if (ack.expected == sendBase) {
                duplicate++;
            } else
                duplicate = 0;

            if (sendBase <= ack.seqNum && ack.seqNum < sendBase + cwnd) {
                for (SelectiveRepeatMessage m : messages) {
                    if (ack.seqNum == m.seqNum) {
                        m.timer.stop();
                        m.acked = true;
                        refresh();
                    }
                }
            }
            if (duplicate >= 3) {
                ssthresh = cwnd / 2;
                cwnd = ssthresh;
                congestionAvoidance = true;
                duplicate = 0;
                System.out.println("Triple duplicate");
                Demo.printWriter.println(decimalFormat.format(host.getNetwork().getScheduler().getCurrentTime()) + "    " + decimalFormat.format(cwnd));
            } else {
                if (congestionAvoidance) {
                    cwnd += MSS * (MSS / cwnd);
                } else {
                    cwnd += MSS;
                    if (cwnd > ssthresh)
                        congestionAvoidance = true;
                }
                Demo.printWriter.println(decimalFormat.format(host.getNetwork().getScheduler().getCurrentTime()) + "    " + decimalFormat.format(cwnd));
            }
        }
        refresh();
    }
}
