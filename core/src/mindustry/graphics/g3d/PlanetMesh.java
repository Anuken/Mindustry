package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.type.*;

/** Defines a mesh that is rendered for a planet. Subclasses provide a mesh and a shader. */
public abstract class PlanetMesh implements Disposable{
    protected final Mesh mesh;
    protected final Planet planet;
    protected final Shader shader;

    public PlanetMesh(Planet planet, Mesh mesh, Shader shader){
        this.planet = planet;
        this.mesh = mesh;
        this.shader = shader;
    }

    /** Should be overridden to set up any shader parameters such as planet position, normals, etc. */
    public abstract void preRender();

    public void render(Mat3D projection, Mat3D transform){
        preRender();
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        shader.setUniformMatrix4("u_trans", transform.val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
    }

    @Override
    public void dispose(){
        mesh.dispose();
    }
}
