package utils;

import arc.struct.*;
import org.junit.*;

import static org.junit.Assert.*;

public class BitsTest{

    @Test
    public void testHashcodeAndEquals(){
        Bits b1 = new Bits();
        Bits b2 = new Bits();

        b1.set(1);
        b2.set(1);

        assertEquals(b1.hashCode(), b2.hashCode());
        assertEquals(b1, b2);

        //temporarily setting/clearing a single bit causing
        //the backing array to grow
        b2.set(420);
        b2.clear(420);

        assertEquals(b1.hashCode(), b2.hashCode());
        assertEquals(b1, b2);

        b1.set(810);
        b1.clear(810);

        assertEquals(b1.hashCode(), b2.hashCode());
        assertEquals(b1, b2);
    }

    @Test
    public void testXor(){
        Bits b1 = new Bits();
        Bits b2 = new Bits();

        b2.set(200);

        // b1:s array should grow to accommodate b2
        b1.xor(b2);

        assertTrue(b1.get(200));

        b1.set(1024);
        b2.xor(b1);

        assertTrue(b2.get(1024));
    }

    @Test
    public void testOr(){
        Bits b1 = new Bits();
        Bits b2 = new Bits();

        b2.set(200);

        // b1:s array should grow to accommodate b2
        b1.or(b2);

        assertTrue(b1.get(200));

        b1.set(1024);
        b2.or(b1);

        assertTrue(b2.get(1024));
    }

    @Test
    public void testAnd(){
        Bits b1 = new Bits();
        Bits b2 = new Bits();


        b2.set(200);
        // b1 should cancel b2:s bit
        b2.and(b1);

        assertFalse(b2.get(200));

        b1.set(400);
        b1.and(b2);

        assertFalse(b1.get(400));
    }
}
