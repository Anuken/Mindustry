package mindustry.world.blocks.defense;

import arc.Core;
import arc.struct.IntSet;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.util.*;
import mindustry.content.Fx;
import mindustry.entities.Effects;
import mindustry.entities.type.TileEntity;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class MendProjector extends Block{
    private static final IntSet healed = new IntSet();

    public final int timerUse = timers++;
    public Color baseColor = Color.valueOf("84f491");
    public Color phaseColor = Color.valueOf("ffd59e");
    public TextureRegion topRegion;
    public float reload = 250f;
    public float range = 60f;
    public float healPercent = 12f;
    public float phaseBoost = 12f;
    public float phaseRangeBoost = 50f;
    public float useTime = 400f;

    public MendProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
        entityType = MendEntity::new;
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
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.repairTime, (int)(100f / healPercent * reload / 60f), StatUnit.seconds);
        stats.add(BlockStat.range, range / tilesize, StatUnit.blocks);

        stats.add(BlockStat.boostEffect, phaseRangeBoost / tilesize, StatUnit.blocks);
        stats.add(BlockStat.boostEffect, (phaseBoost + healPercent) / healPercent, StatUnit.timesSpeed);
    }

    @Override
    public void update(Tile tile){
        MendEntity entity = tile.ent();
        entity.heat = Mathf.lerpDelta(entity.heat, entity.cons.valid() || tile.isEnemyCheat() ? 1f : 0f, 0.08f);
        entity.charge += entity.heat * entity.delta();

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, Mathf.num(entity.cons.optionalValid()), 0.1f);

        if(entity.cons.optionalValid() && entity.timer.get(timerUse, useTime) && entity.efficiency() > 0){
            entity.cons.trigger();
        }

        if(entity.charge >= reload){
            float realRange = range + entity.phaseHeat * phaseRangeBoost;
            entity.charge = 0f;

            int tileRange = (int)(realRange / tilesize + 1);
            healed.clear();

            for(int x = -tileRange + tile.x; x <= tileRange + tile.x; x++){
                for(int y = -tileRange + tile.y; y <= tileRange + tile.y; y++){
                    if(!Mathf.within(x * tilesize, y * tilesize, tile.drawx(), tile.drawy(), realRange)) continue;

                    Tile other = world.ltile(x, y);

                    if(other == null) continue;

                    if(other.getTeamID() == tile.getTeamID() && !healed.contains(other.pos()) && other.entity != null && other.entity.health < other.entity.maxHealth()){
                        other.entity.healBy(other.entity.maxHealth() * (healPercent + entity.phaseHeat * phaseBoost) / 100f * entity.efficiency());
                        Effects.effect(Fx.healBlockFull, Tmp.c1.set(baseColor).lerp(phaseColor, entity.phaseHeat), other.drawx(), other.drawy(), other.block().size);
                        healed.add(other.pos());
                    }
                }
            }
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range, Pal.accent);
    }

    @Override
    public void drawSelect(Tile tile){
        MendEntity entity = tile.ent();
        float realRange = range + entity.phaseHeat * phaseRangeBoost;

        Drawf.dashCircle(tile.drawx(), tile.drawy(), realRange, baseColor);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        MendEntity entity = tile.ent();
        float f = 1f - (Time.time() / 100f) % 1f;

        Draw.color(baseColor, phaseColor, entity.phaseHeat);
        Draw.alpha(entity.heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
        //Draw.blend(Blending.additive);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        //Draw.blend();

        Draw.alpha(1f);
        Lines.stroke((2f * f + 0.2f) * entity.heat);
        Lines.square(tile.drawx(), tile.drawy(), ((1f - f) * 8f) * size / 2f);

        Draw.reset();
    }

    @Override
    public void drawLight(Tile tile){
        renderer.lights.add(tile.drawx(), tile.drawy(), 50f * tile.entity.efficiency(), baseColor, 0.7f * tile.entity.efficiency());
    }

    class MendEntity extends TileEntity{
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
