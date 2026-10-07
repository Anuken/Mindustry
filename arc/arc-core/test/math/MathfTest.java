package math;

import arc.math.Mathf;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathfTest{

    @Test
    public void lerpAngleDeg(){
        Assert.assertEquals(10, Mathf.slerp(10, 30, 0.0f), 0.01f);
        assertEquals(20, Mathf.slerp(10, 30, 0.5f), 0.01f);
        assertEquals(30, Mathf.slerp(10, 30, 1.0f), 0.01f);
    }

    @Test
    public void lerpAngleDegCrossingZero(){
        assertEquals(350, Mathf.slerp(350, 10, 0.0f), 0.01f);
        assertEquals(0, Mathf.slerp(350, 10, 0.5f), 0.01f);
        assertEquals(10, Mathf.slerp(350, 10, 1.0f), 0.01f);
    }

    @Test
    public void lerpAngleDegCrossingZeroBackwards(){
        assertEquals(10, Mathf.slerp(10, 350, 0.0f), 0.01f);
        assertEquals(0, Mathf.slerp(10, 350, 0.5f), 0.01f);
        assertEquals(350, Mathf.slerp(10, 350, 1.0f), 0.01f);
    }

}
