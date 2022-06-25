package mindustry.game;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
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
        CommandModeObjective::new, CoreItemObjective::new, DestroyCoreObjective::new, DestroyUnitsObjective::new,
        TimerObjective::new, FlagObjective::new, DestroyBlockObjective::new, ProduceObjective::new,
        DestroyBlocksObjective::new
    };

    public static Prov<ObjectiveMarker>[] allMarkerTypes = new Prov[]{
        TextMarker::new, ShapeMarker::new, ShapeTextMarker::new, MinimapMarker::new
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

    /** Produce a specific piece of content in the tech tree (essentially research with different text). */
    public static class ProduceObjective extends MapObjective{
        public UnlockableContent content = Items.copper;

        public ProduceObjective(UnlockableContent content){
            this.content = content;
        }

        public ProduceObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.produce", content.emoji(), content.localizedName);
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

    /** Produce a certain amount of units. */
    public static class DestroyUnitsObjective extends MapObjective{
        public int count = 1;

        public DestroyUnitsObjective(int count){
            this.count = count;
        }

        public DestroyUnitsObjective(){
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.destroyunits", count);
        }

        @Override
        public boolean complete(){
            return state.stats.enemyUnitsDestroyed >= count;
        }
    }

    public static class TimerObjective extends MapObjective{
        public String text;
        public float countup;
        public float duration = 60f * 30f;

        public TimerObjective(String text, float duration){
            this.text = text;
            this.duration = duration;
        }

        public TimerObjective(){
        }

        @Override
        public boolean complete(){
            return countup >= duration;
        }

        @Override
        public void update(){
            countup += Time.delta;
        }

        @Override
        public void reset(){
            countup = 0f;
        }

        @Nullable
        @Override
        public String text(){
            if(text != null){
                int i = (int)((duration - countup) / 60f);
                StringBuilder timeString = new StringBuilder();

                int m = i / 60;
                int s = i % 60;
                if(m > 0){
                    timeString.append(m);
                    timeString.append(":");
                    if(s < 10){
                        timeString.append("0");
                    }
                }
                timeString.append(s);

                if(text.startsWith("@")){
                    return Core.bundle.format(text.substring(1), timeString.toString());
                }else{
                    return Core.bundle.formatString(text, timeString.toString());
                }
            }
            return text;
        }
    }

    public static class DestroyBlockObjective extends MapObjective{
        public int x, y;
        public Team team = Team.crux;
        public Block block = Blocks.router;

        public DestroyBlockObjective(Block block, int x, int y, Team team){
            this.block = block;
            this.x = x;
            this.y = y;
            this.team = team;
        }

        public DestroyBlockObjective(){
        }

        @Override
        public boolean complete(){
            var build = world.build(x, y);
            return build == null || build.team != team || build.block != block;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.destroyblock", block.emoji(), block.localizedName);
        }
    }

    public static class DestroyBlocksObjective extends MapObjective{
        public int[] positions = {};
        public Team team = Team.crux;
        public Block block = Blocks.router;

        public DestroyBlocksObjective(Block block, Team team, int... positions){
            this.block = block;
            this.team = team;
            this.positions = positions;
        }

        public DestroyBlocksObjective(){
        }

        public int progress(){
            int count = 0;
            for(var pos : positions){
                int x = Point2.x(pos), y = Point2.y(pos);

                var build = world.build(x, y);
                if(build == null || build.team != team || build.block != block){
                    count ++;
                }
            }
            return count;
        }

        @Override
        public boolean complete(){
            return progress() >= positions.length;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.destroyblocks", progress(), positions.length, block.emoji(), block.localizedName);
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

    /** Wait until a logic flag is set. */
    public static class FlagObjective extends MapObjective{
        public String flag = "flag", text;

        public FlagObjective(String flag, String text){
            this.flag = flag;
            this.text = text;
        }

        public FlagObjective(){
        }

        @Override
        public String text(){
            return text != null && text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text;
        }

        @Override
        public boolean complete(){
            return state.rules.objectiveFlags.contains(flag);
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
        public @Nullable String details;
        public String[] flagsAdded = {};
        public String[] flagsRemoved = {};
        public ObjectiveMarker[] markers = {};

        //TODO localize
        public String typeName(){
            return getClass().getSimpleName().replace("Objective", "");
        }

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

        /** Basic mission display text. If null, falls back to standard text. */
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

    /** Displays a circle on the minimap. */
    public static class MinimapMarker extends ObjectiveMarker{
        //in tiles.
        public float x, y, radius = 5f, stroke = 11f;
        public Color color = Team.crux.color;

        public MinimapMarker(float x, float y){
            this.x = x;
            this.y = y;
        }

        public MinimapMarker(float x, float y, Color color){
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public MinimapMarker(float x, float y, float radius, float stroke, Color color){
            this.x = x;
            this.y = y;
            this.stroke = stroke;
            this.radius = radius;
            this.color = color;
        }

        public MinimapMarker(){
        }

        @Override
        public void drawMinimap(MinimapRenderer minimap){
            minimap.transform(Tmp.v1.set(x * tilesize, y * tilesize));

            float rad = minimap.scale(radius * tilesize);
            float fin = Interp.pow2Out.apply((Time.globalTime / 100f) % 1f);

            Lines.stroke(Scl.scl((1f - fin) * stroke + 0.1f), color);
            Lines.circle(Tmp.v1.x, Tmp.v1.y, rad * fin);
            Draw.reset();
        }
    }

    /** Displays a shape with an outline and color. */
    public static class ShapeMarker extends ObjectiveMarker{
        public float x, y, radius = 6f, rotation = 0f, stroke = 1f;
        public boolean fill = false, outline = true;
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
            if(!fill){
                if(outline){
                    Lines.stroke(stroke + 2f, Pal.gray);
                    Lines.poly(x, y, sides, radius + 1f, rotation);
                }

                Lines.stroke(stroke, color);
                Lines.poly(x, y, sides, radius + 1f, rotation);
            }else{
                Draw.color(color);
                Fill.poly(x, y, sides, radius);
            }

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
        /** Called in the small & large map. */
        public void drawMinimap(MinimapRenderer minimap){}
        /** Add any UI elements necessary. */
        public void added(){}
        /** Remove any UI elements, if necessary. */
        public void removed(){}
    }
}
