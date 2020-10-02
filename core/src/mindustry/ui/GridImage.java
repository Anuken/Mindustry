package mindustry.ui;

import arc.graphics.g2d.Fill;
import arc.scene.Element;

public class GridImage extends Element{
    private int imageWidth, imageHeight;

    public GridImage(int w, int h){
        this.imageWidth = w;
        this.imageHeight = h;
    }

    @Override
    public void draw(){
        float xspace = (getWidth() / imageWidth);
        float yspace = (getHeight() / imageHeight);
        float s = 1f;

        int minspace = 10;

        int jumpx = (int)(Math.max(minspace, xspace) / xspace);
        int jumpy = (int)(Math.max(minspace, yspace) / yspace);

        for(int x = 0; x <= imageWidth; x += jumpx){
            Fill.crect((int)(this.x + xspace * x - s), y - s, 2, getHeight() + (x == imageWidth ? 1 : 0));
        }

        for(int y = 0; y <= imageHeight; y += jumpy){
            Fill.crect(x - s, (int)(this.y + y * yspace - s), getWidth(), 2);
        }
    }

    public void setImageSize(int w, int h){
        this.imageWidth = w;
        this.imageHeight = h;
    }
}
