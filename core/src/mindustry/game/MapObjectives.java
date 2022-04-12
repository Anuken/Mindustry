package mindustry.game;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class MapObjectives{
    public static Prov<MapObjective>[] allObjectiveTypes = new Prov[]{
        ResearchObjective::new
    };

    public static Prov<ObjectiveMarker>[] allMarkerTypes = new Prov[]{
        TextMarker::new, ShapeMarker::new
    };

    public static class ResearchObjective extends MapObjective{
        public UnlockableContent content = Items.copper;

        public ResearchObjective(UnlockableContent content){
            this.content = content;
        }

        public ResearchObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.research", content.emoji(), content.localizedName);
        }

        @Override
        public boolean complete(){
            return content.unlocked();
        }

        @Override
        public @Nullable String details(){
            return super.details();
        }
    }

    public static abstract class MapObjective{
        public ObjectiveMarker[] markers = {};

        public boolean complete(){
            return false;
        }

        public void update(){

        }

        /** Reset internal state, if any. */
        public void reset(){

        }

        public @Nullable String text(){
            return null;
        }

        public @Nullable String details(){
            return null;
        }
    }

    public static class ShapeMarker extends ObjectiveMarker{
        public float x, y, radius = 6f, rotation = 0f;
        public int sides = 4;
        public Color color = Pal.accent;

        public ShapeMarker(float x, float y){
            this.x = x;
            this.y = y;
        }

        public ShapeMarker(){
        }

        @Override
        public void draw(){
            Lines.stroke(3f, Pal.gray);
            Lines.poly(x, y, sides, radius + 1f, rotation);
            Lines.stroke(1f, color);
            Lines.poly(x, y, sides, radius + 1f, rotation);
            Draw.reset();
        }
    }

    public static class TextMarker extends ObjectiveMarker{
        public String text = "sample text";
        public float x, y, fontSize = 1f;
        public byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;

        public TextMarker(String text, float x, float y, float fontSize, byte flags){
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontSize = fontSize;
            this.flags = flags;
        }

        public TextMarker(String text, float x, float y){
            this.text = text;
            this.x = x;
            this.y = y;
        }

        public TextMarker(){
        }

        @Override
        public void draw(){
            WorldLabel.drawAt(text, x, y, Draw.z(), flags, fontSize);
        }
    }

    /** Marker used for drawing UI to indicate something along with an objective. */
    public static abstract class ObjectiveMarker{
        /** makes sure markers are only added once */
        public transient boolean wasAdded;

        /** Called in the overlay draw layer.*/
        public void draw(){}
        /** Add any UI elements necessary. */
        public void added(){}
        /** Remove any UI elements, if necessary. */
        public void removed(){}
    }
}
