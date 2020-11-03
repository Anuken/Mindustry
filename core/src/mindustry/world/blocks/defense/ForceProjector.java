package mindustry.world.blocks.defense;

import arc.*;
import arc.func.*;
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
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ForceProjector extends Block{
    public final int timerUse = timers++;
    public float phaseUseTime = 350f;
    public Color phaseColor = Pal.accent;
    public float phaseRadiusBoost = 80f;
    public float phaseShieldBoost = 400f;
    public boolean hasBoost = true;
    public float radius = 101.7f;
    public float shieldHealth = 700f;
    public float cooldownNormal = 1.75f;
    public float cooldownLiquid = 1.5f;
    public float cooldownBrokenBase = 0.35f;
    public float basePowerDraw = 0.2f;
    public @Load("@-top") TextureRegion topRegion;

    static ForceBuild paramEntity;
    static final Cons<Bullet> shieldConsumer = trait -> {
        if(trait.team != paramEntity.team && trait.type.absorbable && Intersector.isInsideHexagon(paramEntity.x, paramEntity.y, paramEntity.realRadius() * 2f, trait.x(), trait.y())){
            trait.absorb();
            Fx.absorb.at(trait);
            paramEntity.hit = 1f;
            paramEntity.buildup += trait.damage() * paramEntity.warmup;
        }
    };

    public ForceProjector(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        hasLiquids = true;
        hasItems = true;
        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.1f)).boost().update(false);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("shield", (ForceBuild entity) -> new Bar("stat.shieldhealth", Pal.accent, () -> entity.broken ? 0f : 1f - entity.buildup / (shieldHealth + phaseShieldBoost * entity.phaseHeat)).blink(Color.white));
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.shieldHealth, shieldHealth, StatUnit.none);
        stats.add(Stat.cooldownTime, (int) (shieldHealth / cooldownBrokenBase / 60f), StatUnit.seconds);
        stats.add(Stat.powerUse, basePowerDraw * 60f, StatUnit.powerSecond);
        if(hasBoost){
            stats.add(Stat.boostEffect, phaseRadiusBoost / tilesize, StatUnit.blocks);
            stats.add(Stat.boostEffect, phaseShieldBoost, StatUnit.shieldHealth);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        //inner hexagon
        Drawf.hexagon(x * tilesize + offset, y * tilesize + offset, radius, player.team().color);
        
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

            //outside hexagon
            Drawf.hexagon(x * tilesize + offset, y * tilesize + offset, radius + phaseRadiusBoost, phaseColor, 0.25f);

            //expanding hexagon
            Drawf.hexagon(x * tilesize + offset, y * tilesize + offset, radius + expandProgress * phaseRadiusBoost, player.team().color.cpy().lerp(phaseColor, expandProgress), 1f - transparency);

            //arrows
            float sin = Mathf.absin(Time.time(), 6f, 1f);
            for(int i = 0; i < 360; i += 60){
                Tmp.v1.trns(i, 0, radius - sin);
                Tmp.v2.trns(i, 0, radius + phaseRadiusBoost);
                Drawf.arrow(x * tilesize + offset + Tmp.v1.x, y * tilesize + offset + Tmp.v1.y, x * tilesize + offset + Tmp.v2.x, y * tilesize + offset + Tmp.v2.y, phaseRadiusBoost/4f + sin, 4f + sin, phaseColor);
            }
        }
    }

    public class ForceBuild extends Building implements Ranged{
        public boolean broken = true;
        public float buildup, radscl, hit, warmup, phaseHeat;
        public ForceDraw drawer;

        @Override
        public float range(){
            return realRadius();
        }

        @Override
        public void created(){
            super.created();
            drawer = ForceDraw.create();
            drawer.build = this;
            drawer.set(x, y);
            drawer.add();
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            drawer.remove();
        }

        @Override
        public void updateTile(){
            boolean phaseValid = consumes.get(ConsumeType.item).valid(this);
            
            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid), 0.1f);
            }

            if(phaseValid && !broken && timer(timerUse, phaseUseTime) && efficiency() > 0){
                consume();
            }

            radscl = Mathf.lerpDelta(radscl, broken ? 0f : warmup, 0.05f);

            if(Mathf.chanceDelta(buildup / shieldHealth * 0.1f)){
                Fx.reactorsmoke.at(x + Mathf.range(tilesize / 2f), y + Mathf.range(tilesize / 2f));
            }

            warmup = Mathf.lerpDelta(warmup, efficiency(), 0.1f);

            if(buildup > 0){
                float scale = !broken ? cooldownNormal : cooldownBrokenBase;
                ConsumeLiquidFilter cons = consumes.get(ConsumeType.liquid);
                if(cons.valid(this)){
                    cons.update(this);
                    scale *= (cooldownLiquid * (1f + (liquids.current().heatCapacity - 0.4f) * 0.9f));
                }

                buildup -= delta() * scale;
            }

            if(broken && buildup <= 0){
                broken = false;
            }

            if(buildup >= shieldHealth + phaseShieldBoost * phaseHeat && !broken){
                broken = true;
                buildup = shieldHealth;
                Fx.shieldBreak.at(x, y, realRadius(), team.color);
            }

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta;
            }

            float realRadius = realRadius();

            if(realRadius > 0 && !broken){
                paramEntity = this;
                Groups.bullet.intersect(x - realRadius, y - realRadius, realRadius * 2f, realRadius * 2f, shieldConsumer);
            }
        }

        public float realRadius(){
            return (radius + phaseHeat * phaseRadiusBoost) * radscl;
        }

        @Override
        public void draw(){
            super.draw();

            if(drawer != null){
                drawer.set(x, y);
            }

            if(buildup > 0f){
                Draw.alpha(buildup / shieldHealth * 0.75f);
                Draw.blend(Blending.additive);
                Draw.rect(topRegion, x, y);
                Draw.blend();
                Draw.reset();
            }
        }
        
        @Override
        public void drawSelect(){
            if(!cons().optionalValid() && hasBoost && boosterUnlocked()){
                float expandProgress = (Time.time() % 90f <= 30f ? Time.time() % 90f : 30f) / 30f;
                float transparency = Time.time() % 90f / 90f;
                
                //outside hexagon
                Drawf.hexagon(x, y, radius + phaseRadiusBoost, phaseColor, 0.25f);

                //expanding hexagon
                Drawf.hexagon(x, y, radius + expandProgress * phaseRadiusBoost, player.team().color.cpy().lerp(phaseColor, expandProgress), 1f - transparency);

                //arrows
                float sin = Mathf.absin(Time.time(), 6f, 1f);
                for(int i = 0; i < 360; i += 60){
                    Tmp.v1.trns(i, 0, radius - sin);
                    Tmp.v2.trns(i, 0, radius + phaseRadiusBoost);
                    Drawf.arrow(x + Tmp.v1.x, y + Tmp.v1.y, x + Tmp.v2.x, y + Tmp.v2.y, phaseRadiusBoost/4f + sin, 4f + sin, phaseColor);
                }
            }
        }

        public void drawShield(){
            if(!broken){
                float radius = realRadius();

                Draw.z(Layer.shields);
                
                Draw.color(team.color.cpy().lerp(phaseColor, phaseHeat), Color.white, Mathf.clamp(hit));

                if(Core.settings.getBool("animatedshields")){
                    Fill.poly(x, y, 6, radius);
                }else{
                    Drawf.hexagon(x, y, radius, team.color.cpy().lerp(phaseColor, phaseHeat).lerp(Color.white, Mathf.clamp(hit)));
                }
            }

            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(broken);
            write.f(buildup);
            write.f(radscl);
            write.f(warmup);
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            broken = read.bool();
            buildup = read.f();
            radscl = read.f();
            warmup = read.f();
            phaseHeat = read.f();
        }
    }

    @EntityDef(value = {ForceDrawc.class}, serialize = false)
    @Component(base = true)
    abstract class ForceDrawComp implements Drawc{
        transient ForceBuild build;

        @Override
        public void draw(){
            build.drawShield();
        }

        @Replace
        @Override
        public float clipSize(){
            return build.realRadius() * 3f;
        }
    }
}
