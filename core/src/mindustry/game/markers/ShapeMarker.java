package mindustry.game.markers;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.graphics.*;
import mindustry.logic.*;

/** Displays a shape with an outline and color. */
public class ShapeMarker extends PosMarker{
    public float radius = 8f, rotation = 0f, stroke = 1f, startAngle = 0f, endAngle = 360f;
    public boolean fill = false, outline = true;
    public int sides = 4;
    public Color color = Color.valueOf("ffd37f");

    public ShapeMarker(float x, float y){
        this.pos.set(x, y);
    }

    public ShapeMarker(float x, float y, float radius, float rotation){
        this.pos.set(x, y);
        this.radius = radius;
        this.rotation = rotation;
    }

    public ShapeMarker(){
    }

    @Override
    public void draw(float scaleFactor){
        //in case some idiot decides to make 9999999 sides and freeze the game
        int sides = Math.min(this.sides, 200);

        Draw.z(drawLayer);
        if(!fill){
            if(outline){
                Lines.stroke((stroke + 2f) * scaleFactor, Pal.gray);
                Lines.poly(pos.x, pos.y, sides, (radius + 1f) * scaleFactor, rotation + startAngle, rotation + endAngle);
            }

            Lines.stroke(stroke * scaleFactor, color);
            Lines.poly(pos.x, pos.y, sides, (radius + 1f) * scaleFactor, rotation + startAngle, rotation + endAngle);
        }else{
            Draw.color(color);
            if(startAngle < endAngle){
                Fill.arc(pos.x, pos.y, radius * scaleFactor, (endAngle - startAngle) / 360f, rotation + startAngle, sides);
            }else{
                Fill.arc(pos.x, pos.y, radius * scaleFactor, (startAngle - endAngle) / 360f, rotation + endAngle, sides);
            }
        }
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case radius -> radius = (float)p1;
                case stroke -> stroke = (float)p1;
                case outline -> outline = !Mathf.equal((float)p1, 0f);
                case rotation -> rotation = (float)p1;
                case color -> color.fromDouble(p1);
                case shape -> sides = (int)p1;
                case arc -> startAngle = (float)p1;
            }
        }

        if(!Double.isNaN(p2)){
            switch(type){
                case shape -> fill = !Mathf.equal((float)p2, 0f);
                case arc -> endAngle = (float)p2;
            }
        }

        if(!Double.isNaN(p3)){
            if(type == LMarkerControl.shape){
                outline = !Mathf.equal((float)p3, 0f);
            }
        }
    }
}
