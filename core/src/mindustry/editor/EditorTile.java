package mindustry.editor;

import arc.func.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class EditorTile extends Tile{

    public EditorTile(int x, int y, int floor, int overlay, int wall){
        super(x, y, floor, overlay, wall);
    }

    @Override
    public void setFloor(Floor type){
        if(skip()){
            super.setFloor(type);
            return;
        }

        if(type instanceof OverlayFloor){
            //don't place on liquids
            if(floor.hasSurface() || !type.needsSurface){
                setOverlay(type);
            }
            return;
        }

        if(floor == type) return;

        op(DrawOperation.opFloor, floor.id);

        this.floor = type;
        type.floorChanged(this);
    }

    @Override
    public boolean isEditorTile(){
        return true;
    }

    @Override
    public void setBlock(Block type, Team team, int rotation, Prov<Building> entityprov){
        Block prev = this.block;
        Tile prevCenter = (build == null ? this : build.tile);

        if(skip()){
            super.setBlock(type, team, rotation, entityprov);
            return;
        }

        if(this.block == type && (build == null || build.rotation == rotation)){
            return;
        }

        if(!isCenter()){
            EditorTile cen = (EditorTile)build.tile;
            cen.op(DrawOperation.opRotation, (byte)build.rotation);
            cen.op(DrawOperation.opTeam, (byte)build.team.id);
            cen.op(DrawOperation.opBlock, block.id);
            updateStatic();
        }else{
            if(build != null) op(DrawOperation.opRotation, (byte)build.rotation);
            if(build != null) op(DrawOperation.opTeam, (byte)build.team.id);
            op(DrawOperation.opBlock, block.id);
        }

        super.setBlock(type, team, rotation, entityprov);

        if(requiresBlockUpdate(type) || requiresBlockUpdate(prev)){
            if(prev.size > 1){
                prevCenter.getLinkedTilesAs(prev, tile -> editor.renderer.updateBlock(tile));
            }
            getLinkedTiles(tile -> editor.renderer.updateBlock(tile));
        }else{
            renderer.blocks.updateShadowTile(this);
        }
    }

    @Override
    public void setTeam(Team team){
        if(skip()){
            super.setTeam(team);
            return;
        }

        if(getTeamID() == team.id || !synthetic()) return;
        op(DrawOperation.opTeam, (byte)getTeamID());
        super.setTeam(team);

        getLinkedTiles(t -> editor.renderer.updateBlock(t.x, t.y));
    }

    @Override
    public void setOverlay(Block overlay){
        if(skip()){
            super.setOverlay(overlay);
            return;
        }

        if(!floor.hasSurface() && overlay.asFloor().needsSurface && (overlay instanceof OreBlock || !floor.supportsOverlay)) return;
        if(this.overlay == overlay) return;
        op(DrawOperation.opOverlay, this.overlay.id);
        super.setOverlay(overlay);
    }

    @Override
    protected void fireChanged(){
        if(skip()){
            super.fireChanged();
        }else{
            updateStatic();
        }
    }

    @Override
    protected void firePreChanged(){
        if(skip()){
            super.firePreChanged();
        }else{
            updateStatic();
        }
    }

    @Override
    public void recache(){
        if(skip()){
            super.recache();
        }
    }

    @Override
    protected void changed(){
        if(state.isGame()){
            super.changed();
        }
    }

    @Override
    protected void changeBuild(Team team, Prov<Building> entityprov, int rotation){
        if(skip()){
            super.changeBuild(team, entityprov, rotation);
            return;
        }

        build = null;

        if(block == null) block = Blocks.air;
        if(floor == null) floor = (Floor)Blocks.air;

        Block block = block();

        if(block.hasBuilding()){
            build = entityprov.get().init(this, team, false, rotation);
            if(block.hasItems) build.items = new ItemModule();
            if(block.hasLiquids) build.liquids = new LiquidModule();
            if(block.hasPower) build.power = new PowerModule();
        }
    }

    @Override
    public boolean isDarkened(){
        return skip() && super.isDarkened();
    }

    private boolean requiresBlockUpdate(Block block){
        return block != Blocks.air && block.cacheLayer == CacheLayer.normal;
    }

    private void updateStatic(){
        editor.renderer.updateStatic(x, y);
    }

    private boolean skip(){
        return state.isGame() || editor.isLoading() || world.isGenerating();
    }

    private void op(int type, short value){
        editor.addTileOp(TileOp.get(x, y, (byte)type, value));
    }
}
