

package arc.net;

import java.nio.ByteBuffer;

/**
 * Controls how objects are transmitted over the network.
 * <p>
 * Every client connection on the server uses a separate instance for TCP
 * transmissions and the <i>same</i> instance for UDP ones. Therefore all
 * implementing classes have to be synchronized or made thread-safe otherwise.
 */
public interface NetSerializer{

    void write(ByteBuffer buffer, Object object);

    Object read(ByteBuffer buffer);

    /**
     * The fixed number of bytes that will be written by
     * {@link #writeLength(ByteBuffer, int)} and read by
     * {@link #readLength(ByteBuffer)}.
     */
    default int getLengthLength(){
        return 2;
    }

    default void writeLength(ByteBuffer buffer, int length){
        buffer.putShort((short)length);
    }

    default int readLength(ByteBuffer buffer){
        return buffer.getShort() & 0xffff; //convert to unsigned short for a higher max packet size
    }
}
