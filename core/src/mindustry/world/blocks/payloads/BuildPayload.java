package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BuildPayload implements Payload{
    public Building build;

    public BuildPayload(Block block, Team team){
        this.build = block.newBuilding().create(block, team);
        this.build.tile = emptyTile;
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
    public UnlockableContent content(){
        return build.block;
    }

    @Override
    public void update(boolean inUnit){
        if(inUnit && (!build.block.updateInUnits || (!state.rules.unitPayloadUpdate && !build.block.alwaysUpdateInUnits))) return;

        build.tile = emptyTile;
        build.update();
    }

    @Override
    public ItemStack[] requirements(){
        return build.block.requirements;
    }

    @Override
    public float buildTime(){
        return build.block.buildCost;
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
        build.payloadRotation = rotation;
    }

    @Override
    public void drawShadow(float alpha){
        Drawf.squareShadow(build.x, build.y, build.block.size * tilesize * 1.85f, alpha);
    }

    @Override
    public void draw(){
        float prevZ = Draw.z();
        Draw.z(prevZ - 0.0001f);
        drawShadow(1f);
        Draw.z(prevZ);
        Draw.zTransform(z -> z >= Layer.flyingUnitLow ? z : 0.0011f + Mathf.clamp(z, prevZ - 0.001f, prevZ + 0.9f));
        build.tile = emptyTile;
        build.payloadDraw();
        Draw.zTransform();
        Draw.z(prevZ);
    }

    @Override
    public TextureRegion icon(){
        return block().fullIcon;
    }
}
