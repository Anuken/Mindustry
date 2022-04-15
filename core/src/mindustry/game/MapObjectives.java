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
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MapObjectives{
    public static Prov<MapObjective>[] allObjectiveTypes = new Prov[]{
        ResearchObjective::new, BuildCountObjective::new, UnitCountObjective::new, ItemObjective::new,
        CommandModeObjective::new, CoreItemObjective::new, DestroyCoreObjective::new
    };

    public static Prov<ObjectiveMarker>[] allMarkerTypes = new Prov[]{
        TextMarker::new, ShapeMarker::new, ShapeTextMarker::new
    };

    /** Research a specific piece of content in the tech tree. */
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
    }

    /** Have a certain amount of item in your core. */
    public static class ItemObjective extends MapObjective{
        public Item item = Items.copper;
        public int amount = 1;

        public ItemObjective(Item item, int amount){
            this.item = item;
            this.amount = amount;
        }

        public ItemObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.item", state.rules.defaultTeam.items().get(item), amount, item.emoji(), item.localizedName);
        }

        @Override
        public boolean complete(){
            return state.rules.defaultTeam.items().has(item, amount);
        }
    }

    /** Get a certain item in your core (through a block, not manually.) */
    public static class CoreItemObjective extends MapObjective{
        public Item item = Items.copper;
        public int amount = 2;

        public CoreItemObjective(Item item, int amount){
            this.item = item;
            this.amount = amount;
        }

        public CoreItemObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.coreitem", state.stats.coreItemCount.get(item), amount, item.emoji(), item.localizedName);
        }

        @Override
        public boolean complete(){
            return state.stats.coreItemCount.get(item) >= amount;
        }
    }

    /** Build a certain amount of a block. */
    public static class BuildCountObjective extends MapObjective{
        public Block block = Blocks.conveyor;
        public int count = 1;

        public BuildCountObjective(Block block, int count){
            this.block = block;
            this.count = count;
        }

        public BuildCountObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.build", count, block.emoji(), block.localizedName);
        }

        @Override
        public boolean complete(){
            return state.stats.placedBlockCount.get(block, 0) >= count;
        }
    }

    /** Produce a certain amount of a unit. */
    public static class UnitCountObjective extends MapObjective{
        public UnitType unit = UnitTypes.dagger;
        public int count = 1;

        public UnitCountObjective(UnitType unit, int count){
            this.unit = unit;
            this.count = count;
        }

        public UnitCountObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.buildunit", count, unit.emoji(), unit.localizedName);
        }

        @Override
        public boolean complete(){
            return state.rules.defaultTeam.data().countType(unit) >= count;
        }
    }

    /** Command any unit to do anything. Always compete in headless mode. */
    public static class CommandModeObjective extends MapObjective{

        @Override
        public String text(){
            return Core.bundle.get("objective.command");
        }

        @Override
        public boolean complete(){
            return headless || control.input.selectedUnits.contains(u -> u.isCommandable() && u.command().hasCommand());
        }
    }

    /** Destroy all enemy core(s). */
    public static class DestroyCoreObjective extends MapObjective{

        @Override
        public String text(){
            return Core.bundle.get("objective.destroycore");
        }

        @Override
        public boolean complete(){
            return state.rules.waveTeam.cores().size == 0;
        }
    }

    /** Base abstract class for any in-map objective. */
    public static abstract class MapObjective{
        public String[] flagsAdded = {};
        public String[] flagsRemoved = {};
        public ObjectiveMarker[] markers = {};
        public @Nullable String details;

        public MapObjective withFlags(String... flags){
            this.flagsAdded = flags;
            return this;
        }

        public MapObjective withFlagsRemoved(String... flags){
            this.flagsRemoved = flags;
            return this;
        }

        public MapObjective withMarkers(ObjectiveMarker... markers){
            this.markers = markers;
            return this;
        }

        public MapObjective withDetails(String details){
            this.details = details;
            return this;
        }

        public boolean complete(){
            return false;
        }

        /** Called immediately after this objective is completed and removed from the rules. */
        public void completed(){

        }

        public void update(){

        }

        /** Reset internal state, if any. */
        public void reset(){

        }

        /** Basic mission display text. */
        public @Nullable String text(){
            return null;
        }

        /** Details that appear upon click. */
        public @Nullable String details(){
            return details;
        }
    }

    /** Displays text above a shape. */
    public static class ShapeTextMarker extends ObjectiveMarker{
        public String text = "frog";
        public float x, y, fontSize = 1f, textHeight = 7f;
        public byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;

        public float radius = 6f, rotation = 0f;
        public int sides = 4;
        public Color color = Pal.accent;

        //cached localized text
        private transient String fetchedText;

        public ShapeTextMarker(String text, float x, float y){
            this.text = text;
            this.x = x;
            this.y = y;
        }

        public ShapeTextMarker(String text, float x, float y, float radius){
            this.text = text;
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public ShapeTextMarker(String text, float x, float y, float radius, float rotation){
            this.text = text;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.rotation = rotation;
        }

        public ShapeTextMarker(String text, float x, float y, float radius, float rotation, float textHeight){
            this.text = text;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.rotation = rotation;
            this.textHeight = textHeight;
        }

        public ShapeTextMarker(){

        }

        @Override
        public void draw(){
            Lines.stroke(3f, Pal.gray);
            Lines.poly(x, y, sides, radius + 1f, rotation);
            Lines.stroke(1f, color);
            Lines.poly(x, y, sides, radius + 1f, rotation);
            Draw.reset();

            if(fetchedText == null){
                fetchedText = text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text;
            }

            WorldLabel.drawAt(text, x, y + radius + textHeight, Draw.z(), flags, fontSize);
        }
    }

    /** Displays a shape with an outline and color. */
    public static class ShapeMarker extends ObjectiveMarker{
        public float x, y, radius = 6f, rotation = 0f;
        public int sides = 4;
        public Color color = Pal.accent;

        public ShapeMarker(float x, float y){
            this.x = x;
            this.y = y;
        }

        public ShapeMarker(float x, float y, float radius, float rotation){
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.rotation = rotation;
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

    /** Displays text at a location. */
    public static class TextMarker extends ObjectiveMarker{
        public String text = "uwu";
        public float x, y, fontSize = 1f;
        public byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;
        //cached localized text
        private transient String fetchedText;

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
            if(fetchedText == null){
                fetchedText = text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text;
            }

            WorldLabel.drawAt(fetchedText, x, y, Draw.z(), flags, fontSize);
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
