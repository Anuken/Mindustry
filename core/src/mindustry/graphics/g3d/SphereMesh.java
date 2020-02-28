package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.geom.*;

public class SphereMesh extends GenericMesh{
    private static final Vec3 v1 = new Vec3(), v2 = new Vec3(), v3 = new Vec3(), v4 = new Vec3();

    protected final float radius;

    public SphereMesh(int divisions, float radius){
        super(20 * (2 << (2 * divisions - 1)) * 7 * 3, Gl.triangles);
        this.radius = radius;

        MeshResult result = Icosphere.create(divisions);
        for(int i = 0; i < result.indices.size; i+= 3){
            v1.set(result.vertices.items, result.indices.items[i] * 3).setLength(radius);
            v2.set(result.vertices.items, result.indices.items[i + 1] * 3).setLength(radius);
            v3.set(result.vertices.items, result.indices.items[i + 2] * 3).setLength(radius);

            verts(v1, v3, v2, normal(v1, v2, v3).scl(-1f), Color.white);
        }
    }
}
