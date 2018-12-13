package io.anuke.mindustry.world.blocks.defense.turrets;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.values.LiquidFilterValue;
import io.anuke.ucore.core.Effects;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public abstract class LiquidTurret extends Turret{
    protected AmmoType[] ammoTypes;
    protected ObjectMap<Liquid, AmmoType> liquidAmmoMap = new ObjectMap<>();

    public LiquidTurret(String name){
        super(name);
        hasLiquids = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.inputLiquid, new LiquidFilterValue(item -> liquidAmmoMap.containsKey(item)));
    }

    @Override
    protected boolean validateTarget(Tile tile) {
        TurretEntity entity = tile.entity();
        if(entity.liquids.current().canExtinguish() && entity.target instanceof Tile){
            return Fire.has(((Tile) entity.target).x, ((Tile) entity.target).y);
        }
        return super.validateTarget(tile);
    }

    @Override
    protected void findTarget(Tile tile) {
        TurretEntity entity = tile.entity();
        if(entity.liquids.current().canExtinguish()){
            int tr = (int)(range / tilesize);
            for (int x = -tr; x <= tr; x++) {
                for (int y = -tr; y <= tr; y++) {
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
    public void setBars(){
        super.setBars();
        bars.remove(BarType.inventory);
        bars.replace(new BlockBar(BarType.liquid, true, tile -> tile.entity.liquids.total() / liquidCapacity));
    }

    @Override
    protected void effects(Tile tile){
        AmmoType type = peekAmmo(tile);

        TurretEntity entity = tile.entity();

        Effects.effect(type.shootEffect, type.liquid.color, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
        Effects.effect(type.smokeEffect, type.liquid.color, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);

        if(shootShake > 0){
            Effects.shake(shootShake, shootShake, tile.entity);
        }

        entity.recoil = recoil;
    }

    @Override
    public AmmoType useAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        if(tile.isEnemyCheat()) return liquidAmmoMap.get(entity.liquids.current());
        AmmoType type = liquidAmmoMap.get(entity.liquids.current());
        entity.liquids.remove(type.liquid, type.quantityMultiplier);
        return type;
    }

    @Override
    public AmmoType peekAmmo(Tile tile){
        return liquidAmmoMap.get(tile.entity.liquids.current());
    }

    @Override
    public boolean hasAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return liquidAmmoMap.get(entity.liquids.current()) != null && entity.liquids.total() >= liquidAmmoMap.get(entity.liquids.current()).quantityMultiplier;
    }

    @Override
    public void init(){
        super.init();

        for(AmmoType type : ammoTypes){
            if(liquidAmmoMap.containsKey(type.liquid)){
                throw new RuntimeException("Turret \"" + name + "\" has two conflicting ammo entries on liquid type " + type.liquid + "!");
            }else{
                liquidAmmoMap.put(type.liquid, type);
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return super.acceptLiquid(tile, source, liquid, amount) && liquidAmmoMap.get(liquid) != null
                && (tile.entity.liquids.current() == liquid || (liquidAmmoMap.containsKey(tile.entity.liquids.current()) && tile.entity.liquids.get(tile.entity.liquids.current()) <= liquidAmmoMap.get(tile.entity.liquids.current()).quantityMultiplier + 0.001f));
    }

}
