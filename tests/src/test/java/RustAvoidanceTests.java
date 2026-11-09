import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.async.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/** Parity checks for the JNI avoidance-field port. Skipped when no native library is supplied. */
public class RustAvoidanceTests{

    @BeforeAll
    static void loadNative(){
        String path = System.getenv("MINDUSTRY_NATIVE_LIB");
        if(path != null){
            if(!SharedLibraryLoader.isLoaded("mindustry_native")){
                System.load(new File(path).getAbsolutePath());
                SharedLibraryLoader.setLoaded("mindustry_native");
            }
            RustAvoidance.tryLoad();
        }
    }

    @Test
    void smallRadiusMatchesJavaReference(){
        assumeTrue(RustAvoidance.available(), "native library not supplied");

        int w = 10, h = 10;
        int[] requests = request(5, 5, 1f, 7);
        int[] actual = new int[w * h];
        assertTrue(RustAvoidance.build(requests, requests.length, actual, w, h, false));
        assertArrayEquals(reference(requests, requests.length, new int[w * h], w, h, false), actual);
    }

    @Test
    void overlappingRequestsAndClearMatchJavaReference(){
        assumeTrue(RustAvoidance.available(), "native library not supplied");

        int w = 12, h = 8;
        int[] requests = concat(
            request(3, 3, 2.5f, 4),
            request(4, 3, 1.5f, 9),
            request(-1, -1, 1f, 2)
        );
        int[] initial = new int[w * h];
        Arrays.fill(initial, 123);
        int[] actual = initial.clone();
        assertTrue(RustAvoidance.build(requests, requests.length, actual, w, h, true));
        assertArrayEquals(reference(requests, requests.length, initial, w, h, true), actual);
    }

    @Test
    void accumulatesAcrossFrames(){
        assumeTrue(RustAvoidance.available(), "native library not supplied");

        int w = 9, h = 7;
        int[] first = request(4, 3, 1f, 5);
        int[] second = request(4, 4, 2f, 8);
        int[] actual = new int[w * h];
        int[] expected = new int[w * h];

        assertTrue(RustAvoidance.build(first, first.length, actual, w, h, false));
        expected = reference(first, first.length, expected, w, h, false);
        assertTrue(RustAvoidance.build(second, second.length, actual, w, h, false));
        expected = reference(second, second.length, expected, w, h, false);
        assertArrayEquals(expected, actual);
    }

    @Test
    void radiusBeyondTwentyIsNotCapped(){
        assumeTrue(RustAvoidance.available(), "native library not supplied");

        int w = 100, h = 100;
        int[] requests = request(50, 50, 25f, 1);
        int[] actual = new int[w * h];
        assertTrue(RustAvoidance.build(requests, requests.length, actual, w, h, false));
        assertArrayEquals(reference(requests, requests.length, new int[w * h], w, h, false), actual);
    }

    @Test
    void negativeCoordinatesMatchJavaReference(){
        assumeTrue(RustAvoidance.available(), "native library not supplied");

        int w = 5, h = 5;
        int[] requests = request(-1, 2, 1f, 5);
        int[] actual = new int[w * h];
        assertTrue(RustAvoidance.build(requests, requests.length, actual, w, h, false));
        assertArrayEquals(reference(requests, requests.length, new int[w * h], w, h, false), actual);
    }

    @Test
    void invalidInputsReturnFalseWithoutTouchingBuffer(){
        assumeTrue(RustAvoidance.available(), "native library not supplied");

        int[] requests = request(1, 1, 1f, 1);
        int[] buffer = new int[9];
        int[] before = buffer.clone();
        assertFalse(RustAvoidance.build(requests, requests.length, buffer, 0, 3, false));
        assertFalse(RustAvoidance.build(requests, 2, buffer, 3, 3, false));
        assertFalse(RustAvoidance.build(
            new int[]{Point2.pack(1, 1), Float.floatToRawIntBits(Float.NaN), 1},
            3, buffer, 3, 3, false));
        assertArrayEquals(before, buffer);
    }

    static int[] request(int x, int y, float rad, int id){
        return new int[]{Point2.pack(x, y), Float.floatToRawIntBits(rad), id};
    }

    static int[] concat(int[]... arrays){
        int total = 0;
        for(int[] a : arrays) total += a.length;
        int[] out = new int[total];
        int index = 0;
        for(int[] a : arrays){
            System.arraycopy(a, 0, out, index, a.length);
            index += a.length;
        }
        return out;
    }

    /** Exact Java reference for AvoidanceProcess.process(). */
    static int[] reference(int[] requests, int total, int[] initial, int w, int h, boolean clear){
        int[] buffer = initial.clone();
        if(clear) Arrays.fill(buffer, 0);

        for(int i = 0; i < total; i += 3){
            int point = requests[i], id = requests[i + 2];
            int rx = Point2.x(point), ry = Point2.y(point);
            float rad = Float.intBitsToFloat(requests[i + 1]);
            float rad2 = rad * rad;
            int r = Mathf.clamp(1, Mathf.ceil(rad), 20);

            for(int dx = -r; dx <= r; dx++){
                for(int dy = -r; dy <= r; dy++){
                    int x = dx + rx, y = dy + ry;
                    if(x >= 0 && y >= 0 && x < w && y < h && (dx*dx + dy*dy) <= rad2){
                        buffer[x + y * w] = Math.max(buffer[x + y * w], Integer.MAX_VALUE - id);
                    }
                }
            }
        }
        return buffer;
    }
}
