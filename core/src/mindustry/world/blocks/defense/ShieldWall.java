package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ShieldWall extends Wall{
    public float shieldHealth = 900f;
    public float breakCooldown = 60f * 10f;
    public float regenSpeed = 2f;

    public Color glowColor = Color.valueOf("ff7531").a(0.5f);
    public float glowMag = 0.6f, glowScl = 8f;

    public @Load("@-glow") TextureRegion glowRegion;

    public ShieldWall(String name){
        super(name);

        update = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.shieldHealth, shieldHealth);
    }

    public class ShieldWallBuild extends WallBuild{
        public float shield = shieldHealth, shieldRadius = 0f;
        public float breakTimer;

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);

            if(shieldRadius > 0){
                float radius = shieldRadius * tilesize;

                Draw.z(Layer.shields);

                Draw.color(team.color, Color.white, Mathf.clamp(hit));

                if(renderer.animateShields){
                    Fill.square(x, y, radius);
                }else{
                    Lines.stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.square(x, y, radius);
                    Draw.alpha(1f);
                    Lines.poly(x, y, 4, radius, 45f);
                    Draw.reset();
                }

                Draw.reset();

                Drawf.additive(glowRegion, glowColor, (1f - glowMag + Mathf.absin(glowScl, glowMag)) * shieldRadius, x, y, 0f, Layer.blockAdditive);
            }
        }

        @Override
        public void updateTile(){
            if(breakTimer > 0){
                breakTimer -= Time.delta;
            }else{
                //regen when not broken
                shield = Mathf.clamp(shield + regenSpeed * edelta(), 0f, shieldHealth);
            }

            if(hit > 0){
                hit -= Time.delta / 10f;
                hit = Math.max(hit, 0f);
            }

            shieldRadius = Mathf.lerpDelta(shieldRadius, broken() ? 0f : 1f, 0.12f);
        }

        public boolean broken(){
            return breakTimer > 0 || !canConsume();
        }

        @Override
        public void damage(float damage){
            float shieldTaken = broken() ? 0f : Math.min(shield, damage);

            shield -= shieldTaken;
            if(shieldTaken > 0){
                hit = 1f;
            }

            //shield was destroyed, needs to go down
            if(shield <= 0.00001f && shieldTaken > 0){
                breakTimer = breakCooldown;
            }

            if(damage - shieldTaken > 0){
                super.damage(damage - shieldTaken);
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(shield);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            shield = read.f();
            if(shield > 0) shieldRadius = 1f;
        }
    }
}
