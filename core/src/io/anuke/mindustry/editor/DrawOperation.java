package io.anuke.mindustry.editor;

import io.anuke.annotations.Annotations.Struct;
import io.anuke.arc.collection.LongArray;

public class DrawOperation{
    private LongArray array = new LongArray();

    public boolean isEmpty(){
        return array.isEmpty();
    }

    public void addOperation(long op){
        array.add(op);
    }

    public void undo(MapEditor editor){
        for(int i = 0; i < array.size; i++){
            long l = array.get(i);
        }
    }

    public void redo(MapEditor editor){
        for(int i = 0; i < array.size; i++){
            long l = array.get(i);
        }
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
        team
    }
}
