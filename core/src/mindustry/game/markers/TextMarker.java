package mindustry.game.markers;

import arc.math.*;
import arc.util.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.gen.*;
import mindustry.logic.*;

/** Displays text at a location. */
public class TextMarker extends PosMarker{
    public @Multiline String text = "uwu";
    public float fontSize = 1f;
    public @LabelFlag byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;
    public @Alignment int textAlign = Align.center;
    public @Alignment(ver = false) int lineAlign = Align.center;

    // Cached localized text.
    private transient String fetchedText;

    public TextMarker(String text, float x, float y, float fontSize, byte flags){
        this.text = text;
        this.fontSize = fontSize;
        this.flags = flags;
        this.pos.set(x, y);
    }

    public TextMarker(String text, float x, float y){
        this.text = text;
        this.pos.set(x, y);
    }

    public TextMarker(){
    }

    @Override
    public void draw(float scaleFactor){
        // font size cannot be 0
        if(Mathf.equal(fontSize, 0f)) return;

        if(fetchedText == null){
            fetchedText = fetchText(text);
        }

        WorldLabel.drawAt(fetchedText, pos.x, pos.y, drawLayer, flags, fontSize * scaleFactor, textAlign, lineAlign);
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case fontSize -> fontSize = (float)p1;
                case textAlign -> textAlign = (int)p1;
                case lineAlign -> lineAlign = (int)p1;
                case outline -> flags = (byte)Pack.bitmask(flags, WorldLabel.flagOutline, !Mathf.equal((float)p1, 0f));
                case labelFlags -> flags = (byte)Pack.bitmask(flags, WorldLabel.flagBackground, !Mathf.equal((float)p1, 0f));
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
