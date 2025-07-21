package mindustry.editor;

import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class DrawOperation{
    static final byte
    opFloor = 0,
    opBlock = 1,
    opRotation = 2,
    opTeam = 3,
    opOverlay = 4;

    private LongSeq array = new LongSeq();

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

    private void updateTile(int i){
        long l = array.get(i);
        array.set(i, TileOp.get(TileOp.x(l), TileOp.y(l), TileOp.type(l), getTile(editor.tile(TileOp.x(l), TileOp.y(l)), TileOp.type(l))));
        setTile(editor.tile(TileOp.x(l), TileOp.y(l)), TileOp.type(l), TileOp.value(l));
    }

    short getTile(Tile tile, byte type){
        return switch(type){
            case opFloor -> tile.floorID();
            case opOverlay -> tile.overlayID();
            case opBlock -> tile.blockID();
            case opRotation -> tile.build == null ? 0 : (byte)tile.build.rotation;
            case opTeam -> (byte)tile.getTeamID();
            default -> throw new IllegalArgumentException("Invalid type.");
        };
    }

    void setTile(Tile tile, byte type, short to){
        if(type == opBlock || type == opTeam || type == opRotation){
            tile.getLinkedTiles(t -> {
                editor.renderer.updateBlock(t);
                editor.renderer.updateStatic(t.x, t.y);
            });
        }else{
            editor.renderer.updateStatic(tile.x, tile.y);
        }

        editor.load(() -> {
            switch(type){
                case opFloor -> {
                    if(content.block(to) instanceof Floor floor){
                        tile.setFloor(floor);
                    }
                }
                case opOverlay -> {
                    if(content.block(to) instanceof Floor floor){
                        tile.setOverlay(floor);
                    }
                }
                case opBlock -> {
                    Block block = content.block(to);
                    tile.setBlock(block, tile.team(), tile.build == null ? 0 : tile.build.rotation);
                    if(tile.build != null){
                        tile.build.enabled = true;
                    }
                }
                case opRotation -> {
                    if(tile.build != null) tile.build.rotation = to;
                }
                case opTeam -> tile.setTeam(Team.get(to));
            }
        });

        if(type == opBlock || type == opTeam || type == opRotation){
            tile.getLinkedTiles(t -> {
                editor.renderer.updateBlock(t);
                editor.renderer.updateStatic(t.x, t.y);
            });
        }
    }

    @Struct
    class TileOpStruct{
        short x;
        short y;
        byte type;
        short value;
    }
}
