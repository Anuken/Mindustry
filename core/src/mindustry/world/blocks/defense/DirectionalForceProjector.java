package mindustry.world.blocks.defense;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//TODO use completely different layer
//TODO consume heat
//TODO broken class!!!!!
public class DirectionalForceProjector extends Block{
    protected static final Vec2 intersectOut = new Vec2(), p1 = new Vec2(), p2 = new Vec2();
    protected static DirectionalForceProjectorBuild paramEntity;
    protected static Effect paramEffect;
    protected static final Cons<Bullet> dirShieldConsumer = b -> {
        if(b.team != paramEntity.team && b.type.absorbable){
            //just in case
            float deltaAdd = 1.1f;

            if(Intersector.intersectSegments(b.x, b.y,
                    b.x + b.vel.x * (Time.delta + deltaAdd),
                    b.y + b.vel.y * (Time.delta + deltaAdd), p1.x, p1.y, p2.x, p2.y, intersectOut)){
                b.set(intersectOut);
                b.absorb();
                paramEffect.at(b);
                paramEntity.hit = 1f;
                paramEntity.buildup += b.damage();
            }
        }
    };

    public float width = 30f;
    public float shieldHealth = 3000f;
    public float cooldownNormal = 1.75f;
    public float cooldownLiquid = 1.5f;
    public float cooldownBrokenBase = 0.35f;

    public Effect absorbEffect = Fx.absorb;
    public Effect shieldBreakEffect = Fx.shieldBreak;
    public @Load("@-top") TextureRegion topRegion;

    public float length = 40f;
    public float padSize = 40f;

    public DirectionalForceProjector(String name){
        super(name);

        rotate = true;
        rotateDraw = false;

        update = true;
        solid = true;
        group = BlockGroup.projectors;
        envEnabled |= Env.space;
        ambientSound = Sounds.shield;
        ambientSoundVolume = 0.08f;
    }

    @Override
    public void init(){
        updateClipRadius((width + 3f));

        super.init();

        if(length < 0){
            length = size * tilesize/2f;
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("shield", (DirectionalForceProjectorBuild entity) -> new Bar("stat.shieldhealth", Pal.accent, () -> entity.broken ? 0f : 1f - entity.buildup / (shieldHealth)).blink(Color.white));
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
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPotentialLinks(x, y);

        x *= tilesize;
        y *= tilesize;

        Tmp.v1.set(length - size/2f, (width + size/2f)).rotate(rotation * 90).add(x, y);
        Tmp.v2.set(length - size/2f, -(width + size/2f)).rotate(rotation * 90).add(x, y);

        Drawf.dashLine(Color.lightGray, x, y, Tmp.v1.x, Tmp.v1.y);
        Drawf.dashLine(Color.lightGray, x, y, Tmp.v2.x, Tmp.v2.y);
        Drawf.dashLine(Pal.accent, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
    }

    public class DirectionalForceProjectorBuild extends Building{
        public boolean broken = true;
        public float buildup, hit, warmup, shieldRadius;

        @Override
        public boolean shouldAmbientSound(){
            return !broken && shieldRadius > 1f;
        }

        @Override
        public void pickedUp(){
            super.pickedUp();
            shieldRadius = warmup = 0f;
        }

        @Override
        public void updateTile(){
            shieldRadius = Mathf.lerpDelta(shieldRadius, broken ? 0f : warmup * width, 0.05f);

            //TODO ?????????????????
            if(Mathf.chanceDelta(buildup / shieldHealth * 0.1f)){
                Fx.reactorsmoke.at(x + Mathf.range(tilesize / 2f), y + Mathf.range(tilesize / 2f));
            }

            warmup = Mathf.lerpDelta(warmup, efficiency, 0.1f);

            //TODO aaaaaaaaaaaaAAAAAAAAAAAAAAaa
            if(buildup > 0 && false){
                float scale = !broken ? cooldownNormal : cooldownBrokenBase;
                Consume cons = null;
                //if(cons.valid(this)){
                //    cons.update(this);
                //    scale *= (cooldownLiquid * (1f + (liquids.current().heatCapacity - 0.4f) * 0.9f));
                //}

                buildup -= delta() * scale;
            }

            if(broken && buildup <= 0){
                broken = false;
            }

            if(buildup >= shieldHealth && !broken){
                broken = true;
                buildup = shieldHealth;
                shieldBreakEffect.at(x, y, shieldRadius, team.color);
            }

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta;
            }

            deflectBullets();
        }

        public void deflectBullets(){

            if(shieldRadius > 0 && !broken){
                paramEntity = this;
                paramEffect = absorbEffect;

                //top
                p1.set(length, shieldRadius).rotate(rotdeg());
                //bot
                p2.set(length, -shieldRadius).rotate(rotdeg());

                //"check" radius is grown to catch bullets moving at high velocity
                Tmp.r1.set(p2.x, p2.y, p1.x - p2.x, p1.y - p2.y).normalize().grow(padSize);

                p1.add(x, y);
                p2.add(x, y);

                Groups.bullet.intersect(x + Tmp.r1.x, y + Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, dirShieldConsumer);
            }
        }

        @Override
        public void draw(){
            super.draw();

            //TODO wrong
            if(buildup > 0f){
                Draw.alpha(buildup / shieldHealth * 0.75f);
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.rect(topRegion, x, y);
                Draw.blend();
                Draw.z(Layer.block);
                Draw.reset();
            }

            drawShield();
        }

        public void drawShield(){
            if(!broken && shieldRadius > 0){
                float rot = rotdeg();

                p1.set(length, shieldRadius).rotate(rot).add(this);
                p2.set(length, -shieldRadius).rotate(rot).add(this);
                float size = 4f;
                Tmp.r1.set(p2.x, p2.y, p1.x - p2.x, p1.y - p2.y).normalize().grow(size);

                Draw.z(Layer.shields);

                Draw.color(team.color, Color.white, Mathf.clamp(hit));

                if(renderer.animateShields){
                    Fill.rect(Tmp.r1);

                    Tmp.v1.set(length - size/2f, (shieldRadius + size/2f)).rotate(rot).add(this);
                    Tmp.v2.set(length - size/2f, -(shieldRadius + size/2f)).rotate(rot).add(this);

                    //Fill.tri(x, y, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
                    Lines.stroke(3f);
                    Lines.line(x, y, Tmp.v1.x, Tmp.v1.y);
                    Lines.line(x, y, Tmp.v2.x, Tmp.v2.y);

                    Draw.z(Layer.shields);

                    for(int i : Mathf.signs){
                        Tmp.v1.set(length - size/2f, (shieldRadius + size/2f) * i).rotate(rot).add(this);
                        Tmp.v3.set(length + size/2f, (shieldRadius + size/2f) * i).rotate(rot).add(this);
                        Tmp.v2.set(length, (shieldRadius + size) * i).rotate(rot).add(this);
                        Fill.tri(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, Tmp.v3.x, Tmp.v3.y);
                    }
                }else{
                    Lines.stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.rect(Tmp.r1);
                    Draw.alpha(1f);
                    Lines.rect(Tmp.r1);
                    Draw.reset();
                }

                Draw.reset();
            }
        }

        //TODO
        /*
        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(broken);
            write.f(buildup);
            write.f(radscl);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            broken = read.bool();
            buildup = read.f();
            radscl = read.f();
            warmup = read.f();
        }*/
    }
}
