package mindustry.editor;

import arc.input.KeyCode;
import mindustry.world.Tile;

import arc.struct.Array;

public class TileCopy{
    public Array<Array<Tile>> data = new Array<>();
    public Array<KeyCode> supported = new Array<>();
    int x,y;

    MapEditor editor;
    MapView view;

    public TileCopy(MapView view,MapEditor editor){
        this.view = view;
        this.editor = editor;
        supported.add(
                KeyCode.R,
                KeyCode.X,
                KeyCode.Y
        );
    }

    public int getSizeX(){
        if(getSizeY() == 0) return 0;

        return data.get(0).size;
    }
    public int getSizeY(){
        return data.size;
    }

    public boolean inSelection(int sx,int sy){
        return sx >= x && sx <= x + getSizeX() && sy >= y && sy <= y + getSizeY();
    }





}
