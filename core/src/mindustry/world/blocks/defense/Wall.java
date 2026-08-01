package mindustry.world.blocks.defense;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Wall extends Block{
    /** Lighting chance. -1 to disable */
    public float lightningChance = -1f;
    public float lightningDamage = 20f;
    public int lightningLength = 17;
    public Color lightningColor = Pal.surge;
    public Sound lightningSound = Sounds.shootArc;

    /** Bullet deflection chance. -1 to disable */
    public float chanceDeflect = -1f;
    public boolean flashHit;
    public Color flashColor = Color.white;
    public Sound deflectSound = Sounds.none;
    /** If true, this block uses autotiling; variants are not supported. See https://github.com/GglLfr/tile-gen*/
    public boolean autotile = false;

    protected TextureRegion[] autotileRegions;

    public Wall(String name){
        super(name);
        solid = true;
        destructible = true;
        group = BlockGroup.walls;
        buildCostMultiplier = 6f;
        canOverdrive = false;
        drawDisabled = false;
        crushDamageMultiplier = 5f;
        priority = TargetPriority.wall;

        //it's a wall of course it's supported everywhere
        envEnabled = Env.any;
    }

    @Override
    public void setStats(){
        super.setStats();

        if(chanceDeflect > 0f) stats.add(Stat.baseDeflectChance, chanceDeflect, StatUnit.none);
        if(lightningChance > 0f){
            stats.add(Stat.lightningChance, lightningChance * 100f, StatUnit.percent);
            stats.add(Stat.lightningDamage, lightningDamage, StatUnit.none);
        }
    }

    @Override
    public void load(){
        super.load();

        if(autotile){
            autotileRegions = TileBitmask.load(name);
        }
    }

    @Override
    public void init(){
        if(size == 2 && destroySound == Sounds.unset) destroySound = Sounds.blockExplodeWall;
        if(!flashHit){
            drawDynamic = false;
        }
        drawCached = true;
        super.init();
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    public class WallBuild extends Building{
        protected int autotileBits;
        protected float hit;

        protected void updateAutotileBits(){
            int prev = autotileBits;
            autotileBits = 0;
            for(int i = 0; i < 8; i++){
                int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;
                Tile other = tile.nearby(dx * size, dy * size);
                if(other != null && other.build != null && other.build.block == block && other.build.team == team){
                    autotileBits |= (1 << i);
                }
            }
            if(prev != autotileBits) recache();
        }

        protected void updateOtherBits(){
            for(int i = 0; i < 8; i++){
                int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;
                Tile other = tile.nearby(dx * size, dy * size);
                if(other != null && other.build != null && other.isCenter() && other.build.block == block && other.build.team == team && other.build instanceof WallBuild w){
                    w.updateAutotileBits();
                }
            }
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            if(autotile) updateAutotileBits();
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();

            if(autotile) updateOtherBits();
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            if(autotile && !world.isGenerating()) updateOtherBits();
        }

        @Override
        public void drawCached(){
            if(autotile){
                TextureRegion region = autotileRegions[TileBitmask.values[autotileBits]];
                Draw.rect(region, x, y);
            }else{
                super.draw();
            }
        }

        @Override
        public void draw(){

            //draw flashing white overlay if enabled
            if(flashHit && hit >= 0.001f){
                Draw.color(flashColor);
                Draw.alpha(hit * 0.5f);
                Draw.blend(Blending.additive);
                Fill.rect(x, y, tilesize * size, tilesize * size);
                Draw.blend();
                Draw.reset();

                if(!state.isPaused()){
                    hit = Mathf.clamp(hit - Time.delta / 10f);
                }
            }
        }

        @Override
        public boolean collision(Bullet bullet){
            super.collision(bullet);

            hit = 1f;

            //create lightning if necessary
            if(lightningChance > 0f){
                if(Mathf.chance(lightningChance)){
                    Lightning.create(team, lightningColor, lightningDamage, x, y, bullet.rotation() + 180f, lightningLength);
                    lightningSound.at(tile, Mathf.random(0.9f, 1.1f));
                }
            }

            //deflect bullets if necessary
            if(chanceDeflect > 0f){
                //slow bullets are not deflected
                if(bullet.vel.len() <= 0.1f || !bullet.type.reflectable) return true;

                //bullet reflection chance depends on bullet damage
                if(!Mathf.chance(chanceDeflect / bullet.damage())) return true;

                //make sound
                deflectSound.at(tile, Mathf.random(0.9f, 1.1f));

                //translate bullet back to where it was upon collision
                bullet.trns(-bullet.vel.x, -bullet.vel.y);

                float penX = Math.abs(x - bullet.x), penY = Math.abs(y - bullet.y);

                if(penX > penY){
                    bullet.vel.x *= -1;
                }else{
                    bullet.vel.y *= -1;
                }

                bullet.owner = this;
                bullet.team = team;
                bullet.time += 1f;

                //disable bullet collision by returning false
                return false;
            }

            return true;
        }
    }
}
