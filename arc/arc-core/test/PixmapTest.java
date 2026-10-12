import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import org.junit.*;

import static org.junit.Assert.*;

public class PixmapTest{

    @Test
    public void pixmapCreate(){

        //test with no natives
        Pixmap pix = new Pixmap(100, 100);
        pix.fillCircle(50, 50, 30, Color.red.rgba());

        assertEquals(Color.red.rgba(), pix.get(50, 50));
        assertEquals(Color.red.rgba(), pix.get(54, 54));
        assertEquals(0, pix.get(0, 0));

        ArcNativesLoader.load();

        pix = new Pixmap(100, 100);
        pix.fillCircle(50, 50, 30, Color.red.rgba());

        assertEquals(Color.red.rgba(), pix.get(50, 50));
        assertEquals(Color.red.rgba(), pix.get(54, 54));
        assertEquals(0, pix.get(0, 0));
    }

    @Test
    public void pixmapBounds(){
        int x = 176;

        Pixmap base = new Pixmap(176, 269);
        Pixmap crop = new Pixmap(176, 176);

        crop.draw(base, 0, 176, x, x, 0, 0, x, x, true);
    }

    @Test
    public void testPixmapDraw(){
        ArcNativesLoader.load();
        Pixmap src = new Pixmap(500, 500);
        Pixmap dst = src.copy();

        for(int x = 0; x < src.width; x++){
            for(int y = 0; y < src.height; y++){
                src.setRaw(x, y, Mathf.rand.nextInt());
            }
        }

        for(int i = 0; i < 50; i++){
            dst.draw(src, Mathf.random(src.width), Mathf.random(src.height));
        }
    }

}