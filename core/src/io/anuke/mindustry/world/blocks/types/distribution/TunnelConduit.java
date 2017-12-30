package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class TunnelConduit extends LiquidBlock {

    protected int maxdist = 3;

    public TunnelConduit(String name) {
        super(name);
        this.rotate = true;
        this.solid = true;
        this.health = 10;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        int rot = source.relativeTo(tile.x, tile.y);

        if (rot != (tile.getRotation() + 2) % 4)
            return false;

        Tile tunnel = getDestTunnel(tile, liquid, amount);

        if (tunnel != null) {
            Tile to = tunnel.getNearby()[tunnel.getRotation()];
            return to != null && !(to.block() instanceof TunnelConduit) && ((LiquidBlock)to.block()).acceptLiquid(to, tunnel, liquid, amount);
        } else {
            return false;
        }
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        Tile tunnel = getDestTunnel(tile, liquid, amount);

        if (tunnel == null)
            return;

        Tile to = tunnel.getNearby()[tunnel.getRotation()];

        Timers.run(25, () -> {
            if (to == null || to.entity == null)
                return;

            ((LiquidBlock)to.block()).handleLiquid(to, tunnel, liquid, amount);
        });
    }

    @Override
    public float getLiquid(Tile tile) {
        return super.getLiquid(tile);
    }

    @Override
    public float getLiquidCapacity(Tile tile) {
        return super.getLiquidCapacity(tile);
    }

    protected Tile getDestTunnel(Tile tile, Liquid liquid, float amount){
        Tile dest = tile;
        int rel = (tile.getRotation() + 2)%4;
        for(int i = 0; i < maxdist; i ++){
            dest = dest.getNearby()[rel];
            if(dest != null && dest.block() instanceof TunnelConduit && dest.getRotation() == rel
                    && dest.getNearby()[rel] != null
                    && ((LiquidBlock) dest.getNearby()[rel].block()).acceptLiquid(dest, dest.getNearby()[rel], liquid, amount)){
                return dest;
            }
        }
        return null;
    }

    @Override
    public void draw(Tile tile) {
        Draw.rect(name(), tile.worldx(), tile.worldy(), tile.getRotation() * 90);
    }
}
