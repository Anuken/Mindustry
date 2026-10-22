package mindustry.ui.style;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;

/** Generates outer-glow nine-patches shaped like an angled-corner panel, for {@link GlowDrawable}. */
public class GlowCorner{
    /** unscaled glow spread every tile is baked at; requests scale the drawable to fake a different spread. */
    private static final float maxGlow = 25f;
    /** cache of generated tiles, keyed by the scaled corner radius only - not by requested glow spread. */
    private static final IntMap<GlowDrawable> glows = new IntMap<>();

    private GlowCorner(){}

    /** @return a glow drawable tinted with {@code color}; {@code cornerRadius} and {@code glowRadius} are unscaled. */
    public static GlowDrawable create(int cornerRadius, float glowRadius, Color color){
        return getCache(cornerRadius).scaled(glowRadius / maxGlow).tint(color);
    }

    private static GlowDrawable getCache(int unscaledRadius){
        int key = Math.max((int)Scl.scl(unscaledRadius), 1);
        return glows.get(key, () -> generate(unscaledRadius));
    }

    private static GlowDrawable generate(int unscaledRadius){
        int radius = Math.max((int)Scl.scl(unscaledRadius), 1);
        int spread = Math.max((int)Scl.scl(maxGlow), 1);
        int size = radius + spread;

        TextureRegion tile = CornerGenerator.renderTile("glow-" + radius, size, (x, y) -> coverage(x, y, radius, spread));
        return build(tile, size);
    }

    private static GlowDrawable build(TextureRegion topRight, int size){
        TextureRegion topLeft = new TextureRegion(topRight);
        topLeft.flip(true, false);
        TextureRegion bottomRight = new TextureRegion(topRight);
        bottomRight.flip(false, true);
        TextureRegion bottomLeft = new TextureRegion(topRight);
        bottomLeft.flip(true, true);

        //bottom row of the tile is a pure horizontal falloff, stretched vertically to make the side edges
        TextureRegion right = new TextureRegion(topRight, 0, size - 1, size, 1);
        TextureRegion left = new TextureRegion(right);
        left.flip(true, false);
        //left column of the tile is a pure vertical falloff, stretched horizontally to make the top/bottom edges
        TextureRegion top = new TextureRegion(topRight, 0, 0, 1, size);
        TextureRegion bottom = new TextureRegion(top);
        bottom.flip(false, true);

        TextureRegion center = new TextureRegion(Core.atlas.white());

        NinePatch patch = new NinePatch(
        topLeft, top, topRight,
        left, center, right,
        bottomLeft, bottom, bottomRight
        );

        return new GlowDrawable(patch);
    }

    /** coverage (0-1) of the outward glow at (x, y) in a corner chamfered by {@code radius}, faded to 0 over {@code spread} pixels. */
    private static float coverage(int x, int y, int radius, int spread){
        //distance past the two straight edges, and past the diagonal chamfer joining them
        float u = x - radius, v = spread - y;
        float dist = Math.max(u, Math.max(v, (u + v - radius) * 0.70710678f));

        float t = Mathf.clamp(dist / spread);
        return 1f - t * t * (3f - 2f * t);
    }
}