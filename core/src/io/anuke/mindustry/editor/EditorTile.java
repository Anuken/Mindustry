package io.anuke.mindustry.editor;

import io.anuke.mindustry.editor.DrawOperation.OpType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.modules.ConsumeModule;
import io.anuke.mindustry.world.modules.ItemModule;
import io.anuke.mindustry.world.modules.LiquidModule;
import io.anuke.mindustry.world.modules.PowerModule;

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
        op(TileOp.get(x, y, (byte)OpType.block.ordinal(), previous.id, type.id));
    }

    @Override
    public void setTeam(Team team){
        byte previous = getTeamID();
        if(previous == team.ordinal()) return;
        super.setTeam(team);
        op(TileOp.get(x, y, (byte)OpType.team.ordinal(), previous, (byte)team.ordinal()));
    }

    @Override
    public void setRotation(byte rotation){
        byte previous = getRotation();
        if(previous == rotation) return;
        super.setRotation(rotation);
        op(TileOp.get(x, y, (byte)OpType.rotation.ordinal(), previous, rotation));
    }

    @Override
    public void setOre(byte ore){
        byte previous = getRotation();
        if(previous == ore) return;
        super.setOre(ore);
        op(TileOp.get(x, y, (byte)OpType.ore.ordinal(), previous, ore));
    }

    @Override
    protected void preChanged(){
        super.setTeam(Team.none);
    }

    @Override
    protected void changed(){
        entity = null;

        Block block = block();

        if(block.hasEntity()){
            entity = block.newEntity();
            entity.cons = new ConsumeModule(entity);
            if(block.hasItems) entity.items = new ItemModule();
            if(block.hasLiquids) entity.liquids = new LiquidModule();
            if(block.hasPower) entity.power = new PowerModule();
        }
    }

    private static void op(long op){
        ui.editor.editor.addTileOp(op);
    }
}
