package math;

import arc.math.geom.Vec2;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Vector2Test{
    @Test
    public void testToString(){
        assertEquals("(-5.0,42.00055)", new Vec2(-5f, 42.00055f).toString());
    }

    @Test
    public void testFromString(){
        assertEquals(new Vec2(-5f, 42.00055f), new Vec2().fromString("(-5,42.00055)"));
    }
}
