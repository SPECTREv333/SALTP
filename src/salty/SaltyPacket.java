package salty;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

// Packet size.
public record SaltyPacket(short seq,       // 2 bytes
                          short ack,       // 2 bytes
                          BitSet flags,    // (flags / 8) bytes
                          salty.CRC check,       // 1, 2, 4 bytes
                          ByteBuffer payload) {

    public enum Flags {
        SYN,    // synchronize
        ACK,    // acknowledgment
        RST,    // reset seq count
        CRCL,   // CRC8
        CRCH,   // CRC16 if both crc flags are set then CRC32
        FIN;    // close connection

        private static int bytes() {
            return (int) Math.max(1, Math.ceil(Flags.values().length / 8.0));
        }
    }

    public static final int HEADER_SIZE = 4 + Flags.bytes();

    public static SaltyPacket wrap(SaltyProtocol state, ByteBuffer data) {
        BitSet flags = new BitSet(Flags.values().length);
        CRC crc;
        if (data.remaining() < 32) {
            flags.set(Flags.CRCL.ordinal());
            crc = new CRC(CRC.Type.CRC8, data);
        }
        else if (data.remaining() < 256) {
            flags.set(Flags.CRCH.ordinal());
            crc = new CRC(CRC.Type.CRC16, data);
        }
        else {
            flags.set(Flags.CRCL.ordinal());
            flags.set(Flags.CRCH.ordinal());
            crc = new CRC(CRC.Type.CRC32, data);
        }
        return new SaltyPacket((short) 0, (short) 0, flags, crc, data);
    }

    public static SaltyPacket wrap(SaltyProtocol state, byte[] data) {
        return wrap(state, ByteBuffer.wrap(data));
    }

    public static SaltyPacket unwrap(ByteBuffer data) {
        short seq = data.getShort();
        short ack = data.getShort();
        BitSet flags = BitSet.valueOf(data.slice(data.position(), Flags.bytes()));
        data.position(data.position() + Flags.bytes());

        CRC.Type type;
        if (flags.get(Flags.CRCL.ordinal()) && flags.get(Flags.CRCH.ordinal())) type = CRC.Type.CRC32;
        else if (flags.get(Flags.CRCH.ordinal())) type = CRC.Type.CRC16;
        else type = CRC.Type.CRC8;

        CRC check = new CRC(type, data);

        ByteBuffer payload = data.slice(data.position(), data.remaining() - type.size()).asReadOnlyBuffer();
        return new SaltyPacket(seq, ack, flags, check, payload);
    }

    public static SaltyPacket unwrap(byte[] data) {
        return unwrap(ByteBuffer.wrap(data));
    }

    public ByteBuffer toBytes() {
        byte[] flags_array = flags.toByteArray();
        if (flags_array.length == 0) flags_array = new byte[Flags.bytes()];
        ByteBuffer buffer = ByteBuffer.allocate(payload.remaining() + check.type().size() + HEADER_SIZE);
        buffer.putShort(seq);
        buffer.putShort(ack);
        buffer.put(flags_array);
        buffer.put(payload);
        payload.flip();
        buffer.put(check.toBytes());
        return buffer.flip(); //.asReadOnlyBuffer();
    }

    public boolean errored() {
        return false;
    }

}
