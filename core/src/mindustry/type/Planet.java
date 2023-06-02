package mindustry.type;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.EventType.ContentInitEvent;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.graphics.g3d.PlanetRenderer.*;

public class Planet extends UnlockableContent{
    /** intersect() temp var. */
    private static final Vec3 intersectResult = new Vec3();
    /** drawSectors() temp matrix. */
    private static final Mat3D mat = new Mat3D();
    /** drawArc() temp curve points. */
    private static final Seq<Vec3> points = new Seq<>();

    /** Mesh used for rendering. Created on load() - will be null on the server! */
    public @Nullable GenericMesh mesh;
    /** Mesh used for rendering planet clouds. Null if no clouds are present. */
    public @Nullable GenericMesh cloudMesh;
    /** Mesh used for rendering planet grid outlines. Null on server or if {@link #grid} is null. */
    public @Nullable Mesh gridMesh;
    /** Position in global coordinates. Will be 0,0,0 until the Universe updates it. */
    public Vec3 position = new Vec3();
    /** Grid used for the sectors on the planet. Null if this planet can't be landed on. */
    public @Nullable PlanetGrid grid;
    /** Generator that will make the planet. Can be null for planets that don't need to be landed on. */
    public @Nullable PlanetGenerator generator;
    /** Array of sectors; directly maps to tiles in the grid. */
    public Seq<Sector> sectors = new Seq<>();
    /** Default spacing between planet orbits in world units. This is defined per-parent! */
    public float orbitSpacing = 12f;
    /** Radius of this planet's sphere. Does not take into account satellites. */
    public float radius;
    /** Camera radius offset. */
    public float camRadius;
    /** Minimum camera zoom value. */
    public float minZoom = 0.5f;
    /** Whether to draw the orbital circle. */
    public boolean drawOrbit = true;
    /** Atmosphere radius adjustment parameters. */
    public float atmosphereRadIn = 0, atmosphereRadOut = 0.3f;
    /** Frustum sphere clip radius. */
    public float clipRadius = -1f;
    /** Orbital radius around the sun. Do not change unless you know exactly what you are doing.*/
    public float orbitRadius;
    /** Total radius of this planet and all its children. */
    public float totalRadius;
    /** Time for the planet to orbit around the sun once, in seconds. One year. */
    public float orbitTime;
    /** Time for the planet to perform a full revolution, in seconds. One day. */
    public float rotateTime = 24f * 60f;
    /** Random orbit angle offset to prevent planets from starting out in a line. */
    public float orbitOffset;
    /** Approx. radius of one sector. */
    public float sectorApproxRadius;
    /** Whether this planet is tidally locked relative to its parent - see https://en.wikipedia.org/wiki/Tidal_locking */
    public boolean tidalLock = false;
    /** Whether this planet is listed in the planet access UI. **/
    public boolean accessible = true;
    /** Environment flags for sectors on this planet. */
    public int defaultEnv = Env.terrestrial | Env.spores | Env.groundOil | Env.groundWater | Env.oxygen;
    /** Environment attributes. */
    public Attributes defaultAttributes = new Attributes();
    /** If true, a day/night cycle is simulated. */
    public boolean updateLighting = true;
    /** Day/night cycle parameters. */
    public float lightSrcFrom = 0f, lightSrcTo = 0.8f, lightDstFrom = 0.2f, lightDstTo = 1f;
    /** The default starting sector displayed to the map dialog. */
    public int startSector = 0;
    /** Seed for sector base generation on this planet. -1 to use a random one based on ID. */
    public int sectorSeed = -1;
    /** multiplier for core item capacity when launching */
    public float launchCapacityMultiplier = 0.25f;
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
    /** Icon for appearance in planet list. */
    public Color iconColor = Color.white.cpy();
    /** Whether this planet has an atmosphere. */
    public boolean hasAtmosphere = true;
    /** Whether to allow users to specify a custom launch schematic for this map. */
    public boolean allowLaunchSchematics = false;
    /** Whether to allow users to specify the resources they take to this map. */
    public boolean allowLaunchLoadout = false;
    /** Whether to allow sectors to simulate waves in the background. */
    public boolean allowWaveSimulation = false;
    /** Whether to simulate sector invasions from enemy bases. */
    public boolean allowSectorInvasion = false;
    /** If true, sectors saves are cleared when lost. */
    public boolean clearSectorOnLose = false;
    /** Multiplier for enemy rebuild speeds; only applied in campaign (not standard rules) */
    public float enemyBuildSpeedMultiplier = 1f;
    /** If true, enemy cores are replaced with spawnpoints on this planet (for invasions) */
    public boolean enemyCoreSpawnReplace = false;
    /** If true, blocks in the radius of the core will be removed and "built up" in a shockwave upon landing. */
    public boolean prebuildBase = true;
    /** If true, waves are created on sector loss. TODO remove. */
    public boolean allowWaves = false;
    /** If false, players are unable to land on this planet's numbered sectors. */
    public boolean allowLaunchToNumbered = true;
    /** Icon as displayed in the planet selection dialog. This is a string, as drawables are null at load time. */
    public String icon = "planet";
    /** Default core block for launching. */
    public Block defaultCore = Blocks.coreShard;
    /** Sets up rules on game load for any sector on this planet. */
    public Cons<Rules> ruleSetter = r -> {};
    /** Parent body that this planet orbits around. If null, this planet is considered to be in the middle of the solar system.*/
    public @Nullable Planet parent;
    /** The root parent of the whole solar system this planet is in. */
    public Planet solarSystem;
    /** All planets orbiting this one, in ascending order of radius. */
    public Seq<Planet> children = new Seq<>();
    /** Default root node shown when the tech tree is opened here. */
    public @Nullable TechNode techTree;
    /** TODO remove? Planets that can be launched to from this one. Made mutual in init(). */
    public Seq<Planet> launchCandidates = new Seq<>();
    /** Items not available on this planet. Left out for backwards compatibility. */
    public Seq<Item> hiddenItems = new Seq<>();
    /** The only items available on this planet, if defined. */
    public Seq<Item> itemWhitelist = new Seq<>();
    /** Content (usually planet-specific) that is unlocked upon landing here. */
    public Seq<UnlockableContent> unlockedOnLand = new Seq<>();
    /** Loads the mesh. Clientside only. Defaults to a boring sphere mesh. */
    public Prov<GenericMesh> meshLoader = () -> new ShaderSphereMesh(this, Shaders.unlit, 2), cloudMeshLoader = () -> null;
    /** Loads the planet grid outline mesh. Clientside only. */
    public Prov<Mesh> gridMeshLoader = () -> MeshBuilder.buildPlanetGrid(grid, outlineColor, outlineRad * radius);

    public Planet(String name, Planet parent, float radius){
        super(name);

        this.radius = radius;
        this.parent = parent;
        this.orbitOffset = Mathf.randomSeed(id + 1, 360);

        //total radius is initially just the radius
        totalRadius = radius;

        //get orbit radius by extending past the parent's total radius
        orbitRadius = parent == null ? 0f : (parent.totalRadius + parent.orbitSpacing + totalRadius);

        //orbit time is based on radius [kepler's third law]
        orbitTime = Mathf.pow(orbitRadius, 1.5f) * 1000;

        //add this planet to list of children and update parent's radius
        if(parent != null){
            parent.children.add(this);
            parent.updateTotalRadius();
        }

        //if an item whitelist exists, add everything else not in that whitelist to hidden items
        Events.on(ContentInitEvent.class, e -> {
            if(itemWhitelist.size > 0){
                hiddenItems.addAll(content.items().select(i -> !itemWhitelist.contains(i)));
            }
        });

        //calculate solar system
        for(solarSystem = this; solarSystem.parent != null; solarSystem = solarSystem.parent);
    }

    public Planet(String name, Planet parent, float radius, int sectorSize){
        this(name, parent, radius);

        if(sectorSize > 0){
            grid = PlanetGrid.create(sectorSize);

            sectors.ensureCapacity(grid.tiles.length);
            for(int i = 0; i < grid.tiles.length; i++){
                sectors.add(new Sector(this, grid.tiles[i]));
            }

            sectorApproxRadius = sectors.first().tile.v.dst(sectors.first().tile.corners[0].v);
        }
    }

    public @Nullable Sector getStartSector(){
        return sectors.size == 0 ? null : sectors.get(startSector);
    }

    public void applyRules(Rules rules){
        ruleSetter.get(rules);

        rules.attributes.clear();
        rules.attributes.add(defaultAttributes);
        rules.env = defaultEnv;
        rules.hiddenBuildItems.clear();
        rules.hiddenBuildItems.addAll(hiddenItems);
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

    /** @return whether this planet has any sectors to land on. */
    public boolean isLandable(){
        return sectors.size > 0;
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
        return (orbitOffset + universe.secondsf() / (orbitTime / 360f)) % 360f;
    }

    /** Calculates rotation on own axis based on universe time.*/
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
        mesh = meshLoader.get();
    }

    @Override
    public void load(){
        super.load();

        if(!headless){
            mesh = meshLoader.get();
            cloudMesh = cloudMeshLoader.get();
            if(grid != null) gridMesh = gridMeshLoader.get();
        }
    }

    @Override
    public void init(){
        if(techTree == null){
            techTree = TechTree.roots.find(n -> n.planet == this);
        }

        for(Sector sector : sectors){
            sector.loadInfo();
        }

        if(generator != null){
            Noise.setSeed(sectorSeed < 0 ? id + 1 : sectorSeed);

            for(Sector sector : sectors){
                generator.generateSector(sector);
            }

            updateBaseCoverage();
        }

        //make planet launch candidates mutual.
        var candidates = launchCandidates.copy();

        for(Planet planet : content.planets()){
            if(planet.launchCandidates.contains(this)){
                candidates.addUnique(planet);
            }
        }

        //TODO currently, mutual launch candidates are simply a nuisance.
        //launchCandidates = candidates;

        clipRadius = Math.max(clipRadius, radius + atmosphereRadOut + 0.5f);
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

    public void draw(PlanetParams params, Mat3D projection, Mat3D transform){
        mesh.render(params, projection, transform);
    }

    public void drawAtmosphere(Mesh atmosphere, Camera3D cam){
        //atmosphere does not contribute to depth buffer
        Gl.depthMask(false);

        Blending.additive.apply();

        Shaders.atmosphere.camera = cam;
        Shaders.atmosphere.planet = this;
        Shaders.atmosphere.bind();
        Shaders.atmosphere.apply();

        atmosphere.render(Shaders.atmosphere, Gl.triangles);

        Blending.normal.apply();

        Gl.depthMask(true);
    }

    public void drawClouds(PlanetParams params, Mat3D projection, Mat3D transform){
        if(cloudMesh != null){
            cloudMesh.render(params, projection, transform);
        }
    }

    /** Draws sector borders. Supply the batch with {@link Gl#triangles triangle} vertices. */
    public void drawBorders(VertexBatch3D batch, Sector sector, Color base, float alpha){
        Color color = Tmp.c1.set(base).a((base.a + 0.3f + Mathf.absin(Time.globalTime, 5f, 0.3f)) * alpha);

        float r1 = radius;
        float r2 = outlineRad * radius + 0.001f;

        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner c = sector.tile.corners[i], next = sector.tile.corners[(i+1) % sector.tile.corners.length];

            Tmp.v31.set(c.v).setLength(r2);
            Tmp.v32.set(next.v).setLength(r2);
            Tmp.v33.set(c.v).setLength(r1);

            batch.tri2(Tmp.v31, Tmp.v32, Tmp.v33, color);

            Tmp.v31.set(next.v).setLength(r2);
            Tmp.v32.set(next.v).setLength(r1);
            Tmp.v33.set(c.v).setLength(r1);

            batch.tri2(Tmp.v31, Tmp.v32, Tmp.v33, color);
        }
    }

    /** Draws sector plane. Supply the batch with {@link Gl#triangles triangle} vertices. */
    public void fill(VertexBatch3D batch, Sector sector, Color color, float offset){
        float rr = outlineRad * radius + offset;
        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner c = sector.tile.corners[i], next = sector.tile.corners[(i+1) % sector.tile.corners.length];
            batch.tri(Tmp.v31.set(c.v).setLength(rr), Tmp.v32.set(next.v).setLength(rr), Tmp.v33.set(sector.tile.v).setLength(rr), color);
        }
    }

    /** Draws sector when selected. Supply the batch with {@link Gl#triangles triangle} vertices. */
    public void drawSelection(VertexBatch3D batch, Sector sector, Color color, float stroke, float length){
        float arad = (outlineRad + length) * radius;

        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner next = sector.tile.corners[(i + 1) % sector.tile.corners.length];
            Corner curr = sector.tile.corners[i];

            next.v.scl(arad);
            curr.v.scl(arad);
            sector.tile.v.scl(arad);

            Tmp.v31.set(curr.v).sub(sector.tile.v).setLength(curr.v.dst(sector.tile.v) - stroke).add(sector.tile.v);
            Tmp.v32.set(next.v).sub(sector.tile.v).setLength(next.v.dst(sector.tile.v) - stroke).add(sector.tile.v);

            batch.tri(curr.v, next.v, Tmp.v31, color);
            batch.tri(Tmp.v31, next.v, Tmp.v32, color);

            sector.tile.v.scl(1f / arad);
            next.v.scl(1f / arad);
            curr.v.scl(1f /arad);
        }
    }

    /** Renders sector outlines. */
    public void renderSectors(VertexBatch3D batch, Camera3D cam, PlanetParams params){
        //apply transformed position
        batch.proj().mul(getTransform(mat));

        if(params.renderer != null){
            params.renderer.renderSectors(this);
        }

        //render sector grid
        float scaledOutlineRad = outlineRad * radius;
        Mesh mesh = gridMesh;
        Shader shader = Shaders.planetGrid;
        Vec3 tile = intersect(cam.getMouseRay(), scaledOutlineRad);
        Shaders.planetGrid.mouse.lerp(tile == null ? Vec3.Zero : tile.sub(position).rotate(Vec3.Y, getRotation()), 0.2f);

        shader.bind();
        shader.setUniformMatrix4("u_proj", cam.combined.val);
        shader.setUniformMatrix4("u_trans", getTransform(mat).val);
        shader.apply();
        mesh.render(shader, Gl.lines);
    }

    /** Draws an arc from one point to another on the planet. */
    public void drawArc(VertexBatch3D batch, Vec3 a, Vec3 b, Color from, Color to, float length, float timeScale, int pointCount){
        //increase curve height when on opposite side of planet, so it doesn't tunnel through
        float scaledOutlineRad = outlineRad * radius;
        float dot = 1f - (Tmp.v32.set(a).nor().dot(Tmp.v33.set(b).nor()) + 1f)/2f;

        Vec3 avg = Tmp.v31.set(b).add(a).scl(0.5f);
        avg.setLength(radius * (1f + length) + dot * 1.35f);

        points.clear();
        points.addAll(Tmp.v33.set(b).setLength(scaledOutlineRad), Tmp.v31, Tmp.v34.set(a).setLength(scaledOutlineRad));
        Tmp.bz3.set(points);

        for(int i = 0; i < pointCount + 1; i++){
            float f = i / (float)pointCount;
            Tmp.c1.set(from).lerp(to, (f + Time.globalTime / timeScale) % 1f);
            batch.color(Tmp.c1);
            batch.vertex(Tmp.bz3.valueAt(Tmp.v32, f));
        }
        batch.flush(Gl.lineStrip);
    }
}
