import arc.util.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.*;

import static org.junit.jupiter.api.Assertions.*;

public class IOTests{

    @ParameterizedTest
    @ValueSource(strings ={
            "asd asd asd asd asdagagasasjakbgeah;jwrej 23424234",
            "这个服务器可以用自己的语言说话"
    })
    void writeStringTest(String string){
        ByteBuffer buffer = ByteBuffer.allocate(500);
        TypeIO.writeString(buffer, string);
        buffer.position(0);
        assertEquals(TypeIO.readString(buffer), string);
    }

    /**
     * This test can be replaced by a @NullSource annotation
     * in the writeStringTest as soon as JUnit libraries are
     * updated to at least version 5.7.
     */
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
