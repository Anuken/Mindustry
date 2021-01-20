package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class ShockMine extends Block{
    public final int timerDamage = timers++;

    public float cooldown = 80f;
    public float tileDamage = 5f;
    public float damage = 13;
    public float indicatorWidth = 1f;
    public int length = 10;
    public int tendrils = 6;
    public Color lightningColor = Pal.lancerLaser;

    public ShockMine(String name){
        super(name);
        update = false;
        destructible = true;
        solid = false;
        targetable = false;
        rebuildable = false;
    }

    public class ShockMineBuild extends Building{

        @Override
        public void drawTeam(){
            //no
        }

        @Override
        public void draw(){
            super.draw();
            if(player.team() != team) return;
            Draw.color(Tmp.c1.set(Color.black).lerp(team.color, healthf() + Mathf.absin(Time.time, Math.max(healthf() * 5f, 1f), 1f - healthf())));
            Draw.alpha(0.75f);
            Fill.square(x, y, indicatorWidth);
            Draw.color();
        }

        @Override
        public void unitOn(Unit unit){
            if(health - tileDamage > 1 && enabled && unit.team != team && timer(timerDamage, cooldown)){
                for(int i = 0; i < tendrils; i++){
                    Lightning.create(team, lightningColor, damage, x, y, Mathf.random(360f), length);
                }
                damage(tileDamage);
            }
        }

        @Override
        public void drawCracks() {
            //no
        }
    }
}
