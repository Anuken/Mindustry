package io.anuke.mindustry.world.blocks.units;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

public abstract class Projector extends Block {
    protected final int timerApply = timers++;
    protected final float applyTime = 4f;

    protected float range = 80f;

    protected StatusEffect status;
    protected float intensity = 1f;

    public Projector(String name) {
        super(name);
        hasPower = true;
        update = true;
        solid = true;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), range);
        Draw.reset();
    }

    @Override
    public void update(Tile tile) {
        ProjectorEntity entity = tile.entity();

        if(entity.cons.valid()){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.01f);
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.01f);
        }

        if(entity.heat > 0.6f && Timers.get(timerApply, applyTime)) {
            Units.getNearby(tile.getTeam(), tile.drawx(), tile.drawy(), range, unit -> {
                unit.applyEffect(status, intensity);
            });
        }
    }

    @Override
    public TileEntity getEntity() {
        return new ProjectorEntity();
    }

    public class ProjectorEntity extends TileEntity{
        public float heat;
    }
}
