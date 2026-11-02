package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class HexSkyMesh extends PlanetMesh{
    static Mat3D mat = new Mat3D();

    public float speed = 0f;

    public HexSkyMesh(Planet planet, int seed, float speed, float radius, int divisions, Color color, int octaves, float persistence, float scl, float thresh){
        super(planet, MeshBuilder.buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 1f;
            }

            @Override
            public void getColor(Vec3 position, Color out){
                out.set(color);
            }

            @Override
            public boolean skip(Vec3 position){
                return Simplex.noise3d(7 + seed, octaves, persistence, scl, position.x, position.y * 3f, position.z) >= thresh;
            }
        }, divisions, planet.radius, radius), Shaders.clouds);

        this.speed = speed;
    }

    public HexSkyMesh(){
    }

    public float relRot(){
        return Time.globalTime * speed / 40f;
    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        if(params.spaceCamera != null){
            //spaceCamera cloud switch
            if(!params.spaceCamera.clouds) return;
        }else if(params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)){
            //don't waste performance rendering 0-alpha clouds
            return;
        }

        float rotation = cloudRotation(params);
        preRender(params, rotation);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        shader.setUniformMatrix4("u_trans", mat.setToTranslation(planet.position).rotate(Vec3.Y, planet.getRotation() + rotation).val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
    }

    @Override
    public void preRender(PlanetParams params){
        preRender(params, relRot());
    }

    private void preRender(PlanetParams params, float rotation){
        Shaders.clouds.planet = planet;
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation() + rotation).nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = params.spaceCamera != null ? 1f : (params.planet == planet ? 1f - params.uiAlpha : 1f);
    }

    private float cloudRotation(PlanetParams params){
        return params.spaceCamera == null ? relRot() : params.spaceCamera.cloudTime * speed / 40f;
    }
}
