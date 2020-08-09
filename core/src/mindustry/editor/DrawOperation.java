package mindustry.editor;

import mindustry.annotations.Annotations.*;
import arc.struct.LongSeq;
import mindustry.game.Team;
import mindustry.gen.TileOp;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.content;

public class DrawOperation{
    private MapEditor editor;
    private LongSeq array = new LongSeq();

    public DrawOperation(MapEditor editor) {
        this.editor = editor;
    }

    public boolean isEmpty(){
        return array.isEmpty();
    }

    public void addOperation(long op){
        array.add(op);
    }

    public void undo(){
        for(int i = array.size - 1; i >= 0; i--){
            updateTile(i);
        }
    }

    public void redo(){
        for(int i = 0; i < array.size; i++){
            updateTile(i);
        }
    }

    private void updateTile(int i) {
        long l = array.get(i);
        array.set(i, TileOp.get(TileOp.x(l), TileOp.y(l), TileOp.type(l), getTile(editor.tile(TileOp.x(l), TileOp.y(l)), TileOp.type(l))));
        setTile(editor.tile(TileOp.x(l), TileOp.y(l)), TileOp.type(l), TileOp.value(l));
    }

    short getTile(Tile tile, byte type){
        if(type == OpType.floor.ordinal()){
            return tile.floorID();
        }else if(type == OpType.block.ordinal()){
            return tile.blockID();
        }else if(type == OpType.rotation.ordinal()){
            return tile.build == null ? 0 : (byte)tile.build.rotation;
        }else if(type == OpType.team.ordinal()){
            return (byte)tile.getTeamID();
        }else if(type == OpType.overlay.ordinal()){
            return tile.overlayID();
        }
        throw new IllegalArgumentException("Invalid type.");
    }

    void setTile(Tile tile, byte type, short to){
        editor.load(() -> {
            if(type == OpType.floor.ordinal()){
                tile.setFloor((Floor)content.block(to));
            }else if(type == OpType.block.ordinal()){
                Block block = content.block(to);
                tile.setBlock(block, tile.team(), tile.build == null ? 0 : tile.build.rotation);
            }else if(type == OpType.rotation.ordinal()){
                if(tile.build != null) tile.build.rotation = to;
            }else if(type == OpType.team.ordinal()){
                tile.setTeam(Team.get(to));
            }else if(type == OpType.overlay.ordinal()){
                tile.setOverlayID(to);
            }
        });
        editor.renderer().updatePoint(tile.x, tile.y);
    }

    @Struct
    class TileOpStruct{
        short x;
        short y;
        byte type;
        short value;
    }

    public enum OpType{
        floor,
        block,
        rotation,
        team,
        overlay
    }
}
