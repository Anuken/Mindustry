package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.graphics.Draw;

public class TunnelConduit extends LiquidBlock {
    protected int maxdist = 3;
    protected float speed = 53;

    protected TunnelConduit(String name) {
        super(name);
        rotate = true;
        solid = true;
        health = 70;
        instantTransfer = true;
    }

    @Override
    public void setBars() {
        super.setBars();
        bars.remove(BarType.liquid);
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name)};
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(name, tile.drawx(), tile.drawy(), tile.getRotation() * 90);
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        Tile tunnel = getDestTunnel(tile);
        if (tunnel == null) return;
        Tile to = tunnel.getNearby(tunnel.getRotation());
        if (to == null || !(to.block().hasLiquids)) return;

        if (to.block().acceptLiquid(to, tunnel, liquid, amount))
            to.block().handleLiquid(to, tunnel, liquid, amount);
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        int rot = source.relativeTo(tile.x, tile.y);
        if (rot != (tile.getRotation() + 2) % 4) return false;
        Tile tunnel = getDestTunnel(tile);

        if (tunnel != null) {
            Tile to = tunnel.getNearby(tunnel.getRotation());
            return to != null && (to.block().hasLiquids) &&
                    (to.block()).acceptLiquid(to, tunnel, liquid, amount);
        } else {
            return false;
        }
    }

    Tile getDestTunnel(Tile tile) {
        Tile dest = tile;
        int rel = (tile.getRotation() + 2) % 4;
        for (int i = 0; i < maxdist; i++) {
            if (dest == null) return null;
            dest = dest.getNearby(rel);
            if (dest != null && dest.block() instanceof TunnelConduit && dest.getRotation() == rel
                    && dest.getNearby(rel) != null) {
                return dest;
            }
        }
        return null;
    }
}
