import java.nio.ByteBuffer;

public class SaltyPacket {

    private short seq;
    private short ack;
    private byte flags;
    private int checksum;
    private int payloadSize;
    private byte[] data;
    public final int HEADER_LEN = 14;
    public final int SEQ_SIZE = 2;
    public final int ACK_SIZE = 2;

    public int getSeq() {
        return seq;
    }

    public void setSeq(short seq) {
        this.seq = seq;
    }

    public int getAck() {
        return ack;
    }

    public void setAck(short ack) {
        this.ack = ack;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.checksum = calculate_crc(data);
    }

    public SaltyPacket(short seq, short ack, byte flag, int payloadSize, byte[] data) {
        this.seq = seq;
        this.ack = ack;
        this.flags = flag;
        this.checksum = calculate_crc(data);
        this.payloadSize = payloadSize;
        this.data = data;
    }

    public SaltyPacket(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    int calculate_crc(byte[] bytes) { //stackoverflow is the way
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

    public byte[] encode() {
        ByteBuffer packet = ByteBuffer.allocate(payloadSize+HEADER_LEN);
        packet.put(ByteBuffer.allocate(SEQ_SIZE).putShort(seq).array(), 0, SEQ_SIZE);
        packet.put(ByteBuffer.allocate(ACK_SIZE).putShort(ack).array(), 0, ACK_SIZE);
        packet.put(flags);
        packet.put(ByteBuffer.allocate(4).putInt(calculate_crc(data)).array(), 0, 4);
        packet.put((byte) 0);
        packet.put(data);
        return packet.array();
    }

    public SaltyPacket wrap(byte[] packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet);

        byte[] temp = new byte[4]; // seq check
        buffer.get(temp, 0, SEQ_SIZE);
        this.seq = ByteBuffer.wrap(temp).getShort();

        buffer.get(SEQ_SIZE, temp, 0, ACK_SIZE); // ack
        this.ack = ByteBuffer.wrap(temp).getShort();

        buffer.get(SEQ_SIZE+ACK_SIZE, new byte[]{this.flags}, 0, 1); // flags

        buffer.get(SEQ_SIZE+ACK_SIZE+1, temp, 0, 4); // get crc
        this.checksum = ByteBuffer.wrap(temp).getInt();

        buffer.get(SEQ_SIZE+ACK_SIZE+6, data, 0, payloadSize);

        return this;
    }


}
