package io.anuke.mindustry.editor;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.editor.DrawOperation.OpType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.modules.*;

import static io.anuke.mindustry.Vars.ui;

//TODO somehow remove or replace this class with a more flexible solution
public class EditorTile extends Tile{

    public EditorTile(int x, int y, short floor, short overlay, short wall){
        super(x, y, floor, overlay, wall);
    }

    @Override
    public Team getTeam(){
        return Team.all[getTeamID()];
    }

    @Override
    public void setFloor(Floor type){
        if(type instanceof OverlayFloor){
            //don't place on liquids
            if(!floor.isLiquid){
                setOverlayID(type.id);
            }
            return;
        }

        if(floor == type && overlayID() == 0) return;
        if(overlayID() != 0) op(OpType.overlay, overlayID());
        if(floor != type) op(OpType.floor, floor.id);
        super.setFloor(type);
    }

    @Override
    public void setBlock(Block type){
        if(block == type) return;
        op(OpType.block, block.id);
        super.setBlock(type);

        //TODO check if this line is necessary
        //if(pteam != getTeamID()){
        //    op((byte)OpType.team.ordinal(), pteam, getTeamID());
        //}
    }

    @Override
    public void setTeam(Team team){
        if(getTeamID() == team.ordinal()) return;
        op(OpType.team, getTeamID());
        super.setTeam(team);
    }

    @Override
    public void rotation(int rotation){
        if(rotation == rotation()) return;
        op(OpType.rotation, rotation());
        super.rotation(rotation);
    }

    @Override
    public void setOverlayID(short overlay){
        if(overlayID() == overlay) return;
        op(OpType.overlay, overlay);
        super.setOverlayID(overlay);
    }

    @Override
    protected void preChanged(){
        super.setTeam(Team.none);
    }

    @Override
    protected void changed(){
        entity = null;

        if(block == null){
            block = Blocks.air;
        }

        if(floor == null){
            floor = (Floor)Blocks.air;
        }

        Block block = block();

        if(block.hasEntity()){
            entity = block.newEntity();
            entity.health = block.health;
            entity.cons = new ConsumeModule(entity);
            if(block.hasItems) entity.items = new ItemModule();
            if(block.hasLiquids) entity.liquids = new LiquidModule();
            if(block.hasPower) entity.power = new PowerModule();
        }
    }

    private void op(OpType type, short value){
        ui.editor.editor.addTileOp(TileOp.get(x, y, (byte)type.ordinal(), value));
    }
}
