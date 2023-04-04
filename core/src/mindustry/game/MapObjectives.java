package mindustry.game;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;

import java.lang.annotation.*;
import java.util.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static mindustry.Vars.*;

/** Handles and executes in-map objectives. */
public class MapObjectives implements Iterable<MapObjective>, Eachable<MapObjective>{
    public static final Seq<Prov<? extends MapObjective>> allObjectiveTypes = new Seq<>();
    public static final Seq<Prov<? extends ObjectiveMarker>> allMarkerTypes = new Seq<>();

    /**
     * All objectives the executor contains. Do not modify directly, ever!
     * @see #eachRunning(Cons)
     */
    public Seq<MapObjective> all = new Seq<>(4);
    /** @see #checkChanged() */
    protected transient boolean changed;

    static{
        registerObjective(
            ResearchObjective::new,
            ProduceObjective::new,
            ItemObjective::new,
            CoreItemObjective::new,
            BuildCountObjective::new,
            UnitCountObjective::new,
            DestroyUnitsObjective::new,
            TimerObjective::new,
            DestroyBlockObjective::new,
            DestroyBlocksObjective::new,
            DestroyCoreObjective::new,
            CommandModeObjective::new,
            FlagObjective::new
        );

        registerMarker(
            ShapeTextMarker::new,
            MinimapMarker::new,
            ShapeMarker::new,
            TextMarker::new
        );
    }

    @SafeVarargs
    public static void registerObjective(Prov<? extends MapObjective>... providers){
        for(var prov : providers){
            allObjectiveTypes.add(prov);

            Class<? extends MapObjective> type = prov.get().getClass();
            JsonIO.classTag(Strings.camelize(type.getSimpleName().replace("Objective", "")), type);
            JsonIO.classTag(type.getSimpleName().replace("Objective", ""), type);
        }
    }

    @SafeVarargs
    public static void registerMarker(Prov<? extends ObjectiveMarker>... providers){
        for(var prov : providers){
            allMarkerTypes.add(prov);

            Class<? extends ObjectiveMarker> type = prov.get().getClass();
            JsonIO.classTag(Strings.camelize(type.getSimpleName().replace("Marker", "")), type);
            JsonIO.classTag(type.getSimpleName().replace("Marker", ""), type);
        }
    }

    /** Adds all given objectives to the executor as root objectives. */
    public void add(MapObjective... objectives){
        for(var objective : objectives) flatten(objective);
    }

    /** Recursively adds the objective and its children. */
    private void flatten(MapObjective objective){
        for(var child : objective.children) flatten(child);

        objective.children.clear();
        all.add(objective);
    }

    /** Updates all objectives this executor contains. */
    public void update(){
        eachRunning(obj -> {
            for(var marker : obj.markers){
                if(!marker.wasAdded){
                    marker.wasAdded = true;
                    marker.added();
                }
            }

            //objectives cannot get completed on the client, but they do try to update for timers and such
            if(obj.update() && !net.client()){
                obj.completed = true;
                obj.done();
                for(var marker : obj.markers){
                    if(marker.wasAdded){
                        marker.removed();
                        marker.wasAdded = false;
                    }
                }
            }

            changed |= obj.changed;
            obj.changed = false;
        });
    }

    /** @return True if map rules should be synced. Reserved for {@link Vars#logic}; do not invoke directly! */
    public boolean checkChanged(){
        boolean has = changed;
        changed = false;

        return has;
    }

    /** @return Whether there are any qualified objectives at all. */
    public boolean any(){
        return all.count(MapObjective::qualified) > 0;
    }

    public void clear(){
        if(all.size > 0) changed = true;
        all.clear();
    }

    /** Iterates over all qualified in-map objectives. */
    public void eachRunning(Cons<MapObjective> cons){
        all.each(MapObjective::qualified, cons);
    }

    /** Iterates over all qualified in-map objectives, with a filter. */
    public <T extends MapObjective> void eachRunning(Boolf<? super MapObjective> pred, Cons<T> cons){
        all.each(obj -> obj.qualified() && pred.get(obj), cons);
    }

    @Override
    public Iterator<MapObjective> iterator(){
        return all.iterator();
    }

    @Override
    public void each(Cons<? super MapObjective> cons){
        all.each(cons);
    }

    /** Base abstract class for any in-map objective. */
    public static abstract class MapObjective{
        public @Nullable @Multiline String details;
        public @Unordered String[] flagsAdded = {};
        public @Unordered String[] flagsRemoved = {};
        public ObjectiveMarker[] markers = {};

        /** The parents of this objective. All parents must be done in order for this to be updated. */
        public transient Seq<MapObjective> parents = new Seq<>(2);
        /** Temporary container to store references since this class is static. Will immediately be flattened. */
        private transient final Seq<MapObjective> children = new Seq<>(2);

        /** For the objectives UI dialog. Do not modify directly! */
        public transient int editorX = -1, editorY = -1;

        /** Whether this objective has been done yet. This is internally set. */
        private boolean completed;
        /** Internal value. Do not modify! */
        private transient boolean depFinished, changed;

        /** @return True if this objective is done and should be removed from the executor. */
        public abstract boolean update();

        /** Reset internal state, if any. */
        public void reset(){}

        /** Called once after {@link #update()} returns true, before this objective is removed. */
        public void done(){
            changed();
            state.rules.objectiveFlags.removeAll(flagsRemoved);
            state.rules.objectiveFlags.addAll(flagsAdded);
        }

        /** Notifies the executor that map rules should be synced. */
        protected void changed(){
            changed = true;
        }

        /** @return True if all {@link #parents} are completed, rendering this objective able to execute. */
        public final boolean dependencyFinished(){
            if(depFinished) return true;

            boolean f = true;
            for(var parent : parents){
                if(!parent.isCompleted()) return false;
            }

            return f && (depFinished = true);
        }

        /** @return True if this objective is done (practically, has been removed from the executor). */
        public final boolean isCompleted(){
            return completed;
        }

        /** @return Whether this objective should run at all. */
        public boolean qualified(){
            return !completed && dependencyFinished();
        }

        /** @return This objective, with the given child's parents added with this, for chaining operations. */
        public MapObjective child(MapObjective child){
            child.parents.add(this);
            children.add(child);
            return this;
        }

        /** @return This objective, with the given parent added to this objective's parents, for chaining operations. */
        public MapObjective parent(MapObjective parent){
            parents.add(parent);
            return this;
        }

        /** @return This objective, with the details message assigned to, for chaining operations. */
        public MapObjective details(String details){
            this.details = details;
            return this;
        }

        /** @return This objective, with the added-flags assigned to, for chaining operations. */
        public MapObjective flagsAdded(String... flagsAdded){
            this.flagsAdded = flagsAdded;
            return this;
        }

        /** @return This objective, with the removed-flags assigned to, for chaining operations. */
        public MapObjective flagsRemoved(String... flagsRemoved){
            this.flagsRemoved = flagsRemoved;
            return this;
        }

        /** @return This objective, with the markers assigned to, for chaining operations. */
        public MapObjective markers(ObjectiveMarker... markers){
            this.markers = markers;
            return this;
        }

        /** @return Basic mission display text. If null, falls back to standard text. */
        public @Nullable String text(){
            return null;
        }

        /** @return Details that appear upon click. */
        public @Nullable String details(){
            return details;
        }

        /** @return The localized type-name of this objective, defaulting to the class simple name without the "Objective" prefix. */
        public String typeName(){
            String className = getClass().getSimpleName().replace("Objective", "");
            return Core.bundle == null ? className : Core.bundle.get("objective." + className.toLowerCase() + ".name", className);
        }
    }

    /** Research a specific piece of content in the tech tree. */
    public static class ResearchObjective extends MapObjective{
        public @Researchable UnlockableContent content = Items.copper;

        public ResearchObjective(UnlockableContent content){
            this.content = content;
        }

        public ResearchObjective(){}

        @Override
        public boolean update(){
            return content.unlocked();
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.research", content.emoji(), content.localizedName);
        }
    }

    /** Produce a specific piece of content in the tech tree (essentially research with different text). */
    public static class ProduceObjective extends MapObjective{
        public @Researchable UnlockableContent content = Items.copper;

        public ProduceObjective(UnlockableContent content){
            this.content = content;
        }

        public ProduceObjective(){}

        @Override
        public boolean update(){
            return content.unlocked();
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.produce", content.emoji(), content.localizedName);
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

        public ItemObjective(){}

        @Override
        public boolean update(){
            return state.rules.defaultTeam.items().has(item, amount);
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.item", state.rules.defaultTeam.items().get(item), amount, item.emoji(), item.localizedName);
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

        public CoreItemObjective(){}

        @Override
        public boolean update(){
            return state.stats.coreItemCount.get(item) >= amount;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.coreitem", state.stats.coreItemCount.get(item), amount, item.emoji(), item.localizedName);
        }
    }

    /** Build a certain amount of a block. */
    public static class BuildCountObjective extends MapObjective{
        public @Synthetic Block block = Blocks.conveyor;
        public int count = 1;

        public BuildCountObjective(Block block, int count){
            this.block = block;
            this.count = count;
        }

        public BuildCountObjective(){}

        @Override
        public boolean update(){
            return state.stats.placedBlockCount.get(block, 0) >= count;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.build", count - state.stats.placedBlockCount.get(block, 0), block.emoji(), block.localizedName);
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

        public UnitCountObjective(){}

        @Override
        public boolean update(){
            return state.rules.defaultTeam.data().countType(unit) >= count;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.buildunit", count - state.rules.defaultTeam.data().countType(unit), unit.emoji(), unit.localizedName);
        }
    }

    /** Produce a certain amount of units. */
    public static class DestroyUnitsObjective extends MapObjective{
        public int count = 1;

        public DestroyUnitsObjective(int count){
            this.count = count;
        }

        public DestroyUnitsObjective(){}

        @Override
        public boolean update(){
            return state.stats.enemyUnitsDestroyed >= count;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.destroyunits", count - state.stats.enemyUnitsDestroyed);
        }
    }

    public static class TimerObjective extends MapObjective{
        public @Multiline String text;
        public @Second float duration = 60f * 30f;

        protected float countup;

        public TimerObjective(String text, float duration){
            this.text = text;
            this.duration = duration;
        }

        public TimerObjective(){
        }

        @Override
        public boolean update(){
            return (countup += Time.delta) >= duration;
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
                    try{
                        return Core.bundle.formatString(text, timeString.toString());
                    }catch(IllegalArgumentException e){
                        //illegal text.
                        text = "";
                    }

                }
            }

            return null;
        }
    }

    public static class DestroyBlockObjective extends MapObjective{
        public Point2 pos = new Point2();
        public Team team = Team.crux;
        public @Synthetic Block block = Blocks.router;

        public DestroyBlockObjective(Block block, int x, int y, Team team){
            this.block = block;
            this.team = team;
            this.pos.set(x, y);
        }

        public DestroyBlockObjective(){}

        @Override
        public boolean update(){
            var build = world.build(pos.x, pos.y);
            return build == null || build.team != team || build.block != block;
        }

        @Override
        public String text(){
            return Core.bundle.format("objective.destroyblock", block.emoji(), block.localizedName);
        }
    }

    public static class DestroyBlocksObjective extends MapObjective{
        public @Unordered Point2[] positions = {};
        public Team team = Team.crux;
        public @Synthetic Block block = Blocks.router;

        public DestroyBlocksObjective(Block block, Team team, Point2... positions){
            this.block = block;
            this.team = team;
            this.positions = positions;
        }

        public DestroyBlocksObjective(){}

        public int progress(){
            int count = 0;
            for(var pos : positions){
                var build = world.build(pos.x, pos.y);
                if(build == null || build.team != team || build.block != block){
                    count ++;
                }
            }
            return count;
        }

        @Override
        public boolean update(){
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
        public boolean update(){
            return headless || control.input.selectedUnits.contains(u -> u.isCommandable() && u.command().hasCommand());
        }

        @Override
        public String text(){
            return Core.bundle.get("objective.command");
        }
    }

    /** Wait until a logic flag is set. */
    public static class FlagObjective extends MapObjective{
        public String flag = "flag";
        public @Multiline String text;

        public FlagObjective(String flag, String text){
            this.flag = flag;
            this.text = text;
        }

        public FlagObjective(){}

        @Override
        public boolean update(){
            return state.rules.objectiveFlags.contains(flag);
        }

        @Override
        public String text(){
            return text != null && text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text;
        }
    }

    /** Destroy all enemy core(s). */
    public static class DestroyCoreObjective extends MapObjective{
        @Override
        public boolean update(){
            return state.rules.waveTeam.cores().size == 0;
        }

        @Override
        public String text(){
            return Core.bundle.get("objective.destroycore");
        }
    }

    /** Marker used for drawing UI to indicate something along with an objective. */
    public static abstract class ObjectiveMarker{
        /** Makes sure markers are only added once. */
        public transient boolean wasAdded;

        /** Called in the overlay draw layer.*/
        public void draw(){}
        /** Called in the small and large map. */
        public void drawMinimap(MinimapRenderer minimap){}
        /** Add any UI elements necessary. */
        public void added(){}
        /** Remove any UI elements, if necessary. */
        public void removed(){}

        /** @return The localized type-name of this objective, defaulting to the class simple name without the "Marker" prefix. */
        public String typeName(){
            String className = getClass().getSimpleName().replace("Marker", "");
            return Core.bundle == null ? className : Core.bundle.get("marker." + className.toLowerCase() + ".name", className);
        }

        public static String fetchText(String text){
            return text.startsWith("@") ?
                //on mobile, try ${text}.mobile first for mobile-specific hints.
                mobile ? Core.bundle.get(text.substring(1) + ".mobile", Core.bundle.get(text.substring(1))) :
                Core.bundle.get(text.substring(1)) :
                text;

        }
    }

    /** Displays text above a shape. */
    public static class ShapeTextMarker extends ObjectiveMarker{
        public @Multiline String text = "frog";
        public @TilePos Vec2 pos = new Vec2();
        public float fontSize = 1f, textHeight = 7f;
        public @LabelFlag byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;

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

        public ShapeTextMarker(){}

        @Override
        public void draw(){
            Lines.stroke(3f, Pal.gray);
            Lines.poly(pos.x, pos.y, sides, radius + 1f, rotation);
            Lines.stroke(1f, color);
            Lines.poly(pos.x, pos.y, sides, radius + 1f, rotation);
            Draw.reset();

            if(fetchedText == null){
                fetchedText = fetchText(text);
            }

            WorldLabel.drawAt(fetchedText, pos.x, pos.y + radius + textHeight, Draw.z(), flags, fontSize);
        }
    }

    /** Displays a circle on the minimap. */
    public static class MinimapMarker extends ObjectiveMarker{
        public Point2 pos = new Point2();
        public float radius = 5f, stroke = 11f;
        public Color color = Color.valueOf("f25555");

        public MinimapMarker(int x, int y){
            this.pos.set(x, y);
        }

        public MinimapMarker(int x, int y, Color color){
            this.pos.set(x, y);
            this.color = color;
        }

        public MinimapMarker(int x, int y, float radius, float stroke, Color color){
            this.pos.set(x, y);
            this.stroke = stroke;
            this.radius = radius;
            this.color = color;
        }

        public MinimapMarker(){}

        @Override
        public void drawMinimap(MinimapRenderer minimap){
            minimap.transform(Tmp.v1.set(pos.x * tilesize, pos.y * tilesize));

            float rad = minimap.scale(radius * tilesize);
            float fin = Interp.pow2Out.apply((Time.globalTime / 100f) % 1f);

            Lines.stroke(Scl.scl((1f - fin) * stroke + 0.1f), color);
            Lines.circle(Tmp.v1.x, Tmp.v1.y, rad * fin);
            Draw.reset();
        }
    }

    /** Displays a shape with an outline and color. */
    public static class ShapeMarker extends ObjectiveMarker{
        public @TilePos Vec2 pos = new Vec2();
        public float radius = 8f, rotation = 0f, stroke = 1f;
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

        public ShapeMarker(){}

        @Override
        public void draw(){
            //in case some idiot decides to make 9999999 sides and freeze the game
            int sides = Math.min(this.sides, 200);

            if(!fill){
                if(outline){
                    Lines.stroke(stroke + 2f, Pal.gray);
                    Lines.poly(pos.x, pos.y, sides, radius + 1f, rotation);
                }

                Lines.stroke(stroke, color);
                Lines.poly(pos.x, pos.y, sides, radius + 1f, rotation);
            }else{
                Draw.color(color);
                Fill.poly(pos.x, pos.y, sides, radius, rotation);
            }

            Draw.reset();
        }
    }

    /** Displays text at a location. */
    public static class TextMarker extends ObjectiveMarker{
        public @Multiline String text = "uwu";
        public @TilePos Vec2 pos = new Vec2();
        public float fontSize = 1f;
        public @LabelFlag byte flags = WorldLabel.flagBackground | WorldLabel.flagOutline;
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

        public TextMarker(){}

        @Override
        public void draw(){
            if(fetchedText == null){
                fetchedText = fetchText(text);
            }

            WorldLabel.drawAt(fetchedText, pos.x, pos.y, Draw.z(), flags, fontSize);
        }
    }

    /** For arrays or {@link Seq}s; does not create element rearrangement buttons. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Unordered{}

    /** For {@code byte}; treats it as a world label flag. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface LabelFlag{}

    /** For {@link UnlockableContent}; filters all un-researchable content. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Researchable{}

    /** For {@link Block}; filters all un-buildable blocks. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Synthetic{}

    /** For {@link String}; indicates that a text area should be used. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Multiline{}

    /** For {@code float}; multiplies the UI input by 60. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Second{}

    /** For {@code float} or similar data structures, such as {@link Vec2}; multiplies the UI input by {@link Vars#tilesize}. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface TilePos{}
}
