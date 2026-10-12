package arc.scene.flabel;

import arc.graphics.*;
import arc.graphics.font.Font.*;
import arc.graphics.g2d.GlyphLayout.*;
import arc.util.pooling.Pool.*;

/** Extension of {@link Glyph} with additional data exposed to the user. */
public class FGlyph extends Glyph implements Poolable{
    /** {@link GlyphRun} this glyph belongs to. */
    public GlyphRun run = null;
    /** Internal index associated with this glyph. Internal use only. Defaults to -1. */
    int internalIndex = -1;
    /** Color of this glyph. If set to null, the run's color will be used. Defaults to null. */
    public Color color = null;

    public void set(Glyph from){
        id = from.id;
        srcX = from.srcX;
        srcY = from.srcY;
        width = from.width;
        height = from.height;
        texture = from.texture;
        u = from.u;
        v = from.v;
        u2 = from.u2;
        v2 = from.v2;
        xoffset = from.xoffset;
        yoffset = from.yoffset;
        xadvance = from.xadvance;
        kerning = from.kerning; // Keep the same instance, there's no reason to deep clone it
        fixedWidth = from.fixedWidth;

        run = null;
        internalIndex = -1;
        color = null;
    }

    @Override
    public void reset(){
        id = 0;
        srcX = 0;
        srcY = 0;
        width = 0;
        height = 0;
        texture = null;
        u = 0;
        v = 0;
        u2 = 0;
        v2 = 0;
        xoffset = 0;
        yoffset = 0;
        xadvance = 0;
        kerning = null;
        fixedWidth = false;

        run = null;
        internalIndex = -1;
        color = null;
    }

}
