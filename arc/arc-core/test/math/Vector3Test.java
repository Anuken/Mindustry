package math;

import arc.math.geom.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Vector3Test{
    @Test
    public void testToString(){
        assertEquals("(-5.0,42.00055,44444.32)", new Vec3(-5f, 42.00055f, 44444.32f).toString());
    }

    @Test
    public void testFromString(){
        assertEquals(new Vec3(-5f, 42.00055f, 44444.32f), new Vec3().fromString("(-5,42.00055,44444.32)"));
    }
}
