package mindustry.type;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.Saves.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.ui.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

/** A small section of a planet. */
public class Sector{
    private static final Seq<Sector> tmpSeq1 = new Seq<>();

    public final SectorRect rect;
    public final Plane plane;
    public final Planet planet;
    public final Ptile tile;
    public final int id;

    public @Nullable SaveSlot save;
    public @Nullable SectorPreset preset;
    public SectorInfo info = new SectorInfo();

    /** Number 0-1 indicating the difficulty based on nearby bases. */
    public float threat;
    public boolean generateEnemyBase;

    public Sector(Planet planet, Ptile tile){
        this.planet = planet;
        this.tile = tile;
        this.plane = new Plane();
        //empty sector tile needs a special rect
        if(tile.corners.length == 0){
            rect = new SectorRect(1f, Vec3.Zero.cpy(), Vec3.Y.cpy(), Vec3.X.cpy(), 0f);
        }else{
            this.rect = makeRect();
        }
        this.id = tile.id;
    }

    public Seq<Sector> near(){
        tmpSeq1.clear();
        for(Ptile tile : tile.tiles){
            tmpSeq1.add(planet.getSector(tile));
        }

        return tmpSeq1;
    }

    public void near(Cons<Sector> cons){
        for(Ptile tile : tile.tiles){
            cons.get(planet.getSector(tile));
        }
    }

    /** Displays threat as a formatted string. */
    public String displayThreat(){
        float step = 0.25f;
        String color = Tmp.c1.set(Color.white).lerp(Color.scarlet, Mathf.round(threat, step)).toString();
        String[] threats = {"low", "medium", "high", "extreme", "eradication"};
        int index = Math.min((int)(threat / step), threats.length - 1);
        return "[#" + color + "]" + Core.bundle.get("threat." + threats[index]);
    }

    /** @return whether this sector can be landed on at all.
     * Only sectors adjacent to non-wave sectors can be landed on. */
    public boolean unlocked(){
        return hasBase() || (preset != null && preset.alwaysUnlocked);
    }

    public void saveInfo(){
        Core.settings.putJson(planet.name + "-s-" + id + "-info", info);
    }

    public void loadInfo(){
        info = Core.settings.getJson(planet.name + "-s-" + id + "-info", SectorInfo.class, SectorInfo::new);

        //fix an old naming bug; this doesn't happen with new saves, but old saves need manual fixes
        if(info.resources.contains(Blocks.water)){
            info.resources.remove(Blocks.water);
            info.resources.add(Liquids.water);
        }

        if(info.resources.contains(u -> u == null)){
            info.resources = info.resources.select(u -> u != null);
        }
    }

    /** Removes any sector info. */
    public void clearInfo(){
        info = new SectorInfo();
        Core.settings.remove(planet.name + "-s-" + id + "-info");
    }

    public float getProductionScale(){
        return Math.max(1f - info.damage, 0);
    }

    public boolean isAttacked(){
        if(isBeingPlayed()) return state.rules.waves || state.rules.attackMode;
        return save != null && (info.waves || info.attack) && info.hasCore;
    }

    /** @return whether the player has a base here. */
    public boolean hasBase(){
        return save != null && info.hasCore && !(Vars.state.isGame() && Vars.state.rules.sector == this && state.gameOver);
    }

    /** @return whether the enemy has a generated base here. */
    public boolean hasEnemyBase(){
        return ((generateEnemyBase && preset == null) || (preset != null && preset.captureWave == 0)) && (save == null || info.attack);
    }

    public boolean isBeingPlayed(){
        //after the launch dialog, a sector is no longer considered being played
        return Vars.state.isGame() && Vars.state.rules.sector == this && !Vars.state.gameOver && !net.client();
    }

    public String name(){
        if(preset != null && info.name == null) return preset.localizedName;
        //single-sector "planets" use their own name for the sector name.
        if(info.name == null && planet.sectors.size == 1){
            return planet.localizedName;
        }
        return info.name == null ? id + "" : info.name;
    }

    public void setName(String name){
        info.name = name;
        saveInfo();
    }

    @Nullable
    public TextureRegion icon(){
        return info.contentIcon != null ? info.contentIcon.uiIcon : info.icon == null ? null : Fonts.getLargeIcon(info.icon);
    }

    @Nullable
    public String iconChar(){
        if(info.contentIcon != null) return info.contentIcon.emoji();
        if(info.icon != null) return (char)Iconc.codes.get(info.icon) + "";
        return null;
    }

    public boolean isCaptured(){
        if(isBeingPlayed()) return !info.waves && !info.attack;
        return save != null && !info.waves && !info.attack;
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
        return planet.generator == null ? 1 : planet.generator.getSectorSize(this);
    }

    public void removeItems(ItemSeq items){
        ItemSeq copy = items.copy();
        copy.each((i, a) -> copy.set(i, -a));
        addItems(copy);
    }

    public void removeItem(Item item, int amount){
        ItemSeq seq = new ItemSeq();
        seq.add(item, -amount);
        addItems(seq);
    }

    public void addItems(ItemSeq items){

        if(isBeingPlayed()){
            if(state.rules.defaultTeam.core() != null){
                ItemModule storage = state.rules.defaultTeam.items();
                int cap = state.rules.defaultTeam.core().storageCapacity;
                items.each((item, amount) -> storage.add(item, Math.min(cap - storage.get(item), amount)));
            }
        }else if(hasBase()){
            items.each((item, amount) -> info.items.add(item, Math.min(info.storageCapacity - info.items.get(item), amount)));
            info.items.checkNegative();
            saveInfo();
        }
    }

    /** @return items currently in this sector, taking into account playing state. */
    public ItemSeq items(){
        ItemSeq count = new ItemSeq();

        //for sectors being played on, add items directly
        if(isBeingPlayed()){
            if(state.rules.defaultTeam.core() != null) count.add(state.rules.defaultTeam.items());
        }else{
            //add items already present
            count.add(info.items);
        }

        return count;
    }

    public String toString(){
        return planet.name + "#" + id + " (" + name() + ")";
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
}
