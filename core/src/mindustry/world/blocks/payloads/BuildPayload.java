package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BuildPayload implements Payload{
    public Building build;

    public BuildPayload(Block block, Team team){
        this.build = block.newBuilding().create(block, team);
    }

    public BuildPayload(Building build){
        this.build = build;
    }

    public Block block(){
        return build.block;
    }

    public void place(Tile tile){
        place(tile, 0);
    }

    public void place(Tile tile, int rotation){
        tile.setBlock(build.block, build.team, rotation, () -> build);
        build.dropped();
    }

    @Override
    public float x(){
        return build.x;
    }

    @Override
    public float y(){
        return build.y;
    }

    @Override
    public float size(){
        return build.block.size * tilesize;
    }

    @Override
    public void write(Writes write){
        write.b(payloadBlock);
        write.s(build.block.id);
        write.b(build.version());
        build.writeAll(write);
    }

    @Override
    public void set(float x, float y, float rotation){
        build.set(x, y);
    }

    @Override
    public void draw(){
        Drawf.shadow(build.x, build.y, build.block.size * tilesize * 2f);
        Draw.rect(build.block.fullIcon, build.x, build.y);
    }

    @Override
    public TextureRegion icon(){
        return block().fullIcon;
    }
}
