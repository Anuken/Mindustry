package mindustry.ui.style;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;

public class RoundCorner{
    /** Cache of generated fills, keyed by the scaled corner size in pixels. */
    private static final IntMap<NinePatchDrawable> fills = new IntMap<>();
    /** Cache of generated outlines, keyed by {@link Pack#longInt(int, int)} of (scaled size, float bits of scaled stroke). */
    private static final LongMap<OutlinedCornerDrawable> outlines = new LongMap<>();

    private RoundCorner(){}

    /** Generates a NinePatchDrawable with antialiased rounded corners; size is unscaled and scaled by Scl.scl. */
    private static NinePatchDrawable generate(int unscaledSize){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        String name = "corner-round-" + size;
        TextureRegion[] tiles = CornerGenerator.renderCorner(name, size, (x, y) -> coverage(x, y, size));
        return CornerGenerator.ninePatch(tiles);
    }

    /** Generates an antialiased rounded-corner outline (stroke only); size and stroke are unscaled and scaled by Scl.scl. */
    private static OutlinedCornerDrawable generateOutline(int unscaledSize, float unscaledStroke){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        float stroke = Scl.scl(unscaledStroke);
        String name = "corner-round-outline-" + size + "-" + Float.floatToIntBits(stroke);
        TextureRegion[] tiles = CornerGenerator.renderCorner(name, size, (x, y) -> outlineCoverage(x, y, size, stroke));
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

    /** @return a cached filled rounded-corner drawable tinted with {@code color}; size is unscaled. */
    public static NinePatchDrawable fill(int size, Color color){
        return getCache(size).tint(color);
    }

    /** @return a cached rounded-corner outline drawable tinted with {@code color}; size and stroke are unscaled. */
    public static OutlinedCornerDrawable outline(int size, Color color, float stroke){
        return getCache(size, stroke).tint(color);
    }

    /** Creates a fill in {@code bgColor} with an outline in {@code borderColor} on top; size and stroke are unscaled. */
    public static StackDrawable create(int size, Color bgColor, Color borderColor, float stroke){
        //the fill is 1 smaller than the outline to avoid antialiasing artifacts at the edges
        return new StackDrawable(fill(size + 1, bgColor), outline(size, borderColor, stroke));
    }

    private static float cornerDistance(float fx, float fy, int size){
        float qy = Math.max(size - fy, 0f);
        return (float)Math.sqrt(fx * fx + qy * qy) - size;
    }

    private static float coverage(int x, int y, int size){
        float d = cornerDistance(x + 0.5f, y + 0.5f, size);
        return Mathf.clamp(0.5f - d);
    }

    private static float outlineCoverage(int x, int y, int size, float stroke){
        float d = cornerDistance(x + 0.5f, y + 0.5f, size);
        return Mathf.clamp(0.5f - d) - Mathf.clamp(0.5f - d - stroke);
    }
}