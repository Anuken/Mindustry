package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.math.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.world;

public class NodeLogicBlock extends LogicBlock{
    private static int lastPlaced = Pos.invalid;

    protected float range = 100f;

    public NodeLogicBlock(String name){
        super(name);
        entityType = NodeLogicEntity::new;
        configurable = true;
    }

    @Override
    public int signal(Tile tile){
        NodeLogicEntity entity = tile.entity();
        //if(linkValid(tile)){
        //    return world.tile(entity.link)
        //}
        return 0;
    }

    @Override
    public void configured(Tile tile, @Nullable Player player, int value){
        super.configured(tile, player, value);
    }

    public boolean linkValid(Tile tile){
        NodeLogicEntity entity = tile.entity();
        return linkValid(tile, world.tile(entity.link));
    }

    public boolean linkValid(Tile tile, Tile other){
        return other != null && other.entity != null && other.block() instanceof NodeLogicBlock && Mathf.within(tile.drawx(), tile.drawy(), other.drawx(), other.drawy(), range);
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
