package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.ucore.util.Bits;

public class DrawOperation{
    /**
     * Data to apply operation to.
     */
    private MapTileData data;
    /**
     * List of per-tile operations that occurred.
     */
    private Array<TileOperation> operations = new Array<>();
    /**
     * Checks for duplicate operations, useful for brushes.
     */
    private IntSet checks = new IntSet();

    public DrawOperation(MapTileData data){
        this.data = data;
    }

    public boolean isEmpty(){
        return operations.size == 0;
    }

    public boolean checkDuplicate(short x, short y){
        int i = Bits.packInt(x, y);
        if(checks.contains(i)) return true;

        checks.add(i);
        return false;
    }

    public void addOperation(TileOperation op){
        operations.add(op);
    }

    public void undo(MapEditor editor){
        for(int i = operations.size - 1; i >= 0; i--){
            TileOperation op = operations.get(i);
            data.position(op.x, op.y);
            data.write(op.from);
            editor.renderer().updatePoint(op.x, op.y);
        }
    }

    public void redo(MapEditor editor){
        for(TileOperation op : operations){
            data.position(op.x, op.y);
            data.write(op.to);
            editor.renderer().updatePoint(op.x, op.y);
        }
    }

    public static class TileOperation{
        public short x, y;
        public TileDataMarker from;
        public TileDataMarker to;

        public TileOperation(short x, short y, TileDataMarker from, TileDataMarker to){
            this.x = x;
            this.y = y;
            this.from = from;
            this.to = to;
        }
    }
}
