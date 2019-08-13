package io.anuke.mindustry.editor;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.editor.DrawOperation.OpType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.modules.*;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.ui;

//TODO somehow remove or replace this class with a more flexible solution
public class EditorTile extends Tile{

    public EditorTile(int x, int y, int floor, int overlay, int wall){
        super(x, y, floor, overlay, wall);
    }

    @Override
    public void setFloor(Floor type){
        if(state.is(State.playing)){
            super.setFloor(type);
            return;
        }

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
        if(state.is(State.playing)){
            super.setBlock(type);
            return;
        }

        if(block == type) return;
        op(OpType.block, block.id);
        if(rotation != 0) op(OpType.rotation, rotation);
        if(team != 0) op(OpType.team, team);
        super.setBlock(type);
    }

    @Override
    public void setBlock(Block type, Team team, int rotation){
        if(state.is(State.playing)){
            super.setBlock(type, team, rotation);
            return;
        }

        setBlock(type);
        setTeam(team);
        rotation(rotation);
    }

    @Override
    public void setTeam(Team team){
        if(state.is(State.playing)){
            super.setTeam(team);
            return;
        }

        if(getTeamID() == team.ordinal()) return;
        op(OpType.team, getTeamID());
        super.setTeam(team);
    }

    @Override
    public void rotation(int rotation){
        if(state.is(State.playing)){
            super.rotation(rotation);
            return;
        }

        if(rotation == rotation()) return;
        op(OpType.rotation, rotation());
        super.rotation(rotation);
    }

    @Override
    public void setOverlay(Block overlay){
        setOverlayID(overlay.id);
    }

    @Override
    public void setOverlayID(short overlay){
        if(state.is(State.playing)){
            super.setOverlayID(overlay);
            return;
        }

        if(overlayID() == overlay) return;
        op(OpType.overlay, this.overlay.id);
        super.setOverlayID(overlay);
    }

    @Override
    protected void preChanged(){
        if(state.is(State.playing)){
            super.preChanged();
            return;
        }

        super.setTeam(Team.derelict);
    }

    @Override
    protected void changed(){
        if(state.is(State.playing)){
            super.changed();
            return;
        }

        entity = null;

        if(block == null){
            block = Blocks.air;
        }

        if(floor == null){
            floor = (Floor)Blocks.air;
        }

        Block block = block();

        if(block.hasEntity()){
            entity = block.newEntity().init(this, false);
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
