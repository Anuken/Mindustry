package mindustry.type;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.ctype.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;

import static mindustry.Vars.*;

public class Planet extends UnlockableContent{
    /** Default spacing between planet orbits in world units. */
    private static final float orbitSpacing = 11f;
    /** intersect() temp var. */
    private static final Vec3 intersectResult = new Vec3();
    /** Mesh used for rendering. Created on load() - will be null on the server! */
    public @Nullable PlanetMesh mesh;
    /** Position in global coordinates. Will be 0,0,0 until the Universe updates it. */
    public Vec3 position = new Vec3();
    /** Grid used for the sectors on the planet. Null if this planet can't be landed on. */
    public @Nullable PlanetGrid grid;
    /** Generator that will make the planet. Can be null for planets that don't need to be landed on. */
    public @Nullable PlanetGenerator generator;
    /** Array of sectors; directly maps to tiles in the grid. */
    public Seq<Sector> sectors;
    /** Radius of this planet's sphere. Does not take into account satellites. */
    public float radius;
    /** Atmosphere radius adjustment parameters. */
    public float atmosphereRadIn = 0, atmosphereRadOut = 0.3f;
    /** Frustrum sphere clip radius. */
    public float clipRadius = -1f;
    /** Orbital radius around the sun. Do not change unless you know exactly what you are doing.*/
    public float orbitRadius;
    /** Total radius of this planet and all its children. */
    public float totalRadius;
    /** Time for the planet to orbit around the sun once, in seconds. One year. */
    public float orbitTime;
    /** Time for the planet to perform a full revolution, in seconds. One day. */
    public float rotateTime = 24f * 60f;
    /** Approx. radius of one sector. */
    public float sectorApproxRadius;
    /** Whether this planet is tidally locked relative to its parent - see https://en.wikipedia.org/wiki/Tidal_locking */
    public boolean tidalLock = false;
    /** Whether this planet is listed in the planet access UI. **/
    public boolean accessible = true;
    /** The default starting sector displayed to the map dialog. */
    public int startSector = 0;
    /** Whether the bloom render effect is enabled. */
    public boolean bloom = false;
    /** Whether this planet is displayed. */
    public boolean visible = true;
    /** Tint of clouds displayed when landing. */
    public Color landCloudColor = new Color(1f, 1f, 1f, 0.5f);
    /** For suns, this is the color that shines on other planets. Does nothing for children. */
    public Color lightColor = Color.white.cpy();
    /** Atmosphere tint for landable planets. */
    public Color atmosphereColor = new Color(0.3f, 0.7f, 1.0f);
    /** Whether this planet has an atmosphere. */
    public boolean hasAtmosphere = true;
    /** Parent body that this planet orbits around. If null, this planet is considered to be in the middle of the solar system.*/
    public @Nullable Planet parent;
    /** The root parent of the whole solar system this planet is in. */
    public Planet solarSystem;
    /** All planets orbiting this one, in ascending order of radius. */
    public Seq<Planet> children = new Seq<>();
    /** Satellites orbiting this planet. */
    public Seq<Satellite> satellites = new Seq<>();
    /** Loads the mesh. Clientside only. Defaults to a boring sphere mesh. */
    protected Prov<PlanetMesh> meshLoader = () -> new ShaderSphereMesh(this, Shaders.unlit, 2);

    public Planet(String name, Planet parent, int sectorSize, float radius){
        super(name);

        this.radius = radius;
        this.parent = parent;

        if(sectorSize > 0){
            grid = PlanetGrid.create(sectorSize);

            sectors = new Seq<>(grid.tiles.length);
            for(int i = 0; i < grid.tiles.length; i++){
                sectors.add(new Sector(this, grid.tiles[i]));
            }

            sectorApproxRadius = sectors.first().tile.v.dst(sectors.first().tile.corners[0].v);
        }else{
            sectors = new Seq<>();
        }

        //total radius is initially just the radius
        totalRadius += radius;

        //get orbit radius by extending past the parent's total radius
        orbitRadius = parent == null ? 0f : (parent.totalRadius + orbitSpacing + totalRadius);

        //orbit time is based on radius [kepler's third law]
        orbitTime = Mathf.pow(orbitRadius, 1.5f) * 1000;

        //add this planet to list of children and update parent's radius
        if(parent != null){
            parent.children.add(this);
            parent.updateTotalRadius();
        }

        //calculate solar system
        for(solarSystem = this; solarSystem.parent != null; solarSystem = solarSystem.parent);
    }

    public @Nullable Sector getLastSector(){
        if(sectors.isEmpty()){
            return null;
        }
        return sectors.get(Math.min(Core.settings.getInt(name + "-last-sector", startSector), sectors.size - 1));
    }

    public void setLastSector(Sector sector){
        Core.settings.put(name + "-last-sector", sector.id);
    }

    public void preset(int index, SectorPreset preset){
        sectors.get(index).preset = preset;
    }

    /** @return whether this planet has a sector grid to select. */
    public boolean hasGrid(){
        return grid != null && generator != null && sectors.size > 0;
    }

    public void updateTotalRadius(){
        totalRadius = radius;
        for(Planet planet : children){
            //max with highest outer bound planet
            totalRadius = Math.max(totalRadius, planet.orbitRadius + planet.totalRadius);
        }
    }

    public Vec3 getLightNormal(){
        return Tmp.v31.set(solarSystem.position).sub(position).nor();
    }

    /** Calculates orbital rotation based on universe time.*/
    public float getOrbitAngle(){
        //applies random offset to prevent planets from starting out in a line
        float offset = Mathf.randomSeed(id, 360);
        return (offset + universe.secondsf() / (orbitTime / 360f)) % 360f;
    }

    /** Calulates rotation on own axis based on universe time.*/
    public float getRotation(){
        //tidally locked planets always face toward parents
        if(tidalLock){
            return -getOrbitAngle() + 90;
        }
        //random offset for more variability
        float offset = Mathf.randomSeed(id+1, 360);
        return (offset + universe.secondsf() / (rotateTime / 360f)) % 360f;
    }

    /** Adds this planet's offset relative to its parent to the vector. Used for calculating world positions. */
    public Vec3 addParentOffset(Vec3 in){
        //planets with no parents are at the center, so they appear at 0,0
        if(parent == null || Mathf.zero(orbitRadius)){
            return in;
        }

        float angle = getOrbitAngle();
        return in.add(Angles.trnsx(angle, orbitRadius), 0, Angles.trnsy(angle, orbitRadius));
    }

    /** Gets the absolute world position of this planet, taking into account all parents. O(n) complexity.*/
    public Vec3 getWorldPosition(Vec3 in){
        in.setZero();
        for(Planet current = this; current != null; current = current.parent){
            current.addParentOffset(in);
        }
        return in;
    }

    /** Updates wave coverage of bases. */
    public void updateBaseCoverage(){
        for(Sector sector : sectors){
            float sum = 1f;
            for(Sector other : sector.near()){
                if(other.generateEnemyBase){
                    sum += 0.9f;
                }
            }

            if(sector.hasEnemyBase()){
                sum += 0.88f;
            }

            sector.threat = sector.preset == null ? Math.min(sum / 5f, 1.2f) : Mathf.clamp(sector.preset.difficulty / 10f);
        }
    }

    /** @return the supplied matrix with transformation applied. */
    public Mat3D getTransform(Mat3D mat){
        return mat.setToTranslation(position).rotate(Vec3.Y, getRotation());
    }

    /** Regenerates the planet mesh. For debugging only. */
    public void reloadMesh(){
        if(mesh != null){
            mesh.dispose();
        }
        mesh = meshLoader.get();
    }

    @Override
    public void load(){
        super.load();

        mesh = meshLoader.get();
    }

    @Override
    public void init(){

        for(Sector sector : sectors){
            sector.loadInfo();
        }

        if(generator != null){
            Noise.setSeed(id + 1);

            for(Sector sector : sectors){
                generator.generateSector(sector);
            }

            updateBaseCoverage();
        }

        clipRadius = Math.max(clipRadius, radius + atmosphereRadOut + 0.5f);
    }

    @Override
    public void dispose(){
        if(mesh != null){
            mesh.dispose();
            mesh = null;
        }
    }

    /** Gets a sector a tile position. */
    public Sector getSector(Ptile tile){
        return sectors.get(tile.id);
    }

    /** @return the sector that is hit by this ray, or null if nothing intersects it. */
    public @Nullable Sector getSector(Ray ray){
        return getSector(ray, radius);
    }

    /** @return the sector that is hit by this ray, or null if nothing intersects it. */
    public @Nullable Sector getSector(Ray ray, float radius){
        Vec3 vec = intersect(ray, radius);
        if(vec == null) return null;
        vec.sub(position).rotate(Vec3.Y, getRotation());
        return sectors.min(t -> t.tile.v.dst2(vec));
    }

    /** @return the sector that is hit by this ray, or null if nothing intersects it. */
    public @Nullable Vec3 intersect(Ray ray, float radius){
        boolean found = Intersector3D.intersectRaySphere(ray, position, radius, intersectResult);
        if(!found) return null;
        return intersectResult;
    }

    /** Planets cannot be viewed in the database dialog. */
    @Override
    public boolean isHidden(){
        return true;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.planet;
    }

    public boolean visible(){
        return visible;
    }

    public void draw(Mat3D projection, Mat3D transform){
        mesh.render(projection, transform);
    }
}
