import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// SALTP (Secure Application Level Transport Protocol)

public class SaltyProtocol {

    DatagramSocket sender;
    int port;
    InetAddress IP;
    private int seq;
    private int lastAck;
    private final int payloadSize;
    public final int HEADER_LEN = 14;
    public final int SEQ_SIZE = 2;
    public final int ACK_SIZE = 2;

    public SaltyProtocol(String hostname, int port, int payloadSize) throws SocketException, UnknownHostException {
        this.sender = new DatagramSocket();
        this.port = port;
        this.IP = InetAddress.getByName(hostname);
        this.payloadSize = payloadSize;
    }

    public void test(byte[] bytes) throws IOException {
        for (byte[] packet : this.fragment(bytes, payloadSize)) {
            byte[] pack = encode(packet, (byte) 0);
            byte[] unpack = decode(pack);
            System.out.println(pack.length + " packed from encode(): " + Arrays.toString(pack));
            System.out.println(unpack.length + " unpacked from decode():" + Arrays.toString(unpack));
            System.out.println("decoded string: " + new String(unpack, StandardCharsets.UTF_8));
        }
    }

    /*
    salty packet
    16 bit seq
    16 bit ack
    8 bit flag
    32 bit data checksum
    8 bit padding
    this
    header: 14 bytes header


    flags:
        0: SYN synchronize
        1: ACK acknowledgment
        2: FIN close connection
        3: undefined
        4: undefined
        5: undefined
        6: undefined
        7: RST reset seq count
     */

    int calculate_crc(byte[] bytes) {
        int i;
        int crc_value = 0;
        for (byte aByte : bytes) {
            for (i = 0x80; i != 0; i >>= 1) {
                if ((crc_value & 0x8000) != 0) {
                    crc_value = (crc_value << 1) ^ 0x8005;
                } else {
                    crc_value = crc_value << 1;
                }
                if ((aByte & i) != 0) {
                    crc_value ^= 0x8005;
                }
            }
        }
        return crc_value;
    }


    private byte[] encode(byte[] data, byte flags) {
        seq += 1;
        ByteBuffer packet = ByteBuffer.allocate(payloadSize);
        packet.put(ByteBuffer.allocate(SEQ_SIZE).putInt(seq).array(), 0, 4);
        packet.put(ByteBuffer.allocate(ACK_SIZE).putInt(lastAck).array(), 0, 4);
        packet.put(flags);
        packet.put(ByteBuffer.allocate(4).putInt(calculate_crc(data)).array(), 0, 4);
        packet.put((byte) 0);
        packet.put(data);
        return packet.array();
    }

    private byte[] decode(byte[] packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet);

        byte[] temp = new byte[4]; // seq check
        buffer.get(temp, 0, 4);
        lastAck = ByteBuffer.wrap(temp).getInt();
        System.out.println("seq: " + lastAck);

        buffer.get(4, temp, 0, 4); // ack
        int ack = ByteBuffer.wrap(temp).getInt();
        System.out.println("ack: ");

        byte[] flags = new byte[1]; // flags
        buffer.get(8, flags, 0, 1);
        System.out.println("flags: " + Arrays.toString(flags));

        buffer.get(9, temp, 0, 4); // get crc
        int crc = ByteBuffer.wrap(temp).getInt();
        System.out.println("received crc: " + crc);

        temp = new byte[payloadSize]; // get data
        buffer.get(14, temp, 0, payloadSize);
        System.out.println("data: " + Arrays.toString(temp));

        System.out.println("Comparing crc..."); // integrity check
        if (calculate_crc(temp) != crc) {
            System.out.println("Integrity check failed, dropping packet...");
            return null;
        }
        System.out.println("Success.");
        return temp;
    }

    private byte[][] fragment(byte[] data, int chunksize) {
        if (data.length > chunksize) {
            System.out.println("Fragmenting data...");
        }
        int packetNumber = data.length / chunksize + (data.length % chunksize != 0 ? 1 : 0);

        byte[][] fragmented = new byte[packetNumber][chunksize];

        for (int i = 0; i < packetNumber; i++) {
            fragmented[i] = Arrays.copyOfRange(data, i * chunksize, (i + 1) * chunksize);
        }

        return fragmented;
    }
}