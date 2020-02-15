package mindustry.type;

import arc.files.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.graphics.*;
import mindustry.graphics.PlanetGrid.*;
import mindustry.maps.planet.*;
import mindustry.type.Sector.*;

public class Planet extends UnlockableContent{
    /** Mesh used for rendering. Created on load() - will be null on the server! */
    public PlanetMesh mesh;
    /** Grid used for the sectors on the planet. */
    public @NonNull PlanetGrid grid;
    /** Generator that will make the planet. */
    public @NonNull PlanetGenerator generator;
    /** Array of sectors; directly maps to tiles in the grid. */
    public @NonNull Array<Sector> sectors;
    /** Detail in divisions. Must be between 1 and 10. 6 is a good number for this.*/
    public int detail = 3;
    /** Size in terms of divisions. This only controls the amount of sectors on the planet, not the visuals. */
    public int size;
    /** Radius of the mesh/sphere. */
    public float radius = 1f;

    public Planet(String name, int size){
        super(name);

        this.size = 3;

        grid = PlanetGrid.newGrid(size);

        sectors = new Array<>(grid.tiles.length);
        for(int i = 0; i < grid.tiles.length; i++){
            //TODO load sector data
            sectors.add(new Sector(this, grid.tiles[i], new SectorData()));
        }

        //read data
        Fi data = Vars.tree.get("planets/" + name + ".dat");
        if(data.exists()){
            try(Reads read = data.reads()){
                short dsize = read.s();
                for(int i = 0; i < dsize; i++){
                    sectors.get(i).data.read(read);
                }
            }
        }else{
            //TODO crash instead - this is a critical error!
            Log.err("Planet {0} is missing its data file.");
        }
    }

    @Override
    public void load(){
        mesh = new PlanetMesh(detail, generator);
    }

    /** Gets a sector a tile position. */
    public Sector getSector(Ptile tile){
        return sectors.get(tile.id);
    }

    /** @return the sector that is hit by this ray, or null if nothing intersects it.
     * @param center the center of this planet in 3D space, usually (0,0,0). */
    public @Nullable Sector getSector(Vec3 center, Ray ray){
        boolean found = Intersector3D.intersectRaySphere(ray, center, radius, Tmp.v33);
        if(!found) return null;
        //TODO fix O(N) search
        return sectors.min(t -> t.tile.v.dst(Tmp.v33));
    }

    /** Planets cannot be viewed in the database dialog. */
    @Override
    public boolean isHidden(){
        return true;
    }

    @Override
    public void displayInfo(Table table){

    }

    @Override
    public ContentType getContentType(){
        return ContentType.planet;
    }
}
