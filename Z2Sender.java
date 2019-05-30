import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Vector;
import java.util.Iterator;

class Z2Sender {
    static final int DATAGRAM_SIZE = 50;
    static final int SLEEP_TIME = 500;
    static final int MAX_PACKET = 50;
    InetAddress localHost;
    int destinationPort;
    DatagramSocket socket;
    SenderThread sender = new SenderThread();
    ReceiverThread receiver = new ReceiverThread();
    Vector<TimedDatagramPacket> sent = new Vector<TimedDatagramPacket>();

    public Z2Sender(int myPort, int destPort) throws Exception {
        localHost = InetAddress.getByName("127.0.0.1");
        destinationPort = destPort;
        socket = new DatagramSocket(myPort);
    }

    class TimedDatagramPacket {
        public DatagramPacket packet;
        public float time;
        
        public TimedDatagramPacket(DatagramPacket p) {
            packet = p;
            updateTime();
            
        }
        
        public void updateTime() {
            time = System.currentTimeMillis();
        }
        
        public int getSeq() {
            Z2Packet p = new Z2Packet(packet.getData());
            return p.getIntAt(0);
        }
        
        public boolean isOld() {
            float current = System.currentTimeMillis();
            if(current - time > 10000f) {
                return true;
            }
            return false;
        }
    }
    
    class SenderThread extends Thread {
        boolean eof = false;
        int i = 0;
        public void run() {
            try {
                while (!sent.isEmpty() || !eof) {
                    DatagramPacket packet = next_datagrampacket();
                    if(packet != null) {
                        socket.send(packet);
                    }
                    sleep(SLEEP_TIME);
                }
            } catch (Exception e) {
                System.out.println("Z2Sender.SenderThread.run: " + e);
            }
        }
        
        private DatagramPacket next_datagrampacket() throws java.io.IOException {
            //1. Może w sent jest coś starego
            Iterator<TimedDatagramPacket> it = sent.iterator();
            while (it.hasNext()) {
                TimedDatagramPacket tmp = it.next();
                if(tmp.isOld()) {
                    tmp.updateTime();
                    return tmp.packet;
                }
            }
            
            //2. Skoro nie, to sprawdzić stdin
            if(eof) {
                return null;
            }
            int x = System.in.read();
            if(x < 0) {
                eof = true;
                return null;
            }
            Z2Packet p = new Z2Packet(4 + 1);
            p.setIntAt(i++, 0);
            p.data[4] = (byte)x;
            DatagramPacket packet = new DatagramPacket(p.data, p.data.length, localHost, destinationPort);
            sent.add(new TimedDatagramPacket(packet));
            return packet;
        }
    }

    class ReceiverThread extends Thread {
        public void run() {
            try {
                for (; ; ) {
                    byte[] data = new byte[DATAGRAM_SIZE];
                    DatagramPacket packet = new DatagramPacket(data, DATAGRAM_SIZE);
                    socket.receive(packet);
                    Z2Packet p = new Z2Packet(packet.getData());
                    int seq = p.getIntAt(0);
                    Iterator<TimedDatagramPacket> it = sent.iterator();
                    while (it.hasNext()) {
                        if(it.next().getSeq() == seq) {
                            it.remove();
                        }
                    }
                    //System.out.println("S:" + p.getIntAt(0) + ": " + (char) p.data[4]);
                }
            } catch (Exception e) {
                System.out.println("Z2Sender.ReceiverThread.run: " + e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Z2Sender sender = new Z2Sender(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        sender.sender.start();
        sender.receiver.start();
    }
}
