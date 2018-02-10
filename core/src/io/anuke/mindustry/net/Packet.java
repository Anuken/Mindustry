package io.anuke.mindustry.net;

import java.nio.ByteBuffer;

public interface Packet {
    void read(ByteBuffer buffer);
    void write(ByteBuffer buffer);

    interface ImportantPacket{}
    interface UnimportantPacket{}
}
