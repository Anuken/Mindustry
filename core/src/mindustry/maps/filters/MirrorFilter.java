package mindustry.maps.filters;

import arc.graphics.g2d.*;
import arc.math.*;
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
    public float axisX = 0.5f, axisY = 0.5f;

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

        //place the mirror line at the configured normalized pivot
        v1.add(coord(axisX, in.width), coord(axisY, in.height));
        v2.add(coord(axisX, in.width), coord(axisY, in.height));

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
        drawRect(image, Tmp.r1);
        if(Tmp.r1.width <= 0f || Tmp.r1.height <= 0f) return;

        float px = Tmp.r1.x + Mathf.clamp(axisX, 0f, 1f) * Tmp.r1.width;
        float py = Tmp.r1.y + Mathf.clamp(axisY, 0f, 1f) * Tmp.r1.height;

        Tmp.v1.trns(angle - 90, 1f);
        clipHalfLine(Tmp.v1, Tmp.r1.x - px, Tmp.r1.y - py, Tmp.r1.x + Tmp.r1.width - px, Tmp.r1.y + Tmp.r1.height - py);
        Tmp.v2.set(Tmp.v1).scl(-1f); //opposite of v1

        Tmp.v1.add(px + image.x, py + image.y);
        Tmp.v2.add(px + image.x, py + image.y);

        Lines.stroke(Scl.scl(3f), Pal.accent);
        Lines.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
        Draw.color(Pal.accent);
        Fill.circle(px + image.x, py + image.y, Scl.scl(4f));
        Draw.color(Pal.darkestGray);
        Fill.circle(px + image.x, py + image.y, Scl.scl(1.8f));
        Draw.reset();
    }

    public static void drawRect(Image image, Rect out){
        if(image == null || image.getDrawable() == null || out == null){
            if(out != null) out.set(0f, 0f, 0f, 0f);
            return;
        }

        Vec2 size = Scaling.fit.apply(image.getDrawable().getMinWidth(), image.getDrawable().getMinHeight(), image.getWidth(), image.getHeight());
        //account for fit scaling so handles stay on the actual map area
        out.set((image.getWidth() - size.x)/2f, (image.getHeight() - size.y)/2f, size.x, size.y);
    }

    float coord(float axis, int size){
        if(size <= 1) return 0f;
        return Mathf.clamp(axis, 0f, 1f) * (size - 1f);
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
