package mindustry.game.markers;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.graphics.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

/** Displays a line from pos1 to pos2. */
public class LineMarker extends PosMarker{
    public @TilePos Vec2 endPos = new Vec2();
    public float stroke = 1f;
    public boolean outline = true;
    public Color color1 = Color.valueOf("ffd37f");
    public Color color2 = Color.valueOf("ffd37f");

    public LineMarker(float x1, float y1, float x2, float y2, float stroke){
        this.stroke = stroke;
        this.pos.set(x1, y1);
        this.endPos.set(x2, y2);
    }

    public LineMarker(float x1, float y1, float x2, float y2){
        this.pos.set(x1, y1);
        this.endPos.set(x2, y2);
    }

    public LineMarker(){
    }

    @Override
    public void draw(float scaleFactor){
        Draw.z(drawLayer);
        if(outline){
            Lines.stroke((stroke + 2f) * scaleFactor, Pal.gray);
            Lines.line(pos.x, pos.y, endPos.x, endPos.y);
        }

        Lines.stroke(stroke * scaleFactor, Color.white);
        Lines.line(pos.x, pos.y, color1, endPos.x, endPos.y, color2);
    }

    @Override
    public void drawLight(float scaleFactor){
        renderer.lights.line(pos.x, pos.y, endPos.x, endPos.y, stroke, color1, color1.a);
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case endPos -> endPos.x = (float)p1 * tilesize;
                case stroke -> stroke = (float)p1;
                case color -> color1.set(color2.fromDouble(p1));
                case outline -> outline = !Mathf.equal((float)p1, 0f);
            }
        }

        if(!Double.isNaN(p2)){
            switch(type){
                case endPos -> endPos.y = (float)p2 * tilesize;
            }
        }

        if(!Double.isNaN(p1) && !Double.isNaN(p2)){
            switch(type){
                case posi -> ((int)p1 == 0 ? pos : (int)p1 == 1 ? endPos : Tmp.v1).x = (float)p2 * tilesize;
                case colori -> ((int)p1 == 0 ? color1 : (int)p1 == 1 ? color2 : Tmp.c1).fromDouble(p2);
            }
        }

        if(!Double.isNaN(p1) && !Double.isNaN(p3)){
            switch(type){
                case posi -> ((int)p1 == 0 ? pos : (int)p1 == 1 ? endPos : Tmp.v1).y = (float)p3 * tilesize;
            }
        }
    }
}
