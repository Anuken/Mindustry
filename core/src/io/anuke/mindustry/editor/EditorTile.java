package io.anuke.mindustry.editor;

import io.anuke.mindustry.editor.DrawOperation.OpType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.ui;

public class EditorTile extends Tile{

    public EditorTile(int x, int y, byte floor, byte wall){
        super(x, y, floor, wall);
    }

    @Override
    public void setFloor(Floor type){
        Block previous = floor();
        if(previous == type) return;
        super.setFloor(type);
        op(TileOp.get(x, y, (byte)OpType.floor.ordinal(), previous.id, type.id));
    }

    @Override
    public void setBlock(Block type){
        Block previous = block();
        if(previous == type) return;
        super.setBlock(type);
        op(TileOp.get(x, y, (byte)OpType.floor.ordinal(), previous.id, type.id));
    }

    @Override
    public void setTeam(Team team){
        byte previous = getTeamID();
        if(previous == team.ordinal()) return;
        super.setTeam(team);
        op(TileOp.get(x, y, (byte)OpType.floor.ordinal(), previous, (byte)team.ordinal()));
    }

    @Override
    public void setRotation(byte rotation){
        byte previous = getRotation();
        if(previous == rotation) return;
        super.setRotation(rotation);
        op(TileOp.get(x, y, (byte)OpType.floor.ordinal(), previous, rotation));
    }

    private static void op(long op){
        ui.editor.editor.addTileOp(op);
    }
}
