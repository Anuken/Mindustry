package mindustry.ui;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import mindustry.graphics.Pal;

public class BorderImage extends Image{
    public float thickness = 4f;
    public Color borderColor = Pal.gray;

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

    public BorderImage border(Color color){
        this.borderColor = color;
        return this;
    }

    @Override
    public void draw(){
        super.draw();

        Draw.color(borderColor);
        Draw.alpha(parentAlpha);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
        Draw.reset();
    }
}
