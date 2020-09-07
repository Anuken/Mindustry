package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.tilesize;

public class LiquidTurret extends Turret{
    public ObjectMap<Liquid, BulletType> ammoTypes = new ObjectMap<>();
    public @Load("@-liquid") TextureRegion liquidRegion;

    public LiquidTurret(String name){
        super(name);
        acceptCoolant = false;
        hasLiquids = true;
        activeSound = Sounds.spray;
    }

    /** Initializes accepted ammo map. Format: [liquid1, bullet1, liquid2, bullet2...] */
    protected void ammo(Object... objects){
        ammoTypes = OrderedMap.of(objects);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.ammo, new AmmoListValue<>(ammoTypes));
        consumes.add(new ConsumeLiquidFilter(i -> ammoTypes.containsKey(i), 1f){
            @Override
            public boolean valid(Building entity){
                return entity.liquids.total() > 0.001f;
            }

            @Override
            public void update(Building entity){

            }

            @Override
            public void display(BlockStats stats){

            }
        });
    }

    public class LiquidTurretEntity extends TurretEntity{

        @Override
        public void draw(){
            super.draw();
            
            if(Core.atlas.isFound(liquidRegion)){
                Draw.color(liquids.current().color);
                Draw.alpha(liquids.total() / liquidCapacity);
                Draw.rect(liquidRegion, x + tr2.x, y + tr2.y, rotation - 90);
                Draw.color();
            }
        }

        @Override
        public boolean shouldActiveSound(){
            return target != null && hasAmmo();
        }

        @Override
        protected void findTarget(){
            if(liquids.current().canExtinguish()){
                int tr = (int)(range / tilesize);
                for(int x = -tr; x <= tr; x++){
                    for(int y = -tr; y <= tr; y++){
                        if(Fires.has(x + tile.x, y + tile.y)){
                            target = Fires.get(x + tile.x, y + tile.y);
                            return;
                        }
                    }
                }
            }

            super.findTarget();
        }

        @Override
        protected void effects(){
            BulletType type = peekAmmo();

            type.shootEffect.at(x + tr.x, y + tr.y, rotation, liquids.current().color);
            type.smokeEffect.at(x + tr.x, y + tr.y, rotation, liquids.current().color);
            shootSound.at(tile);

            if(shootShake > 0){
                Effects.shake(shootShake, shootShake, tile.build);
            }

            recoil = recoilAmount;
        }

        @Override
        public BulletType useAmmo(){
            if(cheating()) return ammoTypes.get(liquids.current());
            BulletType type = ammoTypes.get(liquids.current());
            liquids.remove(liquids.current(), type.ammoMultiplier);
            return type;
        }

        @Override
        public BulletType peekAmmo(){
            return ammoTypes.get(liquids.current());
        }

        @Override
        public boolean hasAmmo(){
            return ammoTypes.get(liquids.current()) != null && liquids.total() >= ammoTypes.get(liquids.current()).ammoMultiplier;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid, float amount){
            return ammoTypes.get(liquid) != null
                && (liquids.current() == liquid || (ammoTypes.containsKey(liquids.current())
                && liquids.get(liquids.current()) <= ammoTypes.get(liquids.current()).ammoMultiplier + 0.001f));
        }
    }
}
