package arc.scene.ui;

import arc.graphics.Color;

public class ColorImage extends Image{
    private Color set;

    public ColorImage(Color set){
        this.set = new Color(set);
    }

    @Override
    public void draw(){
        setColor(set);
        super.draw();
    }
}
