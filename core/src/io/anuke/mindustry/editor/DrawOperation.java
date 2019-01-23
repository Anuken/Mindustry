package io.anuke.mindustry.editor;

import io.anuke.arc.collection.LongArray;
import io.anuke.arc.util.Pack;

public class DrawOperation{
    private LongArray array = new LongArray();

    public boolean isEmpty(){
        return array.isEmpty();
    }

    public void addOperation(int xy, byte type, byte from, byte to){
        array.add(Pack.longInt(xy, Pack.intBytes(type, from, to, (byte)0)));
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
}
