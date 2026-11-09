import arc.util.*;
import mindustry.ai.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/** Parity checks for the JNI flow-field port. Skipped when no native library is supplied. */
public class RustPathfinderTests{

    @BeforeAll
    static void loadNative(){
        String path = System.getenv("MINDUSTRY_NATIVE_LIB");
        if(path != null){
            if(!SharedLibraryLoader.isLoaded("mindustry_native")){
                System.load(new File(path).getAbsolutePath());
                SharedLibraryLoader.setLoaded("mindustry_native");
            }
            RustPathfinder.tryLoad();
        }
    }

    @Test
    void flatGridMatchesManhattanDistance(){
        assumeTrue(RustPathfinder.available(), "native library not supplied");

        int w = 9, h = 9, target = 4 + 4 * w;
        int[] tiles = new int[w * h];
        int[] weights = new int[w * h];
        short[] searches = new short[w * h];
        Arrays.fill(weights, -1);

        assertTrue(RustPathfinder.build(tiles, weights, searches, new int[]{target}, 1, 4, 1, w, h));

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(Math.abs(x - 4) + Math.abs(y - 4), weights[x + y * w], "at " + x + "," + y);
            }
        }
    }

    @Test
    void nativeFieldMatchesJavaReferenceWithWalls(){
        assumeTrue(RustPathfinder.available(), "native library not supplied");

        int w = 12, h = 10;
        int[] tiles = new int[w * h];
        //allDeep wall, with one gap at the top row
        for(int y = 1; y < h; y++){
            tiles[6 + y * w] |= 1 << 25;
        }

        int[] weights = new int[w * h];
        short[] searches = new short[w * h];
        Arrays.fill(weights, -1);

        assertTrue(RustPathfinder.build(tiles, weights, searches, new int[]{0}, 1, 0, 1, w, h));
        assertArrayEquals(referenceBfs(tiles, w, h, 0), weights);
    }

    @Test
    void invalidDimensionsReturnFalse(){
        assumeTrue(RustPathfinder.available(), "native library not supplied");

        int[] tiles = new int[4];
        int[] weights = new int[4];
        short[] searches = new short[4];
        Arrays.fill(weights, -1);
        assertFalse(RustPathfinder.build(tiles, weights, searches, new int[]{0}, 1, 4, 1, 0, 4));
    }

    /** Plain BFS over a grid where the allDeep bit makes a tile impassable for ground units. */
    static int[] referenceBfs(int[] tiles, int w, int h, int target){
        int[] dist = new int[w * h];
        Arrays.fill(dist, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        dist[target] = 0;
        queue.add(target);

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while(!queue.isEmpty()){
            int pos = queue.remove();
            int x = pos % w, y = pos / w;
            for(int[] d : dirs){
                int nx = x + d[0], ny = y + d[1];
                if(nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int next = nx + ny * w;
                if((tiles[next] & (1 << 25)) != 0) continue;
                if(dist[next] == -1){
                    dist[next] = dist[pos] + 1;
                    queue.add(next);
                }
            }
        }
        return dist;
    }
}
