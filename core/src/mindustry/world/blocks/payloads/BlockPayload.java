package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.tilesize;

public class BlockPayload implements Payload{
    public Building entity;

    public BlockPayload(Block block, Team team){
        this.entity = block.newBuilding().create(block, team);
    }

    public BlockPayload(Building entity){
        this.entity = entity;
    }

    public Block block(){
        return entity.block;
    }

    public void place(Tile tile){
        place(tile, 0);
    }

    public void place(Tile tile, int rotation){
        tile.setBlock(entity.block, entity.team, rotation, () -> entity);
        entity.dropped();
    }

    @Override
    public float size(){
        return entity.block.size * tilesize;
    }

    @Override
    public void write(Writes write){
        write.b(payloadBlock);
        write.s(entity.block.id);
        write.b(entity.version());
        entity.writeAll(write);
    }

    @Override
    public void set(float x, float y, float rotation){
        entity.set(x, y);
    }

    @Override
    public void draw(){
        Drawf.shadow(entity.x, entity.y, entity.block.size * tilesize * 2f);
        Draw.rect(entity.block.icon(Cicon.full), entity.x, entity.y);
    }
}
