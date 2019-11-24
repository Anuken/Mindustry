package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class NodeLogicBlock extends AcceptorLogicBlock{
    private static int lastPlaced = Pos.invalid;

    protected float range = 100f;

    public NodeLogicBlock(String name){
        super(name);
        entityType = NodeLogicEntity::new;
        configurable = true;
        posConfig = true;
        layer = Layer.power;
        rotate = false;
        doOutput = true;
    }

    @Override
    public int signal(Tile tile){
        NodeLogicEntity entity = tile.entity();
        if(linkValid(tile)){
            //when linked, accept signal
            return super.signal(tile);
        }
        return entity.nextSignal;
    }

    @Override
    public void playerPlaced(Tile tile){
        if(linkValid(tile, world.tile(lastPlaced))){
            world.tile(lastPlaced).configure(tile.pos());
            lastPlaced = Pos.invalid;
        }else{
            lastPlaced = tile.pos();
        }
    }

    @Override
    public void update(Tile tile){
        super.update(tile);
        NodeLogicEntity entity = tile.entity();
        Tile link = world.tile(entity.link);
        if(linkValid(tile, link)){
            NodeLogicEntity other = link.entity();
            other.nextSignal = entity.nextSignal;
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);

        if(tile == null) return;

        Lines.stroke(1f);
        Drawf.circles(x * tilesize + offset(), y * tilesize + offset(), range, Color.white);

        Tile last = world.tile(lastPlaced);
        if(linkValid(tile, last)){
            Drawf.square(last.drawx(), last.drawy(), last.block().size * tilesize / 2f + 1f, Pal.place);
        }

        Draw.reset();
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);

        Lines.stroke(1f);

        Drawf.circles(tile.drawx(), tile.drawy(), range, Color.white);
        Draw.reset();
    }

    @Override
    public void drawLayer(Tile tile){
        NodeLogicEntity entity = tile.entity();
        Tile link = world.tile(entity.link);
        if(linkValid(tile, link)){
            Draw.color(entity.nextSignal != 0 ? Pal.accent : Color.white);
            Draw.alpha(1f * Core.settings.getInt("lasersopacity") / 100f);
            Drawf.laser(Core.atlas.find("logic-laser"), Core.atlas.find("logic-laser-end"), tile.drawx(), tile.drawy(), link.drawx(), link.drawy(), 0.25f);
            Draw.reset();
        }
    }

    @Override
    public void drawConfigure(Tile tile){
        NodeLogicEntity entity = tile.entity();
        Draw.color(Color.white);

        Lines.stroke(1.5f);
        Drawf.square(tile.drawx(), tile.drawy(),
        tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time(), 4f, 1f), Color.white);

        Drawf.circles(tile.drawx(), tile.drawy(), range, Color.white);

        Lines.stroke(1.5f);
        int radius = Mathf.round(range / tilesize);

        Geometry.circle(tile.x, tile.y, radius, (x, y) -> {
            Tile link = world.ltile(x, y);

            if(linkValid(tile, link) && entity.link == link.pos()){
                Drawf.square(link.drawx(), link.drawy(), link.block().size * tilesize / 2f + 1f, Pal.place);
            }
        });

        Draw.reset();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        NodeLogicEntity entity = tile.entity();
        other = other.link();

        if(linkValid(tile, other)){
            tile.configure(entity.link == other.pos() ? Pos.invalid : other.pos());
            return false;
        }

        return true;
    }

    @Override
    public void configured(Tile tile, @Nullable Player player, int value){
        NodeLogicEntity entity = tile.entity();
        entity.link = value;
    }

    public boolean linkValid(Tile tile){
        NodeLogicEntity entity = tile.entity();
        return linkValid(tile, world.tile(entity.link));
    }

    public boolean linkValid(Tile tile, Tile other){
        return other != null && other.entity != null && other.block() instanceof NodeLogicBlock
            && Mathf.within(tile.drawx(), tile.drawy(), other.drawx(), other.drawy(), range)
            && (other.getTeam() == tile.getTeam() || !(tile.entity instanceof LogicEntity)) && (other.<NodeLogicEntity>entity().link != tile.pos());
    }

    public class NodeLogicEntity extends LogicEntity{
        public int link = Pos.invalid;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(link);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            link = stream.readInt();
        }
    }
}
