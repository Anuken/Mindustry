package arc.scene.style;

import arc.graphics.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Tmp;

/**
 * Draws a {@link TextureRegion} repeatedly to fill the area, instead of stretching it.
 * @author Nathan Sweet
 */
public class TiledDrawable extends TextureRegionDrawable{
    private final Color color = new Color(1, 1, 1, 1);
    private float tileWidth, tileHeight;

    public TiledDrawable(){
        super();
    }

    public TiledDrawable(TextureRegion region){
        super(region);
    }

    public TiledDrawable(TextureRegionDrawable drawable){
        super(drawable);
    }

    @Override
    public void setRegion(TextureRegion region){
        super.setRegion(region);
        this.tileWidth = region.width;
        this.tileHeight = region.height;
    }

    public void setTileSize(float w, float h){
        tileWidth = w;
        tileHeight = h;
    }

    @Override
    public void draw(float x, float y, float width, float height){
        TextureRegion region = getRegion();
        float regionWidth = tileWidth, regionHeight = tileHeight;
        int fullX = (int)(width / regionWidth), fullY = (int)(height / regionHeight);
        float remainingX = width - regionWidth * fullX, remainingY = height - regionHeight * fullY;
        float startX = x, startY = y;
        Draw.color(color);
        for(int i = 0; i < fullX; i++){
            y = startY;
            for(int ii = 0; ii < fullY; ii++){
                Draw.rect(region, x, y, regionWidth, regionHeight);
                y += regionHeight;
            }
            x += regionWidth;
        }
        Texture texture = region.texture;
        float u = region.u;
        float v2 = region.v2;
        if(remainingX > 0){
            // Right edge.
            float u2 = u + remainingX / texture.width;
            float v = region.v;
            y = startY;
            for(int ii = 0; ii < fullY; ii++){
                Tmp.tr1.set(texture);
                Tmp.tr1.set(u, v2, u2, v);

                Draw.rect(Tmp.tr1, x + remainingX/2f, y + remainingY/2f, remainingX, remainingY);
                y += regionHeight;
            }
            // Upper right corner.
            if(remainingY > 0){
                v = v2 - remainingY / texture.height;
                Tmp.tr1.set(texture);
                Tmp.tr1.set(u, v2, u2, v);

                Draw.rect(Tmp.tr1, x + remainingX/2f, y + remainingY/2f, remainingX, remainingY);
            }
        }
        if(remainingY > 0){
            // Top edge.
            float u2 = region.u2;
            float v = v2 - remainingY / texture.height;
            x = startX;
            for(int i = 0; i < fullX; i++){

                Tmp.tr1.set(texture);
                Tmp.tr1.set(u, v2, u2, v);

                Draw.rect(Tmp.tr1, x + remainingX/2f, y + remainingY/2f, remainingX, remainingY);
                x += regionWidth;
            }
        }
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX,
                     float scaleY, float rotation){
        throw new UnsupportedOperationException();
    }

    public Color getColor(){
        return color;
    }

    @Override
    public TiledDrawable tint(Color tint){
        TiledDrawable drawable = new TiledDrawable(this);
        drawable.color.set(tint);
        drawable.setLeftWidth(getLeftWidth());
        drawable.setRightWidth(getRightWidth());
        drawable.setTopHeight(getTopHeight());
        drawable.setBottomHeight(getBottomHeight());
        return drawable;
    }
}
