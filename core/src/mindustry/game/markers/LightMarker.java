package mindustry.game.markers;

import arc.graphics.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

/** Displays a single point light. */
public class LightMarker extends PosMarker{
    public float radius = 5f;
    public Color color = Color.valueOf("ffd37f");

    public LightMarker(int x, int y){
        this.pos.set(x, y);
    }

    public LightMarker(int x, int y, Color color){
        this.pos.set(x, y);
        this.color = color;
    }

    public LightMarker(int x, int y, float radius, Color color){
        this.pos.set(x, y);
        this.radius = radius;
        this.color = color;
    }

    public LightMarker(){
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
                case color -> color.fromDouble(p1);
            }
        }
    }
}
