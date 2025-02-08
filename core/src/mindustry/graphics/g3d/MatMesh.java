package mindustry.graphics.g3d;

import arc.math.geom.*;

//TODO maybe this is a bad idea
/** A GenericMesh that wraps and applies an additional transform to a generic mesh. */
public class MatMesh implements GenericMesh{
    private static final Mat3D tmp = new Mat3D();

    GenericMesh mesh;
    Mat3D mat;

    public MatMesh(GenericMesh mesh, Mat3D mat){
        this.mesh = mesh;
        this.mat = mat;
    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        mesh.render(params, projection, tmp.set(transform).mul(mat));
    }
}
