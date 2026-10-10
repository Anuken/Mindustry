package mindustry.world;

import arc.util.*;
import mindustry.io.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class DefaultWorldContext extends SaveLoadContext{
    protected final @Nullable Sector sector;

    public DefaultWorldContext(){
        this(null);
    }

    public DefaultWorldContext(@Nullable Sector sector){
        this.sector = sector;
    }

    @Override
    public Tile tile(int index){
        return state.world.geti(index);
    }

    @Override
    public void resize(int width, int height){
        state.resizeWorld(width, height);
    }

    @Override
    public Tile create(int x, int y, int floorID, int overlayID, int wallID){
        Tile tile = new Tile(x, y, floorID, overlayID, wallID);
        state.world.set(x, y, tile);
        return tile;
    }

    @Override
    public boolean isGenerating(){
        return state.generating;
    }

    @Override
    public void begin(){
        state.beginMapLoad();
    }

    @Override
    public void end(){
        state.endMapLoad();
    }

    @Nullable
    @Override
    public Sector getSector(){
        return sector;
    }
}