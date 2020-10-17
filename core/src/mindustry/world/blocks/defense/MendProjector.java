package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MendProjector extends BoostableBlock{
    public Color baseColor = Color.valueOf("84f491");
    public Color phaseColor = Color.valueOf("ffd59e");
    public @Load("@-top") TextureRegion topRegion;
    public float reload = 250f;
    public float range = 60f;
    public float healPercent = 12f;
    public float phaseBoost = 12f;
    public float phaseRangeBoost = 50f;

    public MendProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
        acceptItemBooster = true;
        useTime = 400f;
    }

    @Override
    public boolean outputsItems(){
        return false;
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
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.accent);
    }

    public class MendBuild extends BoostableBlockBuild{
        float heat;
        float charge = Mathf.random(reload);

        @Override
        public float range(){
            return range;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            
            heat = Mathf.lerpDelta(heat, consValid() || cheating() ? 1f : 0f, 0.08f);
            charge += heat * delta();

            if(charge >= reload){
                float realRange = range + currentItemBoost * phaseRangeBoost;
                charge = 0f;

                indexer.eachBlock(this, realRange, other -> other.damaged(), other -> {
                    other.heal(other.maxHealth() * (healPercent + currentItemBoost * phaseBoost) / 100f * efficiency());
                    Fx.healBlockFull.at(other.x, other.y, other.block.size, Tmp.c1.set(baseColor).lerp(phaseColor, currentItemBoost));
                });
            }
        }

        @Override
        public void drawSelect(){
            float realRange = range + currentItemBoost * phaseRangeBoost;

            indexer.eachBlock(this, realRange, other -> true, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));

            Drawf.dashCircle(x, y, realRange, baseColor);
        }

        @Override
        public void draw(){
            super.draw();

            float f = 1f - (Time.time() / 100f) % 1f;

            Draw.color(baseColor, phaseColor, currentItemBoost);
            Draw.alpha(heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
            Draw.rect(topRegion, x, y);
            Draw.alpha(1f);
            Lines.stroke((2f * f + 0.2f) * heat);
            Lines.square(x, y, Math.min(1f + (1f - f) * size * tilesize / 2f, size * tilesize/2f));

            Draw.reset();
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, 50f * efficiency(), baseColor, 0.7f * efficiency());
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(currentItemBoost);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            currentItemBoost = read.f();
        }
    }
}
