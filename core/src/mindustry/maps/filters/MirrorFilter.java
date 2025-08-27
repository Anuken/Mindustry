package mindustry.maps.filters;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

public class MirrorFilter extends GenerateFilter{
    private static final Vec2 v1 = new Vec2(), v2 = new Vec2(), v3 = new Vec2();

    public int angle = 45;
    public boolean rotate = false;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("angle", () -> angle, f -> angle = (int)f, 0, 360, 15),
            new ToggleOption("rotate", () -> rotate, f -> rotate = f)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockMetalFloor5;
    }

    @Override
    public void apply(GenerateInput in){
        v1.trnsExact(angle - 90, 1f);
        v2.set(v1).scl(-1f);

        v1.add(in.width/2f - 0.5f, in.height/2f - 0.5f);
        v2.add(in.width/2f - 0.5f, in.height/2f - 0.5f);

        v3.set(in.x, in.y);

        if(!left(v1, v2, v3)){
            mirror(in.width, in.height, v3, v1.x, v1.y, v2.x, v2.y);
            Tile tile = in.tile(v3.x, v3.y);
            in.floor = tile.floor();
            if(!tile.block().synthetic()){
                in.block = tile.block();
            }
            in.overlay = tile.overlay();
            in.packedData = tile.getPackedData();
        }
    }

    @Override
    public void draw(Image image){
        super.draw(image);
        float imageGetWidth = image.getWidth()/2f;
        float imageGetHeight = image.getHeight()/2f;
        float size = Math.max(imageGetWidth, imageGetHeight);

        Vec2 vsize = Scaling.fit.apply(image.getDrawable().getMinWidth(), image.getDrawable().getMinHeight(), imageGetWidth, imageGetHeight);

        Tmp.v1.trns(angle - 90, size);
        clipHalfLine(Tmp.v1, -vsize.x, -vsize.y, vsize.x, vsize.y);
        Tmp.v2.set(Tmp.v1).scl(-1f); //opposite of v1

        //adding back the coordinates of the center of the image
        Tmp.v1.add(imageGetWidth + image.x, imageGetHeight + image.y);
        Tmp.v2.add(imageGetWidth + image.x, imageGetHeight + image.y);

        Lines.stroke(Scl.scl(3f), Pal.accent);
        Lines.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
        Draw.reset();
    }

    void mirror(int width, int height, Vec2 p, float x0, float y0, float x1, float y1){
        //special case: uneven map mirrored at 45 degree angle (or someone might just want rotational symmetry)
        if((width != height && angle % 90 != 0) || rotate){
            p.x = width - p.x - 1;
            p.y = height - p.y - 1;
        }else{
            float dx = x1 - x0;
            float dy = y1 - y0;

            float a = (dx * dx - dy * dy) / (dx * dx + dy * dy);
            float b = 2 * dx * dy / (dx * dx + dy * dy);

            p.set((a * (p.x - x0) + b * (p.y - y0) + x0), (b * (p.x - x0) - a * (p.y - y0) + y0));
        }
    }

    boolean left(Vec2 a, Vec2 b, Vec2 c){
        return ((b.x - a.x)*(c.y - a.y) > (b.y - a.y)*(c.x - a.x));
    }

    void clipHalfLine(Vec2 v, float xmin, float ymin, float xmax, float ymax){
        //finds the coordinates of the intersection of the half line created by the vector at (0,0) with the clipping rectangle
        v.scl(1f / Math.max(Math.abs(v.x < 0 ? v.x / xmin : v.x / xmax), Math.abs(v.y < 0 ? v.y / ymin : v.y / ymax)));
    }
}
