package salty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

// SALTP (Secure Application Level Transport Protocol)

public class SaltyProtocol {

    private final int MTU;

    // private int seq;
    // private int lastAck;

    public SaltyProtocol(int MTU) {
        this.MTU = MTU;
    }

    public void test(byte[] bytes) throws IOException {
        for (ByteBuffer packet : this.fragment(bytes, 300)) {
            SaltyPacket ppack = SaltyPacket.wrap(this, packet);
            System.out.println(ppack);
            ByteBuffer pack = ppack.toBytes();
            System.out.println(pack);

            SaltyPacket punpack = SaltyPacket.unwrap(pack);
            System.out.println(punpack);
            ByteBuffer unpack = punpack.toBytes();
            System.out.println(unpack);

            // System.out.println("decoded string: " + new String(unpack.d, StandardCharsets.UTF_8));
        }
    }

    private List<ByteBuffer> fragment(byte[] data, int chunkSize) {
        return fragment(ByteBuffer.wrap(data), chunkSize);
    }

    // TODO: considerare l'header variabile.
    private List<ByteBuffer> fragment(ByteBuffer data, int chunkSize) {
        if (data.limit() > chunkSize) {
            System.out.println("Fragmenting data...");
        }
        List<ByteBuffer> out = new LinkedList<>();
        do {
            out.add(data.slice(out.size() * chunkSize, Math.min(chunkSize, data.limit() - out.size() * chunkSize)));
        } while (data.limit() - out.size() * chunkSize > chunkSize);
        return out;
    }
}