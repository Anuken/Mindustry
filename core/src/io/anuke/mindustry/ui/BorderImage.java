package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Unit;

public class BorderImage extends Image{
    private float thickness = 3f;

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

    @Override
    public void draw(){
        super.draw();

        float scaleX = getScaleX();
        float scaleY = getScaleY();

        Draw.color(Palette.accent);
        Lines.stroke(Unit.dp.scl(thickness));
        Lines.rect(x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
        Draw.reset();
    }
}
