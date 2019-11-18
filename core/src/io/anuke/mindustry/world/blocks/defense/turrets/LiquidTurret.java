package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.*;
import io.anuke.mindustry.world.meta.values.*;

import static io.anuke.mindustry.Vars.*;

public class LiquidTurret extends Turret{
    protected ObjectMap<Liquid, BulletType> ammo = new ObjectMap<>();

    public LiquidTurret(String name){
        super(name);
        hasLiquids = true;
        activeSound = Sounds.spray;
    }

    /** Initializes accepted ammo map. Format: [liquid1, bullet1, liquid2, bullet2...] */
    protected void ammo(Object... objects){
        ammo = OrderedMap.of(objects);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.ammo, new AmmoListValue<>(ammo));
        consumes.add(new ConsumeLiquidFilter(i -> ammo.containsKey(i), 1f){
            @Override
            public boolean valid(TileEntity entity){
                return !((TurretEntity)entity).ammo.isEmpty();
            }

            @Override
            public void display(BlockStats stats){

            }
        });
    }

    @Override
    public boolean shouldActiveSound(Tile tile){
        TurretEntity entity = tile.entity();
        return entity.target != null && hasAmmo(tile);
    }

    @Override
    protected boolean validateTarget(Tile tile){
        TurretEntity entity = tile.entity();
        if(entity.liquids.current().canExtinguish() && entity.target instanceof Tile){
            return Fire.has(((Tile)entity.target).x, ((Tile)entity.target).y);
        }
        return super.validateTarget(tile);
    }

    @Override
    protected void findTarget(Tile tile){
        TurretEntity entity = tile.entity();
        if(entity.liquids.current().canExtinguish()){
            int tr = (int)(range / tilesize);
            for(int x = -tr; x <= tr; x++){
                for(int y = -tr; y <= tr; y++){
                    if(Fire.has(x + tile.x, y + tile.y)){
                        entity.target = world.tile(x + tile.x, y + tile.y);
                        return;
                    }
                }
            }
        }

        super.findTarget(tile);
    }

    @Override
    protected void effects(Tile tile){
        BulletType type = peekAmmo(tile);

        TurretEntity entity = tile.entity();

        Effects.effect(type.shootEffect, entity.liquids.current().color, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
        Effects.effect(type.smokeEffect, entity.liquids.current().color, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
        //shootSound.at(tile);

        if(shootShake > 0){
            Effects.shake(shootShake, shootShake, tile.entity);
        }

        entity.recoil = recoil;
    }

    @Override
    public BulletType useAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        if(tile.isEnemyCheat()) return ammo.get(entity.liquids.current());
        BulletType type = ammo.get(entity.liquids.current());
        entity.liquids.remove(entity.liquids.current(), type.ammoMultiplier);
        return type;
    }

    @Override
    public BulletType peekAmmo(Tile tile){
        return ammo.get(tile.entity.liquids.current());
    }

    @Override
    public boolean hasAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return ammo.get(entity.liquids.current()) != null && entity.liquids.total() >= ammo.get(entity.liquids.current()).ammoMultiplier;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return ammo.get(liquid) != null
        && (tile.entity.liquids.current() == liquid || (ammo.containsKey(tile.entity.liquids.current()) && tile.entity.liquids.get(tile.entity.liquids.current()) <= ammo.get(tile.entity.liquids.current()).ammoMultiplier + 0.001f));
    }

}
