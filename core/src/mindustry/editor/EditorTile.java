package mindustry.editor;

import arc.func.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.editor.DrawOperation.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class EditorTile extends Tile{

    public EditorTile(int x, int y, int floor, int overlay, int wall){
        super(x, y, floor, overlay, wall);
    }

    @Override
    public void setFloor(@NonNull Floor type){
        if(skip()){
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
        if(skip()){
            super.setBlock(type, team, rotation);
            return;
        }

        //if(this.block == type && (build == null || build.rotation == rotation)){
        //    ui.editor.editor.renderer().updatePoint(x, y);
        //    return;
        //}

        if(rotation != 0) op(OpType.rotation, (byte)rotation);
        if(team() != Team.derelict) op(OpType.team, (byte)team().id);
        op(OpType.block, block.id);
        super.setBlock(type, team, rotation);
    }

    @Override
    public void setTeam(Team team){
        if(skip()){
            super.setTeam(team);
            return;
        }

        if(getTeamID() == team.id) return;
        op(OpType.team, (byte)getTeamID());
        super.setTeam(team);
    }

    @Override
    public void setOverlay(Block overlay){
        if(skip()){
            super.setOverlay(overlay);
            return;
        }

        if(floor.isLiquid) return;
        if(overlay() == overlay) return;
        op(OpType.overlay, this.overlay.id);
        super.setOverlay(overlay);
    }

    @Override
    protected void fireChanged(){
        if(skip()){
            super.fireChanged();
        }else{
            ui.editor.editor.renderer().updatePoint(x, y);
        }
    }

    @Override
    public void recache(){
        if(skip()){
            super.recache();
        }
    }
    
    @Override
    protected void changeEntity(Team team, Prov<Building> entityprov, int rotation){
        if(skip()){
            super.changeEntity(team, entityprov, rotation);
            return;
        }

        build = null;

        if(block == null) block = Blocks.air;
        if(floor == null) floor = (Floor)Blocks.air;
        
        Block block = block();

        if(block.hasEntity()){
            build = entityprov.get().init(this, team, false, rotation);
            build.cons = new ConsumeModule(build);
            if(block.hasItems) build.items = new ItemModule();
            if(block.hasLiquids) build.liquids(new LiquidModule());
            if(block.hasPower) build.power(new PowerModule());
        }
    }

    private boolean skip(){
        return state.isGame() || ui.editor.editor.isLoading();
    }

    private void op(OpType type, short value){
        ui.editor.editor.addTileOp(TileOp.get(x, y, (byte)type.ordinal(), value));
    }
}
