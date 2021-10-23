package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import mindustry.type.*;

/** Defines a mesh that is rendered for a planet. Subclasses provide a mesh and a shader. */
public abstract class PlanetMesh implements GenericMesh{
    protected Mesh mesh;
    protected Planet planet;
    protected Shader shader;

    public PlanetMesh(Planet planet, Mesh mesh, Shader shader){
        this.planet = planet;
        this.mesh = mesh;
        this.shader = shader;
    }

    public PlanetMesh(){}

    /** Should be overridden to set up any shader parameters such as planet position, normals, etc. */
    public void preRender(PlanetParams params){

    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        preRender(params);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        shader.setUniformMatrix4("u_trans", transform.val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
    }
}
