package mindustry.type;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.ObjectIntMap.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.Saves.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** A small section of a planet. */
public class Sector{
    private static final Seq<Sector> tmpSeq1 = new Seq<>(), tmpSeq2 = new Seq<>(), tmpSeq3 = new Seq<>();
    private static final ObjectSet<Sector> tmpSet = new ObjectSet<>();

    public final SectorRect rect;
    public final Plane plane;
    public final Planet planet;
    public final Ptile tile;
    public final int id;

    public final SectorData data;

    public @Nullable SaveSlot save;
    public @Nullable SectorPreset preset;

    public float baseCoverage;

    //TODO implement a dynamic launch period
    public int launchPeriod = 10;

    public Sector(Planet planet, Ptile tile, SectorData data){
        this.planet = planet;
        this.tile = tile;
        this.plane = new Plane();
        this.rect = makeRect();
        this.id = tile.id;
        this.data = data;
    }

    public Seq<Sector> inRange(int range){
        //TODO cleanup/remove
        if(true){
            tmpSeq1.clear();
            neighbors(tmpSeq1::add);

            return tmpSeq1;
        }

        tmpSeq1.clear();
        tmpSeq2.clear();
        tmpSet.clear();

        tmpSeq1.add(this);
        tmpSet.add(this);
        for(int i = 0; i < range; i++){
            while(!tmpSeq1.isEmpty()){
                Sector sec = tmpSeq1.pop();
                tmpSet.add(sec);
                sec.neighbors(other -> {
                    if(tmpSet.add(other)){
                        tmpSeq2.add(other);
                    }
                });
            }
            tmpSeq1.clear();
            tmpSeq1.addAll(tmpSeq2);
        }

        tmpSeq3.clear().addAll(tmpSeq2);
        return tmpSeq3;
    }

    public void neighbors(Cons<Sector> cons){
        for(Ptile tile : tile.tiles){
            cons.get(planet.getSector(tile));
        }
    }

    /** @return whether this sector can be landed on at all.
     * Only sectors adjacent to non-wave sectors can be landed on.
     * TODO also preset sectors*/
    public boolean unlocked(){
        return hasBase() || Structs.contains(tile.tiles, p -> planet.getSector(p).isCaptured()) || (preset != null && preset.unlocked());
    }

    /** @return whether the player has a base here. */
    public boolean hasBase(){
        return save != null && !save.meta.tags.getBool("nocores");
    }

    /** @return whether the enemy has a generated base here. */
    public boolean hasEnemyBase(){
        return is(SectorAttribute.base) && (save == null || save.meta.rules.waves);
    }

    public boolean isBeingPlayed(){
        //after the launch dialog, a sector is no longer considered being played
        return Vars.state.isGame() && Vars.state.rules.sector == this && !Vars.state.launched && !Vars.state.gameOver;
    }

    public boolean isCaptured(){
        return save != null && !save.meta.rules.waves;
    }

    /** @return whether waves are present - if true, any bases here will be attacked. */
    public boolean hasWaves(){
        return save != null && save.meta.rules.waves;
    }

    public boolean hasSave(){
        return save != null;
    }

    public boolean locked(){
        return !unlocked();
    }

    /** @return light dot product in the range [0, 1]. */
    public float getLight(){
        Vec3 normal = Tmp.v31.set(tile.v).rotate(Vec3.Y, -planet.getRotation()).nor();
        Vec3 light = Tmp.v32.set(planet.solarSystem.position).sub(planet.position).nor();
        //lightness in [0, 1]
        return (normal.dot(light) + 1f) / 2f;
    }

    /** @return the sector size, in tiles */
    public int getSize(){
        int res = (int)(rect.radius * 3200);
        return res % 2 == 0 ? res : res + 1;
    }

    //TODO implement
    public boolean isLaunchWave(int wave){
        return metCondition() && wave % launchPeriod == 0;
    }

    public boolean metCondition(){
        //TODO implement
        return false;
    }

    //TODO this should be stored in a more efficient structure, and be updated each turn
    public ItemSeq getExtraItems(){
        return Core.settings.getJson(key("extra-items"), ItemSeq.class, ItemSeq::new);
    }

    public void setExtraItems(ItemSeq stacks){
        Core.settings.putJson(key("extra-items"),  stacks);
    }

    public void addItem(Item item, int amount){
        removeItem(item, -amount);
    }

    public void removeItem(Item item, int amount){
        if(isBeingPlayed()){
            if(state.rules.defaultTeam.core() != null){
                state.rules.defaultTeam.items().remove(item, amount);
            }
        }else{
            ItemSeq recv = getExtraItems();

            recv.remove(item, amount);

            setExtraItems(recv);
        }
    }

    public ItemSeq calculateItems(){
        ItemSeq count = new ItemSeq();

        //for sectors being played on, add items directly
        if(isBeingPlayed()){
            count.add(state.rules.defaultTeam.items());
        }else if(save != null){
            //add items already present
            for(Entry<Item> ent : save.meta.secinfo.coreItems){
                count.add(ent.key, ent.value);
            }

            count.add(calculateReceivedItems());

            int capacity = save.meta.secinfo.storageCapacity;

            //validation
            for(Item item : content.items()){
                //ensure positive items
                if(count.get(item) < 0) count.set(item, 0);
                //cap the items
                if(count.get(item) > capacity) count.set(item, capacity);
            }
        }

        return count;
    }

    public ItemSeq calculateReceivedItems(){
        ItemSeq count = new ItemSeq();

        if(save != null){
            long seconds = getSecondsPassed();

            //add produced items
            save.meta.secinfo.production.each((item, stat) -> count.add(item, (int)(stat.mean * seconds)));

            //add received items
            getExtraItems().each(count::add);
        }

        return count;
    }

    //TODO these methods should maybe move somewhere else and/or be contained in a data object
    public void setSpawnPosition(int position){
        put("spawn-position", position);
    }

    /** Only valid after this sector has been landed on once. */
    //TODO move to sector data?
    public int getSpawnPosition(){
        return Core.settings.getInt(key("spawn-position"), Point2.pack(world.width() / 2, world.height() / 2));
    }

    /** @return time spent in this sector this turn in ticks. */
    public float getTimeSpent(){
        //return currently counting time spent if being played on
        if(isBeingPlayed()) return state.secinfo.internalTimeSpent;

        //else return the stored value
        return getStoredTimeSpent();
    }

    public void setTimeSpent(float time){
        put("time-spent", time);

        //update counting time
        if(isBeingPlayed()){
            state.secinfo.internalTimeSpent = time;
        }
    }

    public String displayTimeRemaining(){
        float amount = Vars.turnDuration - getTimeSpent();
        int seconds = (int)(amount / 60);
        int sf = seconds % 60;
        return (seconds / 60) + ":" + (sf < 10 ? "0" : "") + sf;
    }

    /** @return the stored amount of time spent in this sector this turn in ticks.
     * Do not use unless you know what you're doing. */
    public float getStoredTimeSpent(){
        return Core.settings.getFloat(key("time-spent"));
    }

    public void setSecondsPassed(long number){
        put("seconds-passed", number);
    }

    /** @return how much time has passed in this sector without the player resuming here.
     * Used for resource production calculations. */
    public long getSecondsPassed(){
        return Core.settings.getLong(key("seconds-passed"));
    }

    private String key(String key){
        return planet.name + "-s-" + id + "-" + key;
    }

    private void put(String key, Object value){
        Core.settings.put(key(key), value);
    }

    /** Projects this sector onto a 4-corner square for use in map gen.
     * Allocates a new object. Do not call in the main loop. */
    private SectorRect makeRect(){
        Vec3[] corners = new Vec3[tile.corners.length];
        for(int i = 0; i < corners.length; i++){
            corners[i] = tile.corners[i].v.cpy().setLength(planet.radius);
        }

        Tmp.v33.setZero();
        for(Vec3 c : corners){
            Tmp.v33.add(c);
        }
        //v33 is now the center of this shape
        Vec3 center = Tmp.v33.scl(1f / corners.length).cpy();
        //radius of circle
        float radius = Tmp.v33.dst(corners[0]) * 0.98f;

        //get plane that these points are on
        plane.set(corners[0], corners[2], corners[4]);

        //relative vectors
        Vec3 planeTop = plane.project(center.cpy().add(0f, 1f, 0f)).sub(center).setLength(radius);
        Vec3 planeRight = plane.project(center.cpy().rotate(Vec3.Y, -4f)).sub(center).setLength(radius);

        //get angle from first corner to top vector
        Vec3 first = corners[1].cpy().sub(center); //first vector relative to center
        float angle = first.angle(planeTop);

        return new SectorRect(radius, center, planeTop, planeRight, angle);
    }

    public boolean is(SectorAttribute attribute){
        return (data.attributes & (1 << attribute.ordinal())) != 0;
    }

    public static class SectorRect{
        public final Vec3 center, top, right;
        public final Vec3 result = new Vec3();
        public final float radius, rotation;

        public SectorRect(float radius, Vec3 center, Vec3 top, Vec3 right, float rotation){
            this.center = center;
            this.top = top;
            this.right = right;
            this.radius = radius;
            this.rotation = rotation;
        }

        /** Project a coordinate into 3D space.
         * Both coordinates should be normalized to floats in the range [0, 1] */
        public Vec3 project(float x, float y){
            float nx = (x - 0.5f) * 2f, ny = (y - 0.5f) * 2f;
            return result.set(center).add(right, nx).add(top, ny);
        }
    }

    /** Cached data about a sector. */
    public static class SectorData{
        public UnlockableContent[] resources = {};
        public int spawnX, spawnY;

        public Block[] floors = {};
        public int[] floorCounts = {};
        public int attributes;

        public void write(Writes write){
            write.s(resources.length);
            for(Content resource : resources){
                write.b(resource.getContentType().ordinal());
                write.s(resource.id);
            }
            write.s(spawnX);
            write.s(spawnY);
            write.s(floors.length);
            for(int i = 0; i < floors.length; i++){
                write.s(floors[i].id);
                write.i(floorCounts[i]);
            }

            write.i(attributes);
        }

        public void read(Reads read){
            resources = new UnlockableContent[read.s()];
            for(int i = 0; i < resources.length; i++){
                resources[i] = Vars.content.getByID(ContentType.all[read.b()], read.s());
            }
            spawnX = read.s();
            spawnY = read.s();
            floors = new Block[read.s()];
            floorCounts = new int[floors.length];
            for(int i = 0; i < floors.length; i++){
                floors[i] = Vars.content.block(read.s());
                floorCounts[i] = read.i();
            }
            attributes = read.i();
        }
    }

    public enum SectorAttribute{
        /** Requires naval technology to land on, e.g. mostly water */
        naval,
        /** Has rain. */
        rainy,
        /** Has snow. */
        snowy,
        /** Has sandstorms. */
        desert,
        /** Has an enemy base. */
        base,
        /** Has spore weather. */
        spores
    }
}
