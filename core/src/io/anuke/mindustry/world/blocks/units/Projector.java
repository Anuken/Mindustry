package io.anuke.mindustry.world.blocks.units;

import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public abstract class Projector extends Block {
    protected final int timerApply = timers++;
    protected final float applyTime = 4f;

    protected float powerUse = 0.01f;
    protected float range = 40f;

    protected StatusEffect status;
    protected float intensity;

    public Projector(String name) {
        super(name);
        hasPower = true;
        update = true;
        solid = true;
    }

    @Override
    public void update(Tile tile) {
        if(Timers.get(timerApply, applyTime)) {
            Units.getNearby(tile.getTeam(), tile.drawx(), tile.drawy(), range, unit -> {
                unit.applyEffect(status, intensity);
            });
        }
    }
}
