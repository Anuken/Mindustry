package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MendProjector extends Block{
    public final int timerUse = timers++;
    public Color baseColor = Pal.heal;
    public Color phaseColor = Pal.accent;
    public @Load("@-top") TextureRegion topRegion;
    public float reload = 250f;
    public float range = 60f;
    public float healPercent = 12f;
    public float phaseBoost = 12f;
    public float phaseRangeBoost = 50f;
    public float useTime = 400f;
    public boolean hasBoost = true;

    public MendProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
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
        
        if(hasBoost){
            stats.add(BlockStat.boostEffect, phaseRangeBoost / tilesize, StatUnit.blocks);
            stats.add(BlockStat.boostEffect, (phaseBoost + healPercent) / healPercent, StatUnit.timesSpeed);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        //inner circle
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, baseColor);

        boolean boosterUnlocked = true;
        for(ItemStack item : consumes.getItem().items) {
            if(!item.item.unlockedNow()) {
                boosterUnlocked = false;
                break;
            }
        }

        if(hasBoost && boosterUnlocked) {
            float expandProgress = (Time.time() % 90f <= 30f ? Time.time() % 90f : 30f) / 30f;
            float transparency = Time.time() % 90f / 90f;

            //outside circle
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + phaseRangeBoost, phaseColor, 0.5f);
            
            //expanding circle
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + expandProgress * phaseRangeBoost, Tmp.c1.set(baseColor).lerp(phaseColor, transparency), 1f - transparency);
            
            //arrows
            float sin = Mathf.absin(Time.time(), 6f, 1f);
            for(int i = 0; i < 360; i += 60){
                Tmp.v1.trns(i, 0, range - sin);
                Tmp.v2.trns(i, 0, range + phaseRangeBoost);
                Drawf.arrow(x * tilesize + offset + Tmp.v1.x, y * tilesize + offset + Tmp.v1.y, x * tilesize + offset + Tmp.v2.x, y * tilesize + offset + Tmp.v2.y, phaseRangeBoost/2f + sin, 4f + sin, phaseColor);
            }
        }
    }

    public class MendBuild extends Building{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;

        @Override
        public void updateTile(){
            heat = Mathf.lerpDelta(heat, consValid() || cheating() ? 1f : 0f, 0.08f);
            charge += heat * delta();
            
            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(cons.optionalValid()), 0.1f);
            }
            
            if(cons.optionalValid() && timer(timerUse, useTime) && efficiency() > 0){
                consume();
            }

            if(charge >= reload){
                float realRange = range + phaseHeat * phaseRangeBoost;
                charge = 0f;

                indexer.eachBlock(this, realRange, other -> other.damaged(), other -> {
                    other.heal(other.maxHealth() * (healPercent + phaseHeat * phaseBoost) / 100f * efficiency());
                    Fx.healBlockFull.at(other.x, other.y, other.block.size, Tmp.c1.set(baseColor).lerp(phaseColor, phaseHeat));
                });
            }
        }

        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            if(!cons().optionalValid() || !hasBoost) {
                indexer.eachBlock(this, realRange, other -> true, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));
                Drawf.dashCircle(x, y, realRange, baseColor);
            } else {
                indexer.eachBlock(this, realRange, other -> true, other -> Drawf.selected(other, Tmp.c1.set(phaseColor).a(Mathf.absin(4f, 1f))));
                Drawf.dashCircle(x, y, realRange, phaseColor);
            }
        }

        @Override
        public void draw(){
            super.draw();

            float f = 1f - (Time.time() / 100f) % 1f;

            Draw.color(baseColor, phaseColor, phaseHeat);
            Draw.alpha(heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
            Draw.rect(topRegion, x, y);

            Draw.alpha(1f);
            Lines.stroke((2f * f + 0.2f) * heat);
            Lines.square(x, y, ((1f - f) * 8f) * size / 2f);

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
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            phaseHeat = read.f();
        }
    }
}
