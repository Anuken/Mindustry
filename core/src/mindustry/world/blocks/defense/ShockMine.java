package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class ShockMine extends Block{
    public final int timerDamage = timers++;

    public float cooldown = 80f;
    public float tileDamage = 5f;
    public float damage = 13;
    public int length = 10;
    public int tendrils = 6;
    public Color lightningColor = Pal.lancerLaser;
    public int shots = 6;
    public float inaccuracy = 0f;
    public @Nullable BulletType bullet;
    public float teamAlpha = 0.3f;
    public @Load("@-team-top") TextureRegion teamRegion;

    public ShockMine(String name){
        super(name);
        update = false;
        destructible = true;
        solid = false;
        targetable = false;
    }

    public class ShockMineBuild extends Building{

        @Override
        public void drawTeam(){
            //no
        }

        @Override
        public void draw(){
            super.draw();
            Draw.color(team.color, teamAlpha);
            Draw.rect(teamRegion, x, y);
            Draw.color();
        }

        @Override
        public void drawCracks(){
            //no
        }

        @Override
        public void unitOn(Unit unit){
            if(enabled && unit.team != team && timer(timerDamage, cooldown)){
                triggered();
                damage(tileDamage);
            }
        }

        public void triggered(){
            for(int i = 0; i < tendrils; i++){
                Lightning.create(team, lightningColor, damage, x, y, Mathf.random(360f), length);
            }
            if(bullet != null){
                for(int i = 0; i < shots; i++){
                    bullet.create(this, x, y, (360f / shots) * i + Mathf.random(inaccuracy));
                }
            }
        }
    }
}
