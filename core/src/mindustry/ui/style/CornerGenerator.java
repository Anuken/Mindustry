package mindustry.ui.style;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.PixmapPacker.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.struct.*;
import mindustry.core.*;

/** Package-private rendering helpers shared by {@link AngledCorner}, {@link RoundCorner} and {@link GlowCorner}. */
class CornerGenerator{
    /** Tiles that have been written to their page's pixmap, but not yet to its texture. */
    private static final Seq<PendingUpload> pending = new Seq<>(false, 8, PendingUpload.class);

    private CornerGenerator(){}

    /** Uploads any tiles generated since the last call to their textures; must be called before drawing a corner tile. */
    static void uploadPending(){
        if(pending.isEmpty()) return;

        for(PendingUpload upload : pending){
            //only the tile's own rect is uploaded; marking the whole page dirty would reupload all of it, and only whenever a font happens to update
            upload.page.texture.draw(upload.tile, upload.x, upload.y);
            upload.tile.dispose();
        }
        pending.clear();
    }

    /** Packs a size x size tile filled via {@code fn} and returns the raw, unmirrored region. */
    static TextureRegion renderTile(String name, int size, CoverageFn fn){
        PixmapPacker packer = UI.packer;
        Rect rect = packer.pack(name, size, size);
        Page page = packer.getPage(name);
        Pixmap tile = new Pixmap(size, size);

        int baseX = (int)rect.x, baseY = (int)rect.y;

        for(int y = 0; y < size; y++){
            for(int x = 0; x < size; x++){
                float coverage = fn.get(x, y);
                tile.setRaw(x, y, Color.rgba8888(1f, 1f, 1f, coverage));
            }
        }

        //keep the page pixmap in sync, as fonts may reupload the whole page from it later
        page.image.draw(tile, baseX, baseY, false);
        pending.add(new PendingUpload(page, tile, baseX, baseY));

        return new TextureRegion(page.texture, baseX, baseY, size, size);
    }

    /** Packs a size x size tile filled via {@code fn}; returns {topLeft, topRight, bottomLeft, bottomRight}. */
    static TextureRegion[] renderCorner(String name, int size, CoverageFn fn){
        TextureRegion topRight = renderTile(name, size, fn);

        //top-right corner tile, exactly as generated
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

        return new CornerPatchDrawable(patch);
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

    private static class PendingUpload{
        final Page page;
        final Pixmap tile;
        final int x, y;

        PendingUpload(Page page, Pixmap tile, int x, int y){
            this.page = page;
            this.tile = tile;
            this.x = x;
            this.y = y;
        }
    }

    /** Nine-patch drawable that uploads pending corner tiles before drawing. */
    private static class CornerPatchDrawable extends NinePatchDrawable{
        CornerPatchDrawable(NinePatch patch){
            super(patch);
        }

        CornerPatchDrawable(CornerPatchDrawable drawable){
            super(drawable);
        }

        @Override
        public void draw(float x, float y, float width, float height){
            uploadPending();
            super.draw(x, y, width, height);
        }

        @Override
        public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
            uploadPending();
            super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }

        @Override
        public NinePatchDrawable tint(Color tint){
            CornerPatchDrawable drawable = new CornerPatchDrawable(this);
            drawable.setPatch(new NinePatch(drawable.getPatch(), tint));
            return drawable;
        }

        @Override
        public NinePatchDrawable tint(Color top, Color bottom){
            CornerPatchDrawable drawable = new CornerPatchDrawable(this);
            drawable.setPatch(new NinePatch(drawable.getPatch(), top, bottom));
            return drawable;
        }
    }
}