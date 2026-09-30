package mindustry.world;

import arc.util.*;
import mindustry.core.*;
import mindustry.io.*;
import mindustry.type.*;

public class DefaultWorldContext extends SaveLoadContext{
    protected final @Nullable Sector sector;
    protected final GameState state;

    public DefaultWorldContext(GameState state){
        this(state, null);
    }

    public DefaultWorldContext(GameState state, @Nullable Sector sector){
        this.state = state;
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