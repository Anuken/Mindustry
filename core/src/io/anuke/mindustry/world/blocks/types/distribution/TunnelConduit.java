package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidAcceptor;

public class TunnelConduit extends Conduit {
    protected int maxdist = 3;
    protected float speed = 53;

    protected TunnelConduit(String name) {
        super(name);
        rotate = true;
        update = false;
        solid = true;
        health = 70;
        instantTransfer = true;
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        Tile tunnel = getDestTunnel(tile, liquid, amount);
        if (tunnel == null) return;
        Tile to = tunnel.getNearby(tunnel.getRotation());
        if (to == null || !(to instanceof LiquidAcceptor)) return;

        LiquidAcceptor a = (LiquidAcceptor) to.block();

        if (a.acceptLiquid(tile, source, liquid, amount)) a.handleLiquid(to, tunnel, liquid, amount);
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        TunnelConveyor.TunnelEntity entity = tile.entity();

        if (entity.index >= entity.buffer.length - 1) return false;

        int rot = source.relativeTo(tile.x, tile.y);
        if (rot != (tile.getRotation() + 2) % 4) return false;
        Tile tunnel = getDestTunnel(tile, liquid, amount);

        if (tunnel != null) {
            Tile to = tunnel.getNearby(tunnel.getRotation());
            return to != null && (to instanceof LiquidAcceptor) && ((LiquidAcceptor) to.block()).acceptLiquid(to, tunnel, liquid, amount);
        } else {
            return false;
        }
    }

    Tile getDestTunnel(Tile tile, Liquid liquid, float amount) {
        Tile dest = tile;
        int rel = (tile.getRotation() + 2) % 4;
        for (int i = 0; i < maxdist; i++) {
            if (dest == null) return null;
            dest = dest.getNearby(rel);
            if (dest != null && dest.block() instanceof TunnelConduit && dest.getRotation() == rel
                    && dest.getNearby(rel) != null
                    && ((TunnelConduit) dest.getNearby(rel).block()).acceptLiquid(dest.getNearby(rel), dest, liquid, amount)) {
                return dest;
            }
        }
        return null;
    }
}
