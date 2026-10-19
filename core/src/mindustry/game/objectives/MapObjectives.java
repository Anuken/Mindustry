package mindustry.game.objectives;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.markers.*;
import mindustry.gen.*;
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
            QuadMarker::new,
            LightMarker::new
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

    public static void registerLegacyMarker(String name, Prov<? extends ObjectiveMarker> prov){
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

    /** For {@code int}; treats it as a boolean with -1 for false and any other value for true (defaulting to 1) */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface IndexBool{}

    /** For {@code byte}; treats it as a world label flag. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface LabelFlag{}

    /** For {@code int}; treats it as an alignment from {@link Align} */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Alignment{
        boolean hor() default true;
        boolean ver() default true;
    }

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

    /** For {@link String}; indicates that text corresponds to logic code. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface LogicCode{}

    /** For {@code float}; multiplies the UI input by 60. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Second{}

    /** For {@code float} or similar data structures, such as {@link Vec2}; multiplies the UI input by {@link Vars#tilesize}. */
    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface TilePos{}

}
