package mindustry.editor;

import mindustry.world.Tile;

import java.util.ArrayList;

public class TileCopy{
    public ArrayList<ArrayList<Tile>> data=new ArrayList<>();
    int x,y;

    MapEditor editor;
    MapView view;

    public TileCopy(MapView view,MapEditor editor){
        this.view=view;
        this.editor=editor;
    }

    public int getSizeX(){
        if(getSizeY()==0) return 0;

        return data.get(0).size();
    }
    public int getSizeY(){
        return data.size();
    }

    public boolean inSelection(int sx,int sy){
        return sx>=x && sx<=x+getSizeX() && sy>=y && sy<=y+getSizeY();
    }
}
