package mindustry.ui;

import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class BorderImage extends Image{
    public float thickness = 4f, pad = 0f;
    public Color borderColor = Pal.gray;
    public boolean forceNearest = false, drawAlpha = false;
    public Color alphaColor = Color.gray.cpy();

    public BorderImage(){

    }

    public BorderImage(Texture texture){
        super(texture);
    }

    public BorderImage(Texture texture, float thick){
        super(texture);
        thickness = thick;
    }

    public BorderImage(TextureRegion region, float thick){
        super(region);
        thickness = thick;
    }

    public BorderImage(Drawable region){
        super(region);
    }

    public BorderImage border(Color color){
        this.borderColor = color;
        return this;
    }

    @Override
    public void draw(){
        TextureFilter prev = TextureFilter.linear;

        if(forceNearest && getDrawable() instanceof TextureRegionDrawable draw){
            prev = draw.getRegion().texture.getMinFilter();
            draw.getRegion().texture.setFilter(TextureFilter.nearest);
        }
        if(drawAlpha){
            Draw.color(alphaColor, parentAlpha);
            Vec2 v = scaling.apply(imageWidth, imageHeight, width, height).scl(1f / width, 1f / height);
            TextureRegion region = ((TextureRegionDrawable)Tex.alphaBg).getRegion();
            Tmp.tr1.set(region.texture);
            Tmp.tr1.set(region.u, region.v, Mathf.lerp(region.u, region.u2, v.x), Mathf.lerp(region.v, region.v2, v.y));
            Draw.rect(Tmp.tr1, x + imageX + imageWidth * scaleX/2f, y + imageY + imageHeight * scaleY/2f, imageWidth * scaleX, imageHeight * scaleY);
        }

        super.draw();

        Draw.color(borderColor);
        Draw.alpha(parentAlpha);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x + imageX - pad, y + imageY - pad, imageWidth * scaleX + pad*2, imageHeight * scaleY + pad*2);
        Draw.reset();

        if(forceNearest && getDrawable() instanceof TextureRegionDrawable draw){
            draw.getRegion().texture.setFilter(prev);
        }
    }
}
