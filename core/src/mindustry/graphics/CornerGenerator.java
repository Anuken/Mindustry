package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.PixmapPacker.*;
import arc.math.geom.*;
import arc.scene.style.*;
import mindustry.core.*;

/** Package-private rendering helpers shared by {@link AngledCorner} and {@link RoundCorner}. */
class CornerGenerator{

    private CornerGenerator(){}

    /** Packs a size x size tile filled via {@code fn}; returns {topLeft, topRight, bottomLeft, bottomRight}. */
    static TextureRegion[] renderCorner(String name, int size, CoverageFn fn){
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

    /** Builds a filled nine-patch drawable out of the four corner tiles from {@link #renderCorner}. */
    static NinePatchDrawable ninePatch(TextureRegion[] corners){
        TextureRegion topLeft = corners[0], topRight = corners[1], bottomLeft = corners[2], bottomRight = corners[3];
        int size = topRight.height;

        //Core.atlas uses an array texture, guaranteed to be the same as the packer's assuming it targets it
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

    /** Builds an {@link OutlinedCornerDrawable} out of the four corner tiles from {@link #renderCorner}. */
    static OutlinedCornerDrawable outlinedDrawable(TextureRegion[] corners, int size, float stroke){
        TextureRegion topLeft = corners[0], topRight = corners[1], bottomLeft = corners[2], bottomRight = corners[3];

        return new OutlinedCornerDrawable(topLeft, topRight, bottomLeft, bottomRight, Core.atlas.white(), size, stroke);
    }

    /** (x, y) pixel coordinate -> coverage (0-1). */
    interface CoverageFn{
        float get(int x, int y);
    }
}