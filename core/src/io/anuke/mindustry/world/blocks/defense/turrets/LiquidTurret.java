package io.anuke.mindustry.world.blocks.defense.turrets;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.values.LiquidFilterValue;
import io.anuke.ucore.core.Effects;

public abstract class LiquidTurret extends Turret {
    protected AmmoType[] ammoTypes;
    protected ObjectMap<Liquid, AmmoType> liquidAmmoMap = new ObjectMap<>();

    public LiquidTurret(String name) {
        super(name);
        hasLiquids = true;
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(BlockStat.inputLiquid, new LiquidFilterValue(item -> liquidAmmoMap.containsKey(item)));
    }

    @Override
    public void setBars() {
        super.setBars();
        bars.remove(BarType.inventory);
        bars.replace(new BlockBar(BarType.liquid, true, tile -> tile.entity.liquids.amount / liquidCapacity));
    }

    @Override
    protected void effects(Tile tile){
        AmmoType type = peekAmmo(tile);

        TurretEntity entity = tile.entity();

        Effects.effect(shootEffect, type.liquid.color, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
        Effects.effect(smokeEffect, type.liquid.color, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);

        if (shootShake > 0) {
            Effects.shake(shootShake, shootShake, tile.entity);
        }

        entity.recoil = recoil;
    }

    @Override
    public AmmoType useAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        AmmoType type = liquidAmmoMap.get(entity.liquids.liquid);
        entity.liquids.amount -= type.quantityMultiplier;
        return type;
    }

    @Override
    public AmmoType peekAmmo(Tile tile){
        return liquidAmmoMap.get(tile.entity.liquids.liquid);
    }

    @Override
    public boolean hasAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return liquidAmmoMap.get(entity.liquids.liquid) != null && entity.liquids.amount >= liquidAmmoMap.get(entity.liquids.liquid).quantityMultiplier;
    }

    @Override
    public void init(){
        super.init();

        for (AmmoType type : ammoTypes) {
            if (liquidAmmoMap.containsKey(type.liquid)) {
                throw new RuntimeException("Turret \"" + name + "\" has two conflicting ammo entries on liquid type " + type.liquid + "!");
            } else {
                liquidAmmoMap.put(type.liquid, type);
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return false;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return super.acceptLiquid(tile, source, liquid, amount) && liquidAmmoMap.get(liquid) != null;
    }

}
