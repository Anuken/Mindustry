package io.anuke.mindustry.world.blocks.logic;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

public abstract class LogicBlock extends Block{
    protected boolean doOutput = true;

    public LogicBlock(String name){
        super(name);
        rotate = true;
        group = BlockGroup.logic;
        update = true;
        entityType = LogicEntity::new;
        controllable = false;
    }

    @CallSuper
    @Override
    public void update(Tile tile){
        LogicEntity entity = tile.entity();
        entity.lastSignal = entity.nextSignal;
        entity.nextSignal = signal(tile);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("signal", entity -> new Bar(
        () -> Core.bundle.format("block.signal", ((LogicEntity)entity).nextSignal),
        () -> Color.clear,
        () -> 0));
    }

    @Override
    public void draw(Tile tile){
        LogicEntity entity = tile.entity();
        Draw.rect("logic-base", tile.drawx(), tile.drawy());

        Draw.color(entity.nextSignal > 0 ? Pal.accent : Color.white);
        super.draw(tile);
        Draw.color();
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("logic-base"), Core.atlas.find(name)};
    }

    @Override
    public void drawRequestRegion(BuilderTrait.BuildRequest req, Eachable<BuildRequest> list) {
        TextureRegion back = Core.atlas.find("logic-base");
        Draw.rect(back, req.drawx(), req.drawy(),
        back.getWidth() * req.animScale * Draw.scl,
        back.getHeight() * req.animScale * Draw.scl,
        0);
        Draw.rect(region, req.drawx(), req.drawy(),
        region.getWidth() * req.animScale * Draw.scl,
        region.getHeight() * req.animScale * Draw.scl,
        !rotate ? 0 : req.rotation * 90);
    }

    public int getSignal(Tile from, Tile tile){
        return !canSignal(from, tile) ? 0 : tile.<LogicEntity>entity().lastSignal;
    }

    public boolean canSignal(Tile from, Tile tile){
        return tile != null && tile.block() instanceof LogicBlock && (!tile.block().rotate || tile.front() == from) && ((LogicBlock)tile.block()).doOutput;
    }

    public int sfront(Tile tile){
        return getSignal(tile, tile.front());
    }

    public int sback(Tile tile){
        return getSignal(tile, tile.back());
    }

    public int sleft(Tile tile){
        return getSignal(tile, tile.left());
    }

    public int sright(Tile tile){
        return getSignal(tile, tile.right());
    }

    /** @return signal to send next frame. */
    public abstract int signal(Tile tile);

    public class LogicEntity extends TileEntity{
        public int nextSignal;
        public int lastSignal;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(nextSignal);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            nextSignal = lastSignal = stream.readInt();
        }
    }
}
