package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
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
            Draw.color(team.color);
            Draw.alpha(0.22f);
            Fill.rect(x, y, 2f, 2f);
            Draw.color();
        }

        @Override
        public void unitOn(Unit unit){
            if(enabled && unit.team != team && timer(timerDamage, cooldown)){
                for(int i = 0; i < tendrils; i++){
                    Lightning.create(team, lightningColor, damage, x, y, Mathf.random(360f), length);
                }
                damage(tileDamage);
            }
        }
    }
}
