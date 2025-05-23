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
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
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
    public static final ObjectMap<String, Prov<? extends ObjectiveMarker>> markerNameToType = new ObjectMap<>();
    public static final Seq<String> allMarkerTypeNames = new Seq<>();

    /**
     * All objectives the executor contains. Do not modify directly, ever!
     * @see #eachRunning(Cons)
     */
    public Seq<MapObjective> all = new Seq<>(4);

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
            PointMarker::new,
            ShapeMarker::new,
            TextMarker::new,
            LineMarker::new,
            TextureMarker::new,
            QuadMarker::new
        );

        registerLegacyMarker("Minimap", PointMarker::new);
    }

    @SafeVarargs
    public static void registerObjective(Prov<? extends MapObjective>... providers){
        for(var prov : providers){
            allObjectiveTypes.add(prov);

            Class<? extends MapObjective> type = prov.get().getClass();
            String name = type.getSimpleName().replace("Objective", "");
            JsonIO.classTag(Strings.camelize(name), type);
            JsonIO.classTag(name, type);
        }
    }

    @SafeVarargs
    public static void registerMarker(Prov<? extends ObjectiveMarker>... providers){
        for(var prov : providers){
            allMarkerTypes.add(prov);

            Class<? extends ObjectiveMarker> type = prov.get().getClass();
            String name = type.getSimpleName().replace("Marker", "");
            allMarkerTypeNames.add(Strings.camelize(name));
            markerNameToType.put(name, prov);
            markerNameToType.put(Strings.camelize(name), prov);
            JsonIO.classTag(Strings.camelize(name), type);
            JsonIO.classTag(name, type);
        }
    }

    public static void registerLegacyMarker(String name, Prov<? extends ObjectiveMarker> prov) {
        Class<?> type = prov.get().getClass();

        markerNameToType.put(name, prov);
        markerNameToType.put(Strings.camelize(name), prov);
        JsonIO.classTag(Strings.camelize(name), type);
        JsonIO.classTag(name, type);
    }

    public MapObjectives(Seq<MapObjective> all){
        this.all.addAll(all);
    }

    public MapObjectives(){
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
            //objectives cannot get completed on the client, but they do try to update for timers and such
            if(obj.update() && !net.client()){
                Call.completeObjective(all.indexOf(obj));
            }
        });
    }

    public @Nullable MapObjective get(int index){
        return index < 0 || index >= all.size ? null : all.get(index);
    }

    /** @return Whether there are any qualified objectives at all. */
    public boolean any(){
        return all.count(MapObjective::qualified) > 0;
    }

    public void clear(){
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
        public boolean hidden;
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
        private transient boolean depFinished;

        /** @return True if this objective is done and should be removed from the executor. */
        public abstract boolean update();

        /** Reset internal state, if any. */
        public void reset(){}

        /** Called once after {@link #update()} returns true, before this objective is removed. */
        public void done(){
            state.rules.objectiveFlags.removeAll(flagsRemoved);
            state.rules.objectiveFlags.addAll(flagsAdded);
            completed = true;
        }

        /** @return True if all {@link #parents} are completed, rendering this objective able to execute. */
        public final boolean dependencyFinished(){
            if(depFinished) return true;

            for(var parent : parents){
                if(!parent.isCompleted()) return false;
            }

            return depFinished = true;
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

        /** Validate fields after reading to make sure none of them are null. */
        public void validate(){

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

        @Override
        public void validate(){
            if(content == null) content = Items.copper;
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

        @Override
        public void validate(){
            if(content == null) content = Items.copper;
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

        @Override
        public void validate(){
            if(item == null) item = Items.copper;
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

        @Override
        public void validate(){
            if(item == null) item = Items.copper;
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

        @Override
        public void validate(){
            if(block == null) block = Blocks.conveyor;
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

        @Override
        public void validate(){
            if(unit == null) unit = UnitTypes.dagger;
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
            return (countup += Time.delta) >= duration * state.rules.objectiveTimerMultiplier;
        }

        @Override
        public void reset(){
            countup = 0f;
        }

        @Nullable
        @Override
        public String text(){
            if(text != null){
                int i = (int)((duration * state.rules.objectiveTimerMultiplier - countup) / 60f);
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
                    if(state.mapLocales.containsProperty(text.substring(1))){
                        try{
                            return state.mapLocales.getFormatted(text.substring(1), timeString.toString());
                        }catch(IllegalArgumentException e){
                            //illegal text.
                            text = "";
                        }
                    }
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

        @Override
        public void validate(){
            if(block == null) block = Blocks.router;
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

        @Override
        public void validate(){
            if(block == null) block = Blocks.router;
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

        @Nullable
        @Override
        public String text(){
            if(text == null) return null;

            if(text.startsWith("@")){
                if(state.mapLocales.containsProperty(text.substring(1))) return state.mapLocales.getProperty(text.substring(1));
                return Core.bundle.get(text.substring(1));
            }else{
                return text;
            }
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

    /** Marker used for drawing various content to indicate something along with an objective. Mostly used as UI overlay.  */
    public static abstract class ObjectiveMarker{
        /** Internal use only! Do not access. */
        public transient int arrayIndex;

        /** Whether to display marker in the world. */
        public boolean world = true;
        /** Whether to display marker on minimap. */
        public boolean minimap = false;
        /** Whether to scale marker corresponding to player's zoom level. */
        public boolean autoscale = false;
        /** On which z-sorting layer is marker drawn. */
        protected float drawLayer = Layer.overlayUI;

        public void draw(float scaleFactor){}

        /** Control marker with world processor code. Ignores NaN (null) values. */
        public void control(LMarkerControl type, double p1, double p2, double p3){
            if(Double.isNaN(p1)) return;

            switch(type){
                case world -> world = !Mathf.equal((float)p1, 0f);
                case minimap -> minimap = !Mathf.equal((float)p1, 0f);
                case autoscale -> autoscale = !Mathf.equal((float)p1, 0f);
                case drawLayer -> drawLayer = (float)p1;
            }
        }

        public void setText(String text, boolean fetch){}

        public void setTexture(String textureName){}

        /** @return The localized type-name of this objective, defaulting to the class simple name without the "Marker" prefix. */
        public String typeName(){
            String className = getClass().getSimpleName().replace("Marker", "");
            return Core.bundle == null ? className : Core.bundle.get("marker." + className.toLowerCase() + ".name", className);
        }

        public static String fetchText(String text){
            if(text == null) return "";

            if(text.startsWith("@")){
                String key = text.substring(1);

                String out;
                if(mobile){
                    out = state.mapLocales.containsProperty(key + ".mobile") ?
                        state.mapLocales.getProperty(key + ".mobile") :
                        Core.bundle.get(key + ".mobile", Core.bundle.get(key));
                }else{
                    out = state.mapLocales.containsProperty(key) ?
                        state.mapLocales.getProperty(key) :
                        Core.bundle.get(key);
                }
                return UI.formatIcons(out);
            }else{
                return UI.formatIcons(text);
            }
        }
    }

    /** A marker that has a position in the world in world coordinates. */
    public static abstract class PosMarker extends ObjectiveMarker{
        /** Position of marker, in world coordinates */
        public @TilePos Vec2 pos = new Vec2();

        @Override
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                if(type == LMarkerControl.pos){
                    pos.x = (float)p1 * tilesize;
                }
            }

            if(!Double.isNaN(p2)){
                if(type == LMarkerControl.pos){
                    pos.y = (float)p2 * tilesize;
                }
            }
        }
    }

    /** Displays text above a shape. */
    public static class ShapeTextMarker extends PosMarker{
        public @Multiline String text = "frog";
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

            WorldLabel.drawAt(fetchedText, pos.x, pos.y + radius * scaleFactor + textHeight * scaleFactor, drawLayer, flags, fontSize * scaleFactor);
        }

        @Override
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                switch(type){
                    case fontSize -> fontSize = (float)p1;
                    case textHeight -> textHeight = (float)p1;
                    case labelFlags -> {
                        if(!Mathf.equal((float)p1, 0f)){
                            flags |= WorldLabel.flagBackground;
                        }else{
                            flags &= ~WorldLabel.flagBackground;
                        }
                    }
                    case radius -> radius = (float)p1;
                    case rotation -> rotation = (float)p1;
                    case color -> color.fromDouble(p1);
                    case shape -> sides = (int)p1;
                }
            }

            if(!Double.isNaN(p2)){
                switch(type){
                    case labelFlags -> {
                        if(!Mathf.equal((float)p2, 0f)){
                            flags |= WorldLabel.flagOutline;
                        }else{
                            flags &= ~WorldLabel.flagOutline;
                        }
                    }
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

    /** Displays a circle in the world. */
    public static class PointMarker extends PosMarker{
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

        public PointMarker(){}

        @Override
        public void draw(float scaleFactor){
            float rad = radius * tilesize * scaleFactor;
            float fin = Interp.pow2Out.apply((Time.globalTime / 100f) % 1f);

            Draw.z(drawLayer);
            Lines.stroke(Scl.scl((1f - fin) * stroke + 0.1f), color);
            Lines.circle(pos.x, pos.y, rad * fin);

            Draw.reset();
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

    /** Displays a shape with an outline and color. */
    public static class ShapeMarker extends PosMarker{
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

        public ShapeMarker(){}

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
                if (startAngle < endAngle){
                    Fill.arc(pos.x, pos.y, radius * scaleFactor, (endAngle - startAngle) / 360f, rotation + startAngle, sides);
                }else{
                    Fill.arc(pos.x, pos.y, radius * scaleFactor, (startAngle - endAngle) / 360f, rotation + endAngle, sides);
                }
            }

            Draw.reset();
        }

        @Override
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                switch(type){
                    case radius -> radius = (float)p1;
                    case stroke -> stroke = (float)p1;
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

    /** Displays text at a location. */
    public static class TextMarker extends PosMarker{
        public @Multiline String text = "uwu";
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
        public void draw(float scaleFactor){
            // font size cannot be 0
            if(Mathf.equal(fontSize, 0f)) return;

            if(fetchedText == null){
                fetchedText = fetchText(text);
            }

            WorldLabel.drawAt(fetchedText, pos.x, pos.y, drawLayer, flags, fontSize * scaleFactor);
        }

        @Override
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                switch(type){
                    case fontSize -> fontSize = (float)p1;
                    case labelFlags -> {
                        if(!Mathf.equal((float)p1, 0f)){
                            flags |= WorldLabel.flagBackground;
                        }else{
                            flags &= ~WorldLabel.flagBackground;
                        }
                    }
                }
            }

            if(!Double.isNaN(p2)){
                switch(type){
                    case labelFlags -> {
                        if(!Mathf.equal((float)p2, 0f)){
                            flags |= WorldLabel.flagOutline;
                        }else{
                            flags &= ~WorldLabel.flagOutline;
                        }
                    }
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

    /** Displays a line from pos1 to pos2. */
    public static class LineMarker extends PosMarker{
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

        public LineMarker(){}

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
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                switch(type){
                    case endPos -> endPos.x = (float)p1 * tilesize;
                    case stroke -> stroke = (float)p1;
                    case color -> color1.set(color2.fromDouble(p1));
                }
            }

            if(!Double.isNaN(p2)){
                switch(type){
                    case endPos -> endPos.y = (float)p2 * tilesize;
                }
            }

            if(!Double.isNaN(p1) && !Double.isNaN(p2)){
                switch (type){
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

    /** Displays a texture with specified name. */
    public static class TextureMarker extends PosMarker{
        public float rotation = 0f, width = 0f, height = 0f; // Zero width/height scales marker to original texture's size
        public String textureName = "";
        public Color color = Color.white.cpy();

        private transient TextureRegion fetchedRegion;

        public TextureMarker(String textureName, float x, float y, float width, float height){
            this.textureName = textureName;
            this.pos.set(x, y);
            this.width = width;
            this.height = height;
        }

        public TextureMarker(String textureName, float x, float y){
            this.textureName = textureName;
            this.pos.set(x, y);
        }

        public TextureMarker(){}

        @Override
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                switch(type){
                    case rotation -> rotation = (float)p1;
                    case textureSize -> width = (float)p1 * tilesize;
                    case color -> color.fromDouble(p1);
                }
            }

            if(!Double.isNaN(p2)){
                switch(type){
                    case textureSize -> height = (float)p2 * tilesize;
                }
            }
        }

        @Override
        public void draw(float scaleFactor){
            if(textureName.isEmpty()) return;

            if(fetchedRegion == null) setTexture(textureName);

            // Zero width/height scales marker to original texture's size
            if(Mathf.equal(width, 0f)) width = fetchedRegion.width * fetchedRegion.scl() * Draw.xscl;
            if(Mathf.equal(height, 0f)) height = fetchedRegion.height * fetchedRegion.scl() * Draw.yscl;

            Draw.z(drawLayer);
            Draw.color(color);
            Draw.rect(fetchedRegion, pos.x, pos.y, width * scaleFactor, height * scaleFactor, rotation);
        }

        @Override
        public void setTexture(String textureName){
            this.textureName = textureName;

            if(headless) return;
            if(fetchedRegion == null) fetchedRegion = new TextureRegion();
            lookupRegion(textureName, fetchedRegion);
        }

    }

    public static class QuadMarker extends ObjectiveMarker{
        public String textureName = "white";
        public @Vertices float[] vertices = new float[24];
        private boolean mapRegion = true;

        private transient TextureRegion fetchedRegion;

        public QuadMarker() {
            for(int i = 0; i < 4; i++){
                vertices[i * 6 + 2] = Color.white.toFloatBits();
                vertices[i * 6 + 5] = Color.clearFloatBits;
            }
        }

        @Override
        public void draw(float scaleFactor){
            if(fetchedRegion == null) setTexture(textureName);

            Draw.z(drawLayer);
            Draw.vert(fetchedRegion.texture, vertices, 0, vertices.length);
        }

        @Override
        public void control(LMarkerControl type, double p1, double p2, double p3){
            super.control(type, p1, p2, p3);

            if(!Double.isNaN(p1)){
                switch(type){
                    case color -> {
                        float col = Tmp.c1.fromDouble(p1).toFloatBits();
                        for(int i = 0; i < 4; i++) vertices[i * 6 + 2] = col;
                    }
                    case pos -> vertices[0] = (float)p1 * tilesize;
                    case posi -> setPos((int)p1, p2, p3);
                    case uvi -> setUv((int)p1, p2, p3);
                }
            }

            if(!Double.isNaN(p2)){
                switch(type){
                    case pos -> vertices[1] = (float)p1 * tilesize;
                }
            }

            if(!Double.isNaN(p1) && !Double.isNaN(p2)){
                switch(type){
                    case colori -> setColor((int)p1, p2);
                }
            }
        }

        @Override
        public void setTexture(String textureName){
            this.textureName = textureName;
            if(headless) return;

            boolean firstUpdate = fetchedRegion == null;

            if(fetchedRegion == null) fetchedRegion = new TextureRegion();
            Tmp.tr1.set(fetchedRegion);

            lookupRegion(textureName, fetchedRegion);

            if(firstUpdate){
                if(mapRegion){
                    mapRegion = false;

                    // possibly from the editor, we need to clamp the values
                    for(int i = 0; i < 4; i++){
                        vertices[i * 6 + 3] = Mathf.map(Mathf.clamp(vertices[i * 6 + 3]), fetchedRegion.u, fetchedRegion.u2);
                        vertices[i * 6 + 4] = Mathf.map(1 - Mathf.clamp(vertices[i * 6 + 4]), fetchedRegion.v, fetchedRegion.v2);
                    }
                }
            }else{
                for(int i = 0; i < 4; i++){
                    vertices[i * 6 + 3] = Mathf.map(vertices[i * 6 + 3], Tmp.tr1.u, Tmp.tr1.u2, fetchedRegion.u, fetchedRegion.u2);
                    vertices[i * 6 + 4] = Mathf.map(vertices[i * 6 + 4], Tmp.tr1.v, Tmp.tr1.v2, fetchedRegion.v, fetchedRegion.v2);
                }
            }
        }

        private void setPos(int i, double x, double y){
            if(i >= 0 && i < 4){
                if(!Double.isNaN(x)) vertices[i * 6] = (float)x * tilesize;
                if(!Double.isNaN(y)) vertices[i * 6 + 1] = (float)y * tilesize;
            }
        }

        private void setColor(int i, double c){
            if(i >= 0 && i < 4){
                vertices[i * 6 + 2] = Tmp.c1.fromDouble(c).toFloatBits();
            }
        }

        private void setUv(int i, double u, double v){
            if(i >= 0 && i < 4){
                if(fetchedRegion == null) setTexture(textureName);

                if(!Double.isNaN(u)) vertices[i * 6 + 3] = Mathf.map(Mathf.clamp((float)u), fetchedRegion.u, fetchedRegion.u2);
                if(!Double.isNaN(v)) vertices[i * 6 + 4] = Mathf.map(1 - Mathf.clamp((float)v), fetchedRegion.v, fetchedRegion.v2);
            }
        }

    }

    private static void lookupRegion(String name, TextureRegion out){
        TextureRegion region = Core.atlas.find(name);
        if(region.found()){
            out.set(region);
        }else{
            if(Core.assets.isLoaded(name, Texture.class)){
                out.set(Core.assets.get(name, Texture.class));
            }else{
                out.set(Core.atlas.find("error"));
            }
        }
    }

    /** For arrays or {@link Seq}s; does not create element rearrangement buttons. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Unordered{}

    /** For arrays or {@link Seq}s; does not add the new and delete buttons */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Immutable{}

    /** For {@code float[]}; treats it as an array of vertices. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Vertices{}

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
