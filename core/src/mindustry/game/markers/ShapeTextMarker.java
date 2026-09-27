package mindustry.game.markers;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;

/** Displays text above a shape. */
public class ShapeTextMarker extends PosMarker{
    public @Multiline String text = "frog";
    public float fontSize = 1f, textHeight = 7f;
    public @LabelFlag byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;
    public @Alignment int textAlign = Align.center;
    public @Alignment(ver = false) int lineAlign = Align.center;

    public float radius = 6f, rotation = 0f;
    public int sides = 4;
    public Color color = Color.valueOf("ffd37f");

    // Cached localized text.
    private transient String fetchedText;

    public ShapeTextMarker(String text, float x, float y){
        this.text = text;
        this.pos.set(x, y);
    }

    public ShapeTextMarker(String text, float x, float y, float radius){
        this.text = text;
        this.pos.set(x, y);
        this.radius = radius;
    }

    public ShapeTextMarker(String text, float x, float y, float radius, float rotation){
        this.text = text;
        this.pos.set(x, y);
        this.radius = radius;
        this.rotation = rotation;
    }

    public ShapeTextMarker(String text, float x, float y, float radius, float rotation, float textHeight){
        this.text = text;
        this.pos.set(x, y);
        this.radius = radius;
        this.rotation = rotation;
        this.textHeight = textHeight;
    }

    public ShapeTextMarker(String text, float x, float y, float radius, float rotation, float textHeight, int textAlign){
        this.text = text;
        this.pos.set(x, y);
        this.radius = radius;
        this.rotation = rotation;
        this.textHeight = textHeight;
        this.textAlign = textAlign;
    }

    public ShapeTextMarker(){
    }

    @Override
    public void draw(float scaleFactor){
        //in case some idiot decides to make 9999999 sides and freeze the game
        int sides = Math.min(this.sides, 300);

        Draw.z(drawLayer);
        Lines.stroke(3f * scaleFactor, Pal.gray);
        Lines.poly(pos.x, pos.y, sides, (radius + 1f) * scaleFactor, rotation);
        Lines.stroke(scaleFactor, color);
        Lines.poly(pos.x, pos.y, sides, (radius + 1f) * scaleFactor, rotation);
        Draw.reset();

        if(fetchedText == null){
            fetchedText = fetchText(text);
        }

        // font size cannot be 0
        if(Mathf.equal(fontSize, 0f)) return;

        WorldLabel.drawAt(fetchedText, pos.x, pos.y + radius * scaleFactor + textHeight * scaleFactor, drawLayer, flags, fontSize * scaleFactor, textAlign, lineAlign);
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case fontSize -> fontSize = (float)p1;
                case textHeight -> textHeight = (float)p1;
                case textAlign -> textAlign = (int)p1;
                case lineAlign -> lineAlign = (int)p1;
                case outline -> flags = (byte)Pack.bitmask(flags, WorldLabel.flagOutline, !Mathf.equal((float)p1, 0f));
                case labelFlags -> flags = (byte)Pack.bitmask(flags, WorldLabel.flagBackground, !Mathf.equal((float)p1, 0f));
                case radius -> radius = (float)p1;
                case rotation -> rotation = (float)p1;
                case color -> color.fromDouble(p1);
                case shape -> sides = (int)p1;
            }
        }

        if(!Double.isNaN(p2)){
            switch(type){
                case labelFlags -> flags = (byte)Pack.bitmask(flags, WorldLabel.flagOutline, !Mathf.equal((float)p2, 0f));
            }
        }
    }

    @Override
    public void setText(String text, boolean fetch){
        this.text = text;
        if(fetch){
            fetchedText = fetchText(this.text);
        }else{
            fetchedText = this.text;
        }
    }
}
