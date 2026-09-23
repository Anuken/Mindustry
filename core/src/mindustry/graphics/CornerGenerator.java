package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.PixmapPacker.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import mindustry.core.*;

public class CornerGenerator{
    /** Sub-samples per axis, per pixel, used to antialias the diagonal edge of the sharp/beveled corners. 8 -> 64 samples/pixel. */
    private static final int supersample = 8;

    private CornerGenerator(){}

    /**
     * Generates a NinePatchDrawable with antialiased angled corners.
     * @param unscaledSize size of the corner in unscaled pixels; this is scaled by Scl.scl.
     * */
    public static NinePatchDrawable generateAngled(int unscaledSize){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        TextureRegion[] tiles = renderCorner("corner-" + size, size, (x, y) -> coverage(x, y, size));
        return ninePatch(tiles);
    }

    /**
     * Generates an {@link OutlinedCornerDrawable}: an antialiased, beveled-corner outline in the same visual style as
     * {@link #generateAngled(int)}, but drawing only the outline (stroke) of the shape rather than filling it.
     * @param unscaledSize size of the corner in unscaled pixels; this is scaled by Scl.scl, same as {@link #generateAngled(int)}.
     * @param unscaledStroke width of the outline, perpendicular to the line, in unscaled pixels; this is scaled by Scl.scl.
     */
    public static OutlinedCornerDrawable generateAngledOutline(int unscaledSize, float unscaledStroke){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        float stroke = Scl.scl(unscaledStroke);
        String name = "corner-outline-" + size + "-" + Float.floatToIntBits(stroke);
        TextureRegion[] tiles = renderCorner(name, size, (x, y) -> outlineCoverage(x, y, stroke));
        return outlinedDrawable(tiles, size, stroke);
    }

    /**
     * Generates a NinePatchDrawable with antialiased rounded corners.
     * @param unscaledSize size of the corner in unscaled pixels; this is scaled by Scl.scl, same as {@link #generateAngled(int)}.
     */
    public static NinePatchDrawable generateRound(int unscaledSize){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        String name = "corner-round-" + size;
        TextureRegion[] tiles = renderCorner(name, size, (x, y) -> roundCoverage(x, y, size));
        return ninePatch(tiles);
    }

    /**
     * Generates an outlined drawable with antialiased rounded corners.
     * <p>
     * @param unscaledSize size of the corner in unscaled pixels; this is scaled by Scl.scl.
     * @param unscaledStroke width of the outline, in unscaled pixels; this is scaled by Scl.scl.
     */
    public static OutlinedCornerDrawable generateRoundOutline(int unscaledSize, float unscaledStroke){
        int size = Math.max((int)Scl.scl(unscaledSize), 1);
        float stroke = Scl.scl(unscaledStroke);
        String name = "corner-round-outline-" + size + "-" + Float.floatToIntBits(stroke);
        TextureRegion[] tiles = renderCorner(name, size, (x, y) -> outlineRoundCoverage(x, y, size, stroke));
        return outlinedDrawable(tiles, size, stroke);
    }

    /**
     * Packs a {@code size} x {@code size} tile into the UI packer, filling it pixel-by-pixel via {@code fn}, and
     * returns the four flipped corner regions derived from it.
     * @return {topLeft, topRight, bottomLeft, bottomRight}, matching the flips used by both {@link #generateAngled(int)}
     * and {@link #generateAngledOutline(int, float)}.
     */
    private static TextureRegion[] renderCorner(String name, int size, CoverageFn fn){
        PixmapPacker packer = UI.packer;
        Rect rect = packer.pack(name, size, size);
        Page page = packer.getPage(name);
        Pixmap pix = page.image;

        int baseX = (int)rect.x, baseY = (int)rect.y;

        //draw the antialiased shape directly into the packer page's pixmap
        for(int y = 0; y < size; y++){
            for(int x = 0; x < size; x++){
                float coverage = fn.get(x, y);
                pix.setRaw(baseX + x, baseY + y, Color.rgba8888(1f, 1f, 1f, coverage));
            }
        }

        page.setDirty(true);

        Texture texture = page.texture;

        //top-right corner tile, exactly as generated
        TextureRegion topRight = new TextureRegion(texture, baseX, baseY, size, size);
        TextureRegion topLeft = new TextureRegion(topRight);
        topLeft.flip(true, false);
        TextureRegion bottomRight = new TextureRegion(topRight);
        bottomRight.flip(false, true);
        TextureRegion bottomLeft = new TextureRegion(topRight);
        bottomLeft.flip(true, true);

        return new TextureRegion[]{topLeft, topRight, bottomLeft, bottomRight};
    }

    /** Builds a filled nine-patch drawable out of the four corner tiles returned by {@link #renderCorner}. */
    private static NinePatchDrawable ninePatch(TextureRegion[] corners){
        TextureRegion topLeft = corners[0], topRight = corners[1], bottomLeft = corners[2], bottomRight = corners[3];
        int size = topRight.height;

        //note: Core.atlas uses an array texture, and in this case it is guaranteed to be the same, assuming the packer is set up to target it
        TextureRegion white = Core.atlas.white();

        TextureRegion top = new TextureRegion(white);
        top.height = size;
        TextureRegion bottom = new TextureRegion(white);
        bottom.height = size;
        TextureRegion left = new TextureRegion(white);
        left.width = size;
        TextureRegion right = new TextureRegion(white);
        right.width = size;
        TextureRegion center = new TextureRegion(white);

        NinePatch patch = new NinePatch(
        topLeft, top, topRight,
        left, center, right,
        bottomLeft, bottom, bottomRight
        );

        return new NinePatchDrawable(patch);
    }

    /** Builds an {@link OutlinedCornerDrawable} out of the four corner tiles returned by {@link #renderCorner}. */
    private static OutlinedCornerDrawable outlinedDrawable(TextureRegion[] corners, int size, float stroke){
        TextureRegion topLeft = corners[0], topRight = corners[1], bottomLeft = corners[2], bottomRight = corners[3];

        //note: Core.atlas uses an array texture, and in this case it is guaranteed to be the same, assuming the packer is set up to target it
        return new OutlinedCornerDrawable(topLeft, topRight, bottomLeft, bottomRight, Core.atlas.white(), size, stroke);
    }

    /** @return the antialiased coverage (0-1) of pixel (x, y) with an angled edge. */
    private static float coverage(int x, int y, int size){
        int inside = 0;
        for(int sy = 0; sy < supersample; sy++){
            float fy = y + (sy + 0.5f) / supersample;
            for(int sx = 0; sx < supersample; sx++){
                float fx = x + (sx + 0.5f) / supersample;
                if(fy > fx) inside++;
            }
        }
        return inside / (float)(supersample * supersample);
    }

    /** @return the antialiased coverage (0-1) of pixel (x, y) with an angled outlined edge. */
    private static float outlineCoverage(int x, int y, float stroke){
        int inside = 0;
        float band = stroke;
        float extrude = stroke * ((float)Math.sqrt(2.0) - 1f);
        for(int sy = 0; sy < supersample; sy++){
            float fy = y + (sy + 0.5f) / supersample;
            for(int sx = 0; sx < supersample; sx++){
                float fx = x + (sx + 0.5f) / supersample;
                float diff = fy - fx;
                //on the filled side of the diagonal, extruded a bit toward the top-right, and within the band
                if(diff >= -extrude && diff < band) inside++;
            }
        }
        return inside / (float)(supersample * supersample);
    }

    private static float cornerDistance(float fx, float fy, int size){
        float qy = Math.max(size - fy, 0f);
        return (float)Math.sqrt(fx * fx + qy * qy) - size;
    }

    private static float roundCoverage(int x, int y, int size){
        float d = cornerDistance(x + 0.5f, y + 0.5f, size);
        return Mathf.clamp(0.5f - d);
    }

    private static float outlineRoundCoverage(int x, int y, int size, float stroke){
        float d = cornerDistance(x + 0.5f, y + 0.5f, size);
        return Mathf.clamp(0.5f - d) - Mathf.clamp(0.5f - d - stroke);
    }

    /** (x, y) pixel coordinate -> coverage (0-1). */
    private interface CoverageFn{
        float get(int x, int y);
    }
}