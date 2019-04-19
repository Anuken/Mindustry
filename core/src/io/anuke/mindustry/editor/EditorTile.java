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

public class EditorTile extends Tile{

    public EditorTile(int x, int y, byte floor, byte wall){
        super(x, y, floor, wall);
    }

    @Override
    public Team getTeam(){
        return Team.all[getTeamID()];
    }

    @Override
    public void setFloor(Floor type){
        if(type instanceof OverlayFloor){
            //don't place on liquids
            setOverlayID(type.id);
            return;
        }

        Block previous = floor();
        Block ore = overlay();
        if(previous == type && ore == Blocks.air) return;
        super.setFloor(type);
        //ore may get nullified so make sure to save editrs
        if(overlay() != ore){
            op(TileOp.get(x, y, (byte)OpType.ore.ordinal(), ore.id, overlay().id));
        }
        if(previous != type){
            op(TileOp.get(x, y, (byte)OpType.floor.ordinal(), previous.id, type.id));
        }
    }

    @Override
    public void setBlock(Block type){
        Block previous = block;
        byte pteam = getTeamID();
        if(previous == type) return;
        super.setBlock(type);
        if(pteam != getTeamID()){
            op(TileOp.get(x, y, (byte)OpType.team.ordinal(), pteam, getTeamID()));
        }
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
    public void setOverlayID(byte ore){
        byte previous = getOverlayID();
        if(previous == ore) return;
        super.setOverlayID(ore);
        op(TileOp.get(x, y, (byte)OpType.ore.ordinal(), previous, ore));
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

    private static void op(long op){
        ui.editor.editor.addTileOp(op);
    }
}
