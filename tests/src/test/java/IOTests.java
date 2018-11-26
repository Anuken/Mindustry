import io.anuke.mindustry.io.TypeIO;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IOTests{

    @Test
    void writeEnglish(){
        ByteBuffer buffer = ByteBuffer.allocate(500);
        TypeIO.writeString(buffer, "asd asd asd asd asdagagasasjakbgeah;jwrej 23424234");
        buffer.position(0);
        assertEquals(TypeIO.readString(buffer), "asd asd asd asd asdagagasasjakbgeah;jwrej 23424234");
    }

    @Test
    void writeChinese(){
        ByteBuffer buffer = ByteBuffer.allocate(500);
        TypeIO.writeString(buffer, "这个服务器可以用自己的语言说话");
        buffer.position(0);
        assertEquals(TypeIO.readString(buffer), "这个服务器可以用自己的语言说话");
    }

    @Test
    void writeNull(){
        ByteBuffer buffer = ByteBuffer.allocate(500);
        TypeIO.writeString(buffer, null);
        buffer.position(0);
        assertEquals(TypeIO.readString(buffer), null);
    }

}
