package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Wall extends Block{
    public int variants = 0;

    public float lightningChance = -0.001f;
    public float lightningDamage = 20f;
    public int lightningLength = 17;
    public Color lightningColor = Pal.surge;

    public float chanceDeflect = 10f;
    public boolean flashHit;
    public Color flashColor = Color.white;
    public boolean deflect;

    public Wall(String name){
        super(name);
        solid = true;
        destructible = true;
        group = BlockGroup.walls;
        buildCostMultiplier = 5f;
    }

    @Override
    public void load(){
        super.load();

        if(variants != 0){
            variantRegions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegions[i] = Core.atlas.find(name + (i + 1));
            }
            region = variantRegions[0];
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    @Override
    public boolean canReplace(Block other){
        return super.canReplace(other) && health > other.health && size == other.size;
    }

    public class WallBuild extends Building{
        public float hit;

        @Override
        public void draw(){
            if(variants == 0){
                Draw.rect(region, x, y);
            }else{
                Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], x, y);
            }

            //draw flashing white overlay if enabled
            if(flashHit){
                if(hit < 0.0001f) return;

                Draw.color(flashColor);
                Draw.alpha(hit * 0.5f);
                Draw.blend(Blending.additive);
                Fill.rect(x, y, tilesize * size, tilesize * size);
                Draw.blend();
                Draw.reset();

                hit = Mathf.clamp(hit - Time.delta / 10f);
            }
        }

        @Override
        public boolean collision(Bullet bullet){
            super.collision(bullet);

            hit = 1f;

            //create lightning if necessary
            if(lightningChance > 0){
                if(Mathf.chance(lightningChance)){
                    Lightning.create(team, lightningColor, lightningDamage, x, y, bullet.rotation() + 180f, lightningLength);
                }
            }

            //deflect bullets if necessary
            if(deflect){
                //slow bullets are not deflected
                if(bullet.vel().len() <= 0.1f || !bullet.type.reflectable) return true;

                //bullet reflection chance depends on bullet damage
                if(!Mathf.chance(chanceDeflect / bullet.damage())) return true;

                //translate bullet back to where it was upon collision
                bullet.trns(-bullet.vel.x, -bullet.vel.y);

                float penX = Math.abs(x - bullet.x), penY = Math.abs(y - bullet.y);

                if(penX > penY){
                    bullet.vel.x *= -1;
                }else{
                    bullet.vel.y *= -1;
                }

                bullet.owner(this);
                bullet.team(team);
                bullet.time(bullet.time() + 1f);

                //disable bullet collision by returning false
                return false;
            }

            return true;
        }
    }
}
