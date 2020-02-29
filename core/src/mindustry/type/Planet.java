package mindustry.type;

import arc.files.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;
import mindustry.type.Sector.*;

import static mindustry.Vars.universe;

public class Planet extends UnlockableContent{
    /** Default spacing between planet orbits in world units. */
    private static final float orbitSpacing = 6f;
    /** Mesh used for rendering. Created on load() - will be null on the server! */
    public GenericMesh mesh;
    /** Grid used for the sectors on the planet. */
    public @NonNull PlanetGrid grid;
    /** Generator that will make the planet. */
    public @NonNull PlanetGenerator generator;
    /** Array of sectors; directly maps to tiles in the grid. */
    public @NonNull Array<Sector> sectors;
    /** Detail in divisions. Must be between 1 and 10. 6 is a good number for this.*/
    public int detail = 3;
    /** Radius of this planet's sphere. Does not take into account sattelites. */
    public float radius;
    /** Orbital radius around the sun. Do not change unless you know exactly what you are doing.*/
    public float orbitRadius;
    /** Total radius of this planet and all its children. */
    public float totalRadius;
    /** Time for the planet to orbit around the sun once, in seconds. One year. */
    public float orbitTime;
    /** Time for the planet to perform a full revolution, in seconds. One day. */
    public float rotateTime = 24f * 60f;
    /** Whether this planet is tidally locked relative to its parent - see https://en.wikipedia.org/wiki/Tidal_locking */
    public boolean tidalLock = false;
    /** Parent body that this planet orbits around. If null, this planet is considered to be in the middle of the solar system.*/
    public @Nullable Planet parent;
    /** All planets orbiting this one, in ascending order of radius. */
    public Array<Planet> children = new Array<>();

    public Planet(String name, Planet parent, int size, float radius){
        super(name);

        this.radius = radius;
        this.parent = parent;

        grid = PlanetGrid.create(size);

        sectors = new Array<>(grid.tiles.length);
        for(int i = 0; i < grid.tiles.length; i++){
            sectors.add(new Sector(this, grid.tiles[i], new SectorData()));
        }

        //get orbit radius by extending past the parent's total radius
        orbitRadius = parent == null ? 0f : (parent.totalRadius + orbitSpacing + radius);

        //orbit time is based on radius [kepler's third law]
        orbitTime = Mathf.pow(orbitRadius, 1.5f) * 1000;

        //add this planet to list of children and update parent's radius
        if(parent != null){
            parent.children.add(this);
            parent.updateTotalRadius();
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
            Log.err("Planet {0} is missing its data file.", name);
        }
    }

    public void updateTotalRadius(){
        totalRadius = radius;
        for(Planet planet : children){
            //max with highest outer bound planet
            totalRadius = Math.max(totalRadius, planet.orbitRadius + planet.totalRadius);
        }
    }

    /** Calculates orbital rotation based on universe time.*/
    public float getOrbitAngle(){
        //applies random offset to prevent planets from starting out in a line
        float offset = Mathf.randomSeed(id, 360);
        return (offset + universe.seconds() / (orbitTime / 360f)) % 360f;
    }

    /** Calulates rotation on own axis based on universe time.*/
    public float getRotation(){
        //tidally locked planets always face toward parents
        if(tidalLock){
            return getOrbitAngle();
        }
        //random offset for more variability
        float offset = Mathf.randomSeed(id+1, 360);
        return (offset + universe.seconds() / (rotateTime / 360f)) % 360f;
    }

    /** Adds this planet's offset relative to its parent to the vector. Used for calculating world positions. */
    public Vec3 addParentOffset(Vec3 in){
        //planets with no parents are at the center, so they appear at 0,0
        if(parent == null || Mathf.zero(orbitRadius)){
            return in;
        }

        float angle = getOrbitAngle();
        return in.add(Angles.trnsx(angle, orbitRadius), Angles.trnsy(angle, orbitRadius), 0f);
    }

    /** Gets the absolute world position of this planet, taking into account all parents. O(n) complexity.*/
    public Vec3 getWorldPosition(Vec3 in){
        in.setZero();
        for(Planet current = this; current != null; current = current.parent){
            current.addParentOffset(in);
        }
        return in;
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
