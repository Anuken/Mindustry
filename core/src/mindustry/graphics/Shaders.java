package mindustry.graphics;

import arc.files.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.graphics.shaders.*;

import static mindustry.Vars.*;

public class Shaders{
    public static BlockBuildShader blockbuild;
    public static @Nullable ShieldShader shield;
    public static BuildBeamShader buildBeam;
    public static UnitBuildShader build;
    public static UnitArmorShader armor;
    public static DarknessShader darkness;
    public static FogShader fog;
    public static LightShader light;
    public static SurfaceShader water, mud, tar, slag, cryofluid, space, caustics, arkycite;
    public static PlanetShader planet;
    public static CloudShader clouds;
    public static PlanetGridShader planetGrid;
    public static AtmosphereShader atmosphere;
    public static DepthScreenspaceShader depthScreenspace;
    public static ShockwaveShader shockwave;
    public static MeshShader mesh;
    public static Shader unlit, unlitWhite;
    public static Shader screenspace;

    public static void init(){
        mesh = new MeshShader();
        blockbuild = new BlockBuildShader();
        try{
            shield = new ShieldShader();
        }catch(Throwable t){
            //don't load shield shader
            shield = null;
            t.printStackTrace();
        }
        fog = new FogShader();
        buildBeam = new BuildBeamShader();
        build = new UnitBuildShader();
        armor = new UnitArmorShader();
        darkness = new DarknessShader();
        light = new LightShader();
        water = new SurfaceShader("water");
        arkycite = new SurfaceShader("arkycite");
        mud = new SurfaceShader("mud");
        tar = new SurfaceShader("tar");
        slag = new SurfaceShader("slag");
        cryofluid = new SurfaceShader("cryofluid");
        space = new SpaceShader("space");
        caustics = new SurfaceShader("caustics"){
            @Override
            public String textureName(){
                return "caustics";
            }
        };
        planet = new PlanetShader();
        clouds = new CloudShader();
        planetGrid = new PlanetGridShader();
        atmosphere = new AtmosphereShader();
        unlit = new LoadShader("planet", "unlit");
        unlitWhite = new LoadShader("planet", "unlitwhite");
        screenspace = new LoadShader("screenspace", "screenspace");
        depthScreenspace = new DepthScreenspaceShader();
    }

    public static Fi getShaderFi(String file){
        return tree.get("shaders/" + file);
    }
}
