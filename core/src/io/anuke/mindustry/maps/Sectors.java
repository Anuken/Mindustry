package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.GridMap;

import static io.anuke.mindustry.Vars.headless;

public class Sectors{
    private static final int sectorSize = 256;
    private static final int sectorImageSize = 8;
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

        for(GridPoint2 point : Geometry.d4){
            createSector(sector.x + point.x, sector.y + point.y);
        }
    }

    /**Creates a sector at a location if it is not present, but does not unlock it.*/
    public void createSector(int x, int y){
        if(grid.containsKey(x, y)) return;

        Sector sector = new Sector();
        sector.x = (short)x;
        sector.y = (short)y;
        sector.unlocked = false;
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

        int worldX = sector.x * sectorSize;
        int worldY = sector.y * sectorSize;
        for(int x = worldX; x < (worldX + sectorSize); x++){
            for(int y = worldY; y < (worldY + sectorSize); y++){

            }
        }

        sector.texture = new Texture(pixmap);
        pixmap.dispose();
    }
}
