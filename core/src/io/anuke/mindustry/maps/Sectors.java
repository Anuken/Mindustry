package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.maps.generation.WorldGenerator.GenResult;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.GridMap;

import static io.anuke.mindustry.Vars.*;

public class Sectors{
    private static final int sectorImageSize = 16;

    private GridMap<Sector> grid = new GridMap<>();

    public Sectors(){

    }

    /**If a sector is not yet unlocked, returns null.*/
    public Sector get(int x, int y){
        return grid.get(x, y);
    }

    /**Unlocks a sector. This shows nearby sectors.*/
    public void unlockSector(int x, int y){
        createSector(x, y);
        Sector sector = get(x, y);
        sector.unlocked = true;
        if(sector.texture == null) createTexture(sector);

        //TODO fix
        for(GridPoint2 point : Geometry.d4){
        //    createSector(sector.x + point.x, sector.y + point.y);
        }
    }

    /**Creates a sector at a location if it is not present, but does not unlock it.*/
    public void createSector(int x, int y){
        if(grid.containsKey(x, y)) return;

        Sector sector = new Sector();
        sector.x = (short)x;
        sector.y = (short)y;
        sector.unlocked = false;
        grid.put(x, y, sector);
    }

    public void load(){
        Array<Sector> out = Settings.getJson("sectors", Array.class);

        for(Sector sector : out){
            createTexture(sector);
            grid.put(sector.x, sector.y, sector);
        }
    }

    public void save(){
        Array<Sector> out = new Array<>();

        for(Sector sector : grid.values()){
            out.add(sector);
        }

        Settings.putJson("sectors", out);
        Settings.save();
    }

    private void createTexture(Sector sector){
        if(headless) return; //obviously not created or needed on server

        Pixmap pixmap = new Pixmap(sectorImageSize, sectorImageSize, Format.RGBA8888);

        for(int x = 0; x < sectorImageSize; x++){
            for(int y = 0; y < sectorImageSize; y++){
                int toX = x * sectorSize / sectorImageSize;
                int toY = y * sectorSize / sectorImageSize;

                GenResult result = world.generator().generateTile(sector.x, sector.y, toX, toY);

                int color = Color.rgba8888(result.floor.minimapColor);
                pixmap.drawPixel(x, sectorImageSize - 1 - y, color);
            }
        }

        sector.texture = new Texture(pixmap);
        pixmap.dispose();
    }
}
