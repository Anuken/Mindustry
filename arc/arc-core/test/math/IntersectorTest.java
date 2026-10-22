package math;

import arc.math.Mathf;

import static org.junit.Assert.*;

public class IntersectorTest{

    /**
     * Compares two triangles for equality. Triangles must have the same winding, but may begin with different vertex. Values are
     * epsilon compared, with default tolerance. Triangles are assumed to be valid triangles - no duplicate vertices.
     */
    private static boolean triangleEquals(float[] base, int baseOffset, int stride, float[] comp){
        assertTrue(stride >= 3);
        assertTrue(base.length - baseOffset >= 9);
        assertEquals(9, comp.length);

        int offset = -1;
        // Find first comp vertex in base triangle
        for(int i = 0; i < 3; i++){
            int b = baseOffset + i * stride;
            if(Mathf.equal(base[b], comp[0]) && Mathf.equal(base[b + 1], comp[1])
            && Mathf.equal(base[b + 2], comp[2])){
                offset = i;
                break;
            }
        }
        assertTrue("Triangles do not have common first vertex.", offset != -1);
        // Compare vertices
        for(int i = 0; i < 3; i++){
            int b = baseOffset + ((offset + i) * stride) % (3 * stride);
            int c = i * stride;
            if(!Mathf.equal(base[b], comp[c]) || !Mathf.equal(base[b + 1], comp[c + 1])
            || !Mathf.equal(base[b + 2], comp[c + 2])){
                return false;
            }
        }
        return true;
    }

}
