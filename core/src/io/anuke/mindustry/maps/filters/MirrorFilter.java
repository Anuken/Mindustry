package io.anuke.mindustry.maps.filters;

import io.anuke.arc.function.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.maps.filters.FilterOption.*;
import io.anuke.mindustry.world.*;

public class MirrorFilter extends GenerateFilter{
    private final Vector2 v1 = new Vector2(), v2 = new Vector2(), v3 = new Vector2();

    int angle = 45;

    {
        options(new SliderOption("angle", () -> angle, f -> angle = (int)f, 0, 360, 45));
    }

    @Override
    protected void apply(){
        v1.trnsExact(angle - 90, 1f);
        v2.set(v1).scl(-1f);

        v1.add(in.width/2f - 0.5f, in.height/2f - 0.5f);
        v2.add(in.width/2f - 0.5f, in.height/2f - 0.5f);

        v3.set(in.x, in.y);

        if(!left(v1, v2, v3)){
            mirror(v3, v1.x, v1.y, v2.x, v2.y);
            Tile tile = in.tile(v3.x, v3.y);
            in.floor = tile.floor();
            if(!tile.block().synthetic()){
                in.block = tile.block();
            }
            in.ore = tile.overlay();
        }
    }

    @Override
    public void draw(Image image){
        super.draw(image);

        Vector2 vsize = Scaling.fit.apply(image.getDrawable().getMinWidth(), image.getDrawable().getMinHeight(), image.getWidth(), image.getHeight());
        float imageWidth = Math.max(vsize.x, vsize.y);
        float imageHeight = Math.max(vsize.y, vsize.x);

        float size = Math.max(image.getWidth() *2, image.getHeight()*2);
        Consumer<Vector2> clamper = v ->
            v.clamp(
                image.getX() + image.getWidth()/2f - imageWidth/2f,
                image.getX() + image.getWidth()/2f + imageWidth/2f,
                image.getY() + image.getHeight()/2f - imageHeight/2f,
                image.getY() + image.getHeight()/2f + imageHeight/2f);

        clamper.accept(Tmp.v1.trns(angle - 90, size).add(image.getWidth()/2f + image.getX(), image.getHeight()/2f + image.getY()));
        clamper.accept(Tmp.v2.set(Tmp.v1).sub(image.getWidth()/2f + image.getX(), image.getHeight()/2f + image.getY()).rotate(180f).add(image.getWidth()/2f + image.getX(), image.getHeight()/2f + image.getY()));

        Lines.stroke(Scl.scl(3f), Pal.accent);
        Lines.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
        Draw.reset();
    }

    void mirror(Vector2 p, float x0, float y0, float x1, float y1){
        //special case: uneven map mirrored at 45 degree angle
        if(in.width != in.height && angle % 90 != 0){
            p.x = (p.x - in.width/2f) * -1 + in.width/2f;
            p.y = (p.y - in.height/2f) * -1 + in.height/2f;
        }else{
            float dx = x1 - x0;
            float dy = y1 - y0;

            float a = (dx * dx - dy * dy) / (dx * dx + dy * dy);
            float b = 2 * dx * dy / (dx * dx + dy * dy);

            p.set((a * (p.x - x0) + b * (p.y - y0) + x0), (b * (p.x - x0) - a * (p.y - y0) + y0));
        }
    }

    boolean left(Vector2 a, Vector2 b, Vector2 c){
        return ((b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)) > 0;
    }
}
