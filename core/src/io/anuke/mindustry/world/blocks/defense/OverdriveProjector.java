package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.ProjectorTrait;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class OverdriveProjector extends Block{
    private static final IntSet healed = new IntSet();

    public final int timerUse = timers++;

    public TextureRegion topRegion;
    public float reload = 60f;
    public float range = 80f;
    public float speedBoost = 1.5f;
    public float speedBoostPhase = 0.75f;
    public float useTime = 400f;
    public float phaseRangeBoost = 20f;
    public Color baseColor = Color.valueOf("feb380");
    public Color phaseColor = Color.valueOf("ffd59e");

    public OverdriveProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
        canOverdrive = false;
        entityType = OverdriveEntity::new;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range, Pal.accent);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.speedIncrease, (int)(100f * speedBoost), StatUnit.percent);
        stats.add(BlockStat.range, range / tilesize, StatUnit.blocks);

        stats.add(BlockStat.boostEffect, phaseRangeBoost / tilesize, StatUnit.blocks);
        stats.add(BlockStat.boostEffect, (int)((speedBoost + speedBoostPhase) * 100f), StatUnit.percent);
    }

    @Override
    public void drawLight(Tile tile){
        renderer.lights.add(tile.drawx(), tile.drawy(), 50f * tile.entity.efficiency(), baseColor, 0.7f * tile.entity.efficiency());
    }

    @Override
    public void update(Tile tile){
        OverdriveEntity entity = tile.ent();
        entity.heat = Mathf.lerpDelta(entity.heat, entity.cons.valid() ? 1f : 0f, 0.08f);
        entity.charge += entity.heat * Time.delta();
        entity.rangeProg = Mathf.lerpDelta(entity.rangeProg, entity.power.status > 0 ? 1f : 0f, 0.05f);

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, Mathf.num(entity.cons.optionalValid()), 0.1f);

        if(entity.timer.get(timerUse, useTime) && entity.efficiency() > 0){
            entity.cons.trigger();
        }

        if(entity.charge >= reload){
            float realRange = range + entity.phaseHeat * phaseRangeBoost;
            float realBoost = (speedBoost + entity.phaseHeat * speedBoostPhase) * entity.efficiency();

            entity.charge = 0f;

            int tileRange = (int)(realRange / tilesize + 1);
            healed.clear();

            for(int x = -tileRange + tile.x; x <= tileRange + tile.x; x++){
                for(int y = -tileRange + tile.y; y <= tileRange + tile.y; y++){
                    if(!Mathf.within(x * tilesize, y * tilesize, tile.drawx(), tile.drawy(), realRange)) continue;

                    Tile other = world.ltile(x, y);

                    if(other == null) continue;

                    if(other.getTeamID() == tile.getTeamID() && !healed.contains(other.pos()) && other.entity != null){
                        if(other.entity.timeScale <= realBoost){
                            other.entity.timeScaleDuration = Math.max(other.entity.timeScaleDuration, reload + 1f);
                            other.entity.timeScale = Math.max(other.entity.timeScale, realBoost);
                        }
                        healed.add(other.pos());
                    }
                }
            }
        }
    }

    @Override
    public void drawSelect(Tile tile){
        OverdriveEntity entity = tile.ent();
        float realRange = range + entity.phaseHeat * phaseRangeBoost;

        Drawf.dashCircle(tile.drawx(), tile.drawy(), realRange, baseColor);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        OverdriveEntity entity = tile.ent();
        float f = 1f - (Time.time() / 100f) % 1f;

        Draw.color(baseColor, phaseColor, entity.phaseHeat);
        Draw.alpha(entity.heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.alpha(1f);
        Lines.stroke((2f * f + 0.2f) * entity.heat);
        Lines.square(tile.drawx(), tile.drawy(), (1f - f) * 8f);

        Draw.reset();
    }

    class OverdriveEntity extends TileEntity implements ProjectorTrait{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;
        float rangeProg;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(heat);
            stream.writeFloat(phaseHeat);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            heat = stream.readFloat();
            phaseHeat = stream.readFloat();
        }

        float realRadius(){
            return (range + phaseHeat * phaseRangeBoost) * rangeProg;
        }

        @Override
        public void draw(){
            if(Core.settings.getBool("overdriveprojection")){
                Draw.color(color);
                Fill.circle(x, y, realRadius());
                Draw.color();
            }
        }

        public void drawOver(){
            if(Core.settings.getBool("overdriveprojection")){
                Draw.color(Color.white);
                Draw.alpha(1f - power.status);
                Fill.circle(x, y, realRadius());
                Draw.color();
            }
        }

        public void drawSimple(){
            if(Core.settings.getBool("overdriveprojection")){
                float rad = realRadius();

                Draw.color(color);
                Lines.stroke(1.5f);
                Draw.alpha(0.17f);
                Fill.circle(x, y, rad);
                Draw.alpha(1f);
                Lines.circle(x, y, rad);
                Draw.reset();
            }
        }

        @Override
        public Color accent(){
            return color;
        }

        @Override
        public String projectorSet(){
            return "OverdriveSet";
        }

        @Override
        public EntityGroup targetGroup(){
            return projectorGroup;
        }
    }
}