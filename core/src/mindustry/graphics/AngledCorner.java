package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;

public class AngledCorner{
    /** Cache of generated fills, keyed by the scaled corner size in pixels. */
    private static final IntMap<NinePatchDrawable> fills = new IntMap<>();
    /** Cache of generated outlines, keyed by {@link Pack#longInt(int, int)} of (scaled size, float bits of scaled stroke). */
    private static final LongMap<OutlinedCornerDrawable> outlines = new LongMap<>();

    private AngledCorner(){}

    /** Generates a NinePatchDrawable with antialiased angled corners; size is unscaled and scaled by Scl.scl. */
    private static NinePatchDrawable generate(int unscaledSize){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        TextureRegion[] tiles = CornerGenerator.renderCorner("corner-" + size, size, (x, y) -> halfPlane(y - x));
        return CornerGenerator.ninePatch(tiles);
    }

    /** Generates an antialiased beveled-corner outline (stroke only); size and stroke are unscaled and scaled by Scl.scl. */
    private static OutlinedCornerDrawable generateOutline(int unscaledSize, float unscaledStroke){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        float stroke = Scl.scl(unscaledStroke);
        String name = "corner-outline-" + size + "-" + Float.floatToIntBits(stroke);
        TextureRegion[] tiles = CornerGenerator.renderCorner(name, size, (x, y) -> outlineCoverage(x, y, stroke));
        return CornerGenerator.outlinedDrawable(tiles, size, stroke);
    }

    /** Returns a cached fill keyed on scaled size, generating it on first use; shared, so use tint(Color) for a copy. */
    private static NinePatchDrawable getCache(int unscaledSize){
        int key = Math.max((int)Scl.scl(unscaledSize), 1);
        return fills.get(key, () -> generate(unscaledSize));
    }

    /** Returns a cached outline keyed on scaled size and stroke, generating it on first use; shared, so use tint(Color) for a copy. */
    private static OutlinedCornerDrawable getCache(int unscaledSize, float unscaledStroke){
        long key = Pack.longInt(Math.max((int)Scl.scl(unscaledSize), 1), Float.floatToIntBits(Scl.scl(unscaledStroke)));
        OutlinedCornerDrawable out = outlines.get(key);
        if(out == null){
            out = generateOutline(unscaledSize, unscaledStroke);
            outlines.put(key, out);
        }
        return out;
    }

    /** @return a cached filled angled-corner drawable tinted with {@code color}; size is unscaled. */
    public static NinePatchDrawable fill(int size, Color color){
        return getCache(size).tint(color);
    }

    /** @return a cached filled angled-corner drawable with a top-to-bottom gradient; size is unscaled. */
    public static NinePatchDrawable fill(int size, Color top, Color bottom){
        return getCache(size).tint(top, bottom);
    }

    /** @return a cached angled-corner outline drawable tinted with {@code color}; size and stroke are unscaled. */
    public static OutlinedCornerDrawable outline(int size, Color color, float stroke){
        return getCache(size, stroke).tint(color);
    }

    /** @return a cached angled-corner outline drawable with a top-to-bottom gradient; size and stroke are unscaled. */
    public static OutlinedCornerDrawable outline(int size, Color top, Color bottom, float stroke){
        return getCache(size, stroke).tint(top, bottom);
    }

    /** Creates a fill in {@code bgColor} with an outline in {@code borderColor} on top; size and stroke are unscaled. */
    public static StackDrawable create(int size, Color bgColor, Color borderColor, float stroke){
        //the fill is 1 smaller than the outline to avoid antialiasing artifacts at the edges
        return new StackDrawable(fill(size + 1, bgColor), outline(size, borderColor, stroke));
    }

    /** Same as {@link #create(int, Color, Color, float)}, but with a gradient fill. */
    public static StackDrawable create(int size, Color bgTop, Color bgBottom, Color borderColor, float stroke){
        return new StackDrawable(fill(size + 1, bgTop, bgBottom), outline(size, borderColor, stroke));
    }

    /** Same as {@link #create(int, Color, Color, float)}, but with a gradient fill and outline. */
    public static StackDrawable create(int size, Color bgTop, Color bgBottom, Color borderTop, Color borderBottom, float stroke){
        return new StackDrawable(fill(size + 1, bgTop, bgBottom), outline(size, borderTop, borderBottom, stroke));
    }

    /** @return exact fraction (0-1) of a pixel lying past a 45-degree edge, given the pixel center's diagonal offset v in pixels. */
    private static float halfPlane(float v){
        if(v <= -1f) return 0f;
        if(v >= 1f) return 1f;
        return v < 0f ? (1f + v) * (1f + v) / 2f : 1f - (1f - v) * (1f - v) / 2f;
    }

    /** @return the antialiased coverage (0-1) of pixel (x, y) with an angled outlined edge. */
    private static float outlineCoverage(int x, int y, float stroke){
        //the band spans from the diagonal extruded toward the top-right, to one stroke width past it
        float extrude = stroke * ((float)Math.sqrt(2.0) - 1f);
        float u = y - x;
        return halfPlane(u + extrude) - halfPlane(u - stroke);
    }
}