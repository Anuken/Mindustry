package mindustry.editor;

import arc.input.KeyCode;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Pos;
import mindustry.world.Tile;


import arc.struct.Array;


public class TileCopy{
    public Array<Array<Tile>> data = new Array<>();
    public Array<KeyCode> supported = new Array<>();
    int x, y;

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
    private void swapTerrain(Tile t, Tile other) {
        Block temp = other.floor();
        other.setFloor(t.floor());
        t.setFloor(temp.asFloor());
        temp = other.overlay();
        other.setOverlay(t.overlay());
        t.setOverlay(temp);
    }

    public void flipVertical() {
        for(int y = 0; y < getSizeY() / 2; y++){
            for(int x = 0; x < getSizeX(); x++){
                Tile temp = data.get(y).get(x);
                int opposite = getSizeY()-1-y;
                data.get(y).set(x, data.get(opposite).get(x));
                data.get(opposite).set(x, temp);

            }
        }
        for(int y = 0; y < getSizeY(); y++){
            for(int x = 0; x < getSizeX(); x++){
                Tile t = data.get(y).get(x);
                if(t.block().posConfig){
                    int conf = t.entity.config();
                    t.configure(Pos.get(Pos.x(conf),t.y - Pos.y(conf) + t.y));
                }
                if(t.block().size % 2 == 0){
                    if(y == 0) {
                        t.setBlock(Blocks.air);
                        continue;
                    }
                    Tile other = data.get(y - 1).get(x);
                    //it has to be done this wey,just swapping blocks is unstable
                    swapTerrain(t, other);

                    data.get(y - 1).set(x, t);
                    data.get(y).set(x, other);

                }
            }
        }
        rotateAllTiles(2,false,false);
    }



    public void flipHorizontal() {
        for(int y = 0; y < getSizeY(); y++){
            for(int x = 0; x < getSizeX() / 2; x++){
                int opposite = getSizeX()-1-x;
                Tile temp = data.get(y).get(x);
                data.get(y).set(x,data.get(y).get(opposite));
                data.get(y).set(opposite,temp);
            }
        }
        for(int y = 0; y < getSizeY(); y++){
            for(int x = 0; x < getSizeX(); x++){
                Tile t = data.get(y).get(x);
                if(t.block().posConfig){
                    int conf = t.entity.config();
                    t.configure(Pos.get(t.x - Pos.x(conf) + t.x,Pos.y(conf)));

                }
                if(t.block().size % 2 == 0){
                    if(x == 0) {
                        t.setBlock(Blocks.air);
                        continue;
                    }
                    Tile other = data.get(y).get(x - 1);
                    swapTerrain(t, other);
                    data.get(y).set(x - 1,t);
                    data.get(y).set(x,other);
                }
            }
        }
        rotateAllTiles(2,true,false);
    }

    private void flipDiagonal(){
        Array<Array<Tile>> newData = new Array<>();
        for(int x = 0 ;x < getSizeX();x++) {
            newData.add(new Array<>());
            for (int y = 0; y < getSizeY(); y++) {
                Tile t=data.get(y).get(x);
                if(t.block().posConfig){
                    int conf = t.entity.config();
                    t.configure(Pos.get(Pos.y(conf) - t.y + t.x, Pos.x(conf) - t.x + t.y));


                }
                newData.get(x).add(t);
            }
        }
        data = newData;

    }

    private void rotateAllTiles(int add,boolean h,boolean d) {
        for(int y = 0; y < getSizeY(); y++){
            for(int x = 0; x < getSizeX(); x++){
                Tile tile = data.get(y).get(x);
                if(d || (h && tile.rotation() % 2 == 0) || (!h && !(tile.rotation() % 2 == 0))) {
                    tile.rotation(tile.rotation() + add);
                }

            }
        }
    }

    public void rotate() {
        flipHorizontal();
        flipDiagonal();
        rotateAllTiles(2, false, false);
        rotateAllTiles(1, false, true);
    }




}
