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
    opOverlay = 4,
    opData = 5, //overlay/floor/data field
    opDataExtra = 6; //extraData

    private LongSeq array = new LongSeq();

    public boolean isEmpty(){
        return array.isEmpty();
    }

    public int size(){
        return array.size;
    }

    public void remove(int amount){
        array.setSize(Math.max(0, array.size - amount));
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

    int getTile(Tile tile, int type){
        return switch(type){
            case opFloor -> tile.floorID();
            case opOverlay -> tile.overlayID();
            case opBlock -> tile.blockID();
            case opRotation -> tile.build == null ? 0 : (byte)tile.build.rotation;
            case opTeam -> tile.getTeamID();
            case opData -> TileOpData.get(tile.data, tile.floorData, tile.overlayData);
            case opDataExtra -> tile.extraData;
            default -> throw new IllegalArgumentException("Invalid type.");
        };
    }

    void setTile(Tile tile, int type, int to){
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
                case opData -> {
                    tile.data = TileOpData.data(to);
                    tile.floorData = TileOpData.floorData(to);
                    tile.overlayData = TileOpData.overlayData(to);
                }
                case opDataExtra -> tile.extraData = to;
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
        @StructField(14)
        int x;
        @StructField(14)
        int y;
        @StructField(3)
        int type;
        int value;
    }

    @Struct
    class TileOpDataStruct{
        byte data, floorData, overlayData;
    }
}
