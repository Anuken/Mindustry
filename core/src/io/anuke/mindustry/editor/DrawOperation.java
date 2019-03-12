package io.anuke.mindustry.editor;

import io.anuke.annotations.Annotations.Struct;
import io.anuke.arc.collection.LongArray;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.content;

public class DrawOperation{
    private LongArray array = new LongArray();

    public boolean isEmpty(){
        return array.isEmpty();
    }

    public void addOperation(long op){
        array.add(op);
    }

    public void undo(MapEditor editor){
        for(int i = array.size - 1; i >= 0; i--){
            long l = array.get(i);
            set(editor, editor.tile(TileOp.x(l), TileOp.y(l)), TileOp.type(l), TileOp.from(l));
        }
    }

    public void redo(MapEditor editor){
        for(int i = 0; i < array.size; i++){
            long l = array.get(i);
            set(editor, editor.tile(TileOp.x(l), TileOp.y(l)), TileOp.type(l), TileOp.to(l));
        }
    }

    void set(MapEditor editor, Tile tile, byte type, byte to){
        editor.load(() -> {
            if(type == OpType.floor.ordinal()){
                tile.setFloor((Floor)content.block(to));
            }else if(type == OpType.block.ordinal()){
                tile.setBlock(content.block(to));
            }else if(type == OpType.rotation.ordinal()){
                tile.setRotation(to);
            }else if(type == OpType.team.ordinal()){
                tile.setTeam(Team.all[to]);
            }else if(type == OpType.ore.ordinal()){
                tile.setOreByte(to);
            }
        });
        editor.renderer().updatePoint(tile.x, tile.y);
    }

    @Struct
    class TileOpStruct{
        short x;
        short y;
        byte type;
        byte from;
        byte to;
    }

    public enum OpType{
        floor,
        block,
        rotation,
        team,
        ore
    }
}
