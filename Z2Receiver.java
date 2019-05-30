import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class Z2Receiver {
    static final int DATAGRAM_SIZE = 50;
    InetAddress localHost;
    int destinationPort;
    DatagramSocket socket;

    ReceiverThread receiver = new ReceiverThread();
    HashMap<Integer, Character> received = new HashMap<Integer, Character>();
    int next_to_display = 0;
    

    public Z2Receiver(int myPort, int destPort) throws Exception {
        localHost = InetAddress.getByName("127.0.0.1");
        destinationPort = destPort;
        socket = new DatagramSocket(myPort);
    }

    class ReceiverThread extends Thread {
        public void run() {
            try {
                for (; ; ) {
                    byte[] data = new byte[DATAGRAM_SIZE];
                    DatagramPacket packet =
                            new DatagramPacket(data, DATAGRAM_SIZE);
                    socket.receive(packet);
                    Z2Packet p = new Z2Packet(packet.getData());
                    if(p.getIntAt(0) >= next_to_display)
                        received.put(Integer.valueOf(p.getIntAt(0)), Character.valueOf((char)p.data[4]));
                    print();
                    // WYSLANIE POTWIERDZENIA
                    packet.setPort(destinationPort);
                    socket.send(packet);
                }
            } catch (Exception e) {
                System.out.println("Z2Receiver.ReceiverThread.run: " + e);
            }
        }
        
        private void print() {
            while(received.containsKey(Integer.valueOf(next_to_display))) {
                System.out.println("R:" + next_to_display
                    + ": " + received.get(Integer.valueOf(next_to_display)));
                received.remove(Integer.valueOf(next_to_display));
                next_to_display++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Z2Receiver receiver = new Z2Receiver(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        receiver.receiver.start();
    }
}
