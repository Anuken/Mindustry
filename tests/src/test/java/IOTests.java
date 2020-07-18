import arc.util.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.io.*;
import org.junit.jupiter.api.*;

import java.nio.*;

import static org.junit.jupiter.api.Assertions.*;

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
        assertNull(TypeIO.readString(buffer));
    }

    @Test
    void writeRules(){
        ByteBuffer buffer = ByteBuffer.allocate(500);

        Rules rules = new Rules();
        rules.attackMode = true;
        rules.buildSpeedMultiplier = 99f;

        TypeIO.writeRules(new Writes(new ByteBufferOutput(buffer)), rules);
        buffer.position(0);
        Rules res = TypeIO.readRules(new Reads(new ByteBufferInput(buffer)));

        assertEquals(rules.buildSpeedMultiplier, res.buildSpeedMultiplier);
        assertEquals(rules.attackMode, res.attackMode);
    }

    @Test
    void writeRules2(){
        Rules rules = new Rules();
        rules.attackMode = true;
        rules.tags.put("blah", "bleh");
        rules.buildSpeedMultiplier = 99.1f;

        String str = JsonIO.write(rules);
        Rules res = JsonIO.read(Rules.class, str);

        assertEquals(rules.buildSpeedMultiplier, res.buildSpeedMultiplier);
        assertEquals(rules.attackMode, res.attackMode);
        assertEquals(rules.tags.get("blah"), res.tags.get("blah"));

        String str2 = JsonIO.write(new Rules(){{
            attackMode = true;
        }});
        Log.info(str2);
    }
}
