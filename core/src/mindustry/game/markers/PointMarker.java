package mindustry.game.markers;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

/** Displays a circle in the world. */
public class PointMarker extends PosMarker{
    public float radius = 5f, stroke = 11f;
    public Color color = Color.valueOf("f25555");

    public PointMarker(int x, int y){
        this.pos.set(x, y);
    }

    public PointMarker(int x, int y, Color color){
        this.pos.set(x, y);
        this.color = color;
    }

    public PointMarker(int x, int y, float radius, float stroke, Color color){
        this.pos.set(x, y);
        this.stroke = stroke;
        this.radius = radius;
        this.color = color;
    }

    public PointMarker(){
    }

    @Override
    public void draw(float scaleFactor){
        float rad = radius * tilesize * scaleFactor;
        float fin = Interp.pow2Out.apply((Time.globalTime / 100f) % 1f);

        Draw.z(drawLayer);
        Lines.stroke(Scl.scl((1f - fin) * stroke + 0.1f), color);
        Lines.circle(pos.x, pos.y, rad * fin);
    }

    @Override
    public void drawLight(float scaleFactor){
        float rad = radius * tilesize * scaleFactor;

        renderer.lights.add(pos.x, pos.y, rad, color, color.a);
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case radius -> radius = (float)p1;
                case stroke -> stroke = (float)p1;
                case color -> color.fromDouble(p1);
            }
        }
    }
}
