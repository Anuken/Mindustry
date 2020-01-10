package mindustry.world.blocks.units;

import arc.Core;
import arc.func.*;
import arc.struct.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.Units;
import mindustry.entities.type.*;
import mindustry.graphics.*;
import mindustry.world.Tile;
import mindustry.world.blocks.*;

import static mindustry.Vars.tilesize;

public class TractorBeam extends RepairPoint{
    protected float trackRadius = 1.2f; // modifier, not a value

    protected Boolf<Unit> targetValid = u -> !u.isDead() && u.isFlying();

    public TractorBeam(String name){
        super(name);
        laserColor = Color.white;
        flags = EnumSet.of(); // override/remove repair flag
        entityType = TractorBeamEntity::new;
    }

    public final BlockState

    // periodically check for a new target
    idle = new BlockState(){
        public void entered(Tile tile){
            ((TractorBeamEntity) tile.entity).target = null;
        }

        public void update(Tile tile){
            TractorBeamEntity entity = tile.ent();
            if(!entity.timer.get(timerTarget, 20)) return;
            Unit target = Units.closest(null, tile.drawx(), tile.drawy(), repairRadius * trackRadius, targetValid); //fixme: target enemy team, this is just to demo
            if(target == null) return;
            entity.target = target;
            entity.state.set(tile, track);
        }
    },

    // rotate slowly towards the target, then lock
    track = new BlockState(){
        public void update(Tile tile){
            TractorBeamEntity entity = tile.ent();

            // switch back to idle if it leaves track range
            if(entity.target.dst(tile) > repairRadius * trackRadius){
                entity.state.set(tile, idle);
                return;
            }

            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.05f);

            if(entity.target.dst(tile) > repairRadius) return;
            if(Angles.angleDist(entity.angleTo(entity.target), entity.rotation) < 2.5f) entity.state.set(tile, lock);
        }
    },

    // tell the laser to start pulling in
    lock = new BlockState(){
        public void entered(Tile tile){
            ((TractorBeamEntity) tile.entity).laser = true;
        }
        public void update(Tile tile){
            TractorBeamEntity entity = tile.ent();

            // switch back to tracking if it leaves lock range
            if(entity.target.dst(tile) > repairRadius){
                entity.state.set(tile, track);
                return;
            }

            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
            entity.strength = Mathf.clamp(1 - entity.dst(entity.target) / repairRadius + 0.5f, 0f, 1f);

            if (entity.target.dst(tile) < size * tilesize * 2) return;
            Tmp.v1.set(entity.target).sub(tile.drawx(), tile.drawy()).setLength(0.2f * entity.efficiency() * entity.strength).scl(0.45f * Time.delta());
            entity.target.applyImpulse(-Tmp.v1.x, -Tmp.v1.y);
        }
        public void exited(Tile tile){
            ((TractorBeamEntity) tile.entity).laser = false;
        }
    };

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find("coldfusion-laser");
        laserEnd = Core.atlas.find("coldfusion-laser-end");
        baseRegion = Core.atlas.find("block-" + size);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("block-" + size), Core.atlas.find(name)};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), repairRadius * trackRadius, Pal.accent);
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);
        Drawf.dashCircle(tile.drawx(), tile.drawy(), repairRadius * trackRadius, Pal.accent);
    }

    @Override
    public void update(Tile tile){
        TractorBeamEntity entity = tile.ent();

        if(entity.target != null && !targetValid.get(entity.target)) entity.state.set(tile, idle);

        if(entity.state.current() == null) entity.state.set(tile, idle);
        entity.state.update(tile);

    }

    class TractorBeamEntity extends RepairPointEntity{
        BlockStateMachine state = new BlockStateMachine();
    }
}
