package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class OverdriveProjector extends Block{
    private static Color color = Color.valueOf("feb380");
    private static Color phase = Color.valueOf("ffd59e");
    private static IntSet healed = new IntSet();

    protected int timerUse = timers++;

    protected TextureRegion topRegion;
    protected float reload = 60f;
    protected float range = 80f;
    protected float speedBoost = 1.5f;
    protected float speedBoostPhase = 0.75f;
    protected float useTime = 400f;
    protected float phaseRangeBoost = 20f;

    public OverdriveProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
        canOverdrive = false;
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
    public void update(Tile tile){
        OverdriveEntity entity = tile.entity();
        entity.heat = Mathf.lerpDelta(entity.heat, entity.cons.valid() ? 1f : 0f, 0.08f);
        entity.charge += entity.heat * Time.delta();

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, Mathf.num(entity.cons.optionalValid()), 0.1f);

        if(entity.timer.get(timerUse, useTime) && entity.power.satisfaction > 0){
            entity.cons.trigger();
        }

        if(entity.charge >= reload){
            float realRange = range + entity.phaseHeat * phaseRangeBoost;
            float realBoost = (speedBoost + entity.phaseHeat * speedBoostPhase) * entity.power.satisfaction;

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
        OverdriveEntity entity = tile.entity();
        float realRange = range + entity.phaseHeat * phaseRangeBoost;

        Drawf.dashCircle(tile.drawx(), tile.drawy(), realRange, color);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        OverdriveEntity entity = tile.entity();
        float f = 1f - (Time.time() / 100f) % 1f;

        Draw.color(color, phase, entity.phaseHeat);
        Draw.alpha(entity.heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.alpha(1f);
        Lines.stroke((2f * f + 0.2f) * entity.heat);
        Lines.square(tile.drawx(), tile.drawy(), (1f - f) * 8f);

        Draw.reset();
    }

    @Override
    public TileEntity newEntity(){
        return new OverdriveEntity();
    }

    class OverdriveEntity extends TileEntity{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;

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
    }
}
