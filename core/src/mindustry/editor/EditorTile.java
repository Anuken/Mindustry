package mindustry.editor;

import arc.util.ArcAnnotate.*;
import mindustry.content.Blocks;
import mindustry.core.GameState.State;
import mindustry.editor.DrawOperation.OpType;
import mindustry.game.Team;
import mindustry.gen.TileOp;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.*;
import mindustry.world.modules.*;

import static mindustry.Vars.state;
import static mindustry.Vars.ui;

public class EditorTile extends Tile{

    public EditorTile(int x, int y, int floor, int overlay, int wall){
        super(x, y, floor, overlay, wall);
    }

    @Override
    public void setFloor(@NonNull Floor type){
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
    public void setBlock(Block type, Team team, int rotation){
        if(state.is(State.playing)){
            super.setBlock(type, team, rotation);
            return;
        }

        op(OpType.block, block.id);
        if(rotation != 0) op(OpType.rotation, (byte)rotation);
        if(team() != Team.derelict) op(OpType.team, team().id);
        super.setBlock(type, team, rotation);
    }

    @Override
    public void setTeam(Team team){
        if(state.is(State.playing)){
            super.setTeam(team);
            return;
        }

        if(getTeamID() == team.id) return;
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
        if(state.is(State.playing)){
            super.setOverlay(overlay);
            return;
        }

        if(floor.isLiquid) return;
        if(overlay() == overlay) return;
        op(OpType.overlay, this.overlay.id);
        super.setOverlay(overlay);
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
    protected void changed(Team team){
        if(state.is(State.playing)){
            super.changed(team);
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
            entity = block.newEntity().init(this, team, false);
            entity.cons(new ConsumeModule(entity));
            if(block.hasItems) entity.items(new ItemModule());
            if(block.hasLiquids) entity.liquids(new LiquidModule());
            if(block.hasPower) entity.power(new PowerModule());
        }
    }

    private void op(OpType type, short value){
        ui.editor.editor.addTileOp(TileOp.get(x, y, (byte)type.ordinal(), value));
    }
}
