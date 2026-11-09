import arc.util.Align;
import mindustry.ui.GreenFps;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class GreenFpsTests{

    @Test
    void cornersMapToAlignments(){
        assertEquals(Align.topLeft, GreenFps.cornerAlign(GreenFps.cornerTopLeft));
        assertEquals(Align.topRight, GreenFps.cornerAlign(GreenFps.cornerTopRight));
        assertEquals(Align.bottomLeft, GreenFps.cornerAlign(GreenFps.cornerBottomLeft));
        assertEquals(Align.bottomRight, GreenFps.cornerAlign(GreenFps.cornerBottomRight));
    }

    @Test
    void defaultCornerIsTopRight(){
        assertEquals(GreenFps.cornerTopRight, GreenFps.defaultCorner);
        assertEquals(Align.topRight, GreenFps.cornerAlign(GreenFps.defaultCorner));
    }

    @Test
    void invalidCornerValuesAreClamped(){
        assertEquals(Align.topLeft, GreenFps.cornerAlign(-10));
        assertEquals(Align.bottomRight, GreenFps.cornerAlign(99));
    }
}
