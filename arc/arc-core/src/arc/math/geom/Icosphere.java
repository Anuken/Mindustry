package arc.math.geom;

import arc.math.*;
import arc.struct.*;

public class Icosphere{
    private static final float t = (Mathf.sqrt(5) - 1f) / 2f;
    private static final Vec3[] baseVert = {v(-1, -t, 0), v(0, 1, t), v(0, 1, -t), v(1, t, 0), v(1, -t, 0), v(0, -1, -t), v(0, -1, t), v(t, 0, 1), v(-t, 0, 1), v(t, 0, -1), v(-t, 0, -1), v(-1, t, 0)};
    private static final int[][] baseFace = new int[][]{
    {3, 7, 1}, {4, 7, 3}, {6, 7, 4}, {8, 7, 6}, {7, 8, 1}, {9, 4, 3}, {2, 9, 3}, {2, 3, 1}, {11, 2, 1}, {10, 2, 11},
    {10, 9, 2}, {9, 5, 4}, {6, 4, 5}, {0, 6, 5}, {0, 11, 8}, {11, 1, 8}, {10, 0, 5}, {10, 5, 9}, {0, 8, 6}, {0, 10, 11},
    };

    public static MeshResult create(int level){
        MeshResult data = new MeshResult();

        for(Vec3 v : baseVert){
            data.vertices.add(v.x, v.y, v.z);
        }

        for(int[] f : baseFace){
            subdivide(f[0], f[1], f[2], data.vertices, data.indices, level);
        }

        return data;
    }

    private static void subdivide(int v1, int v2, int v3, FloatSeq vertices, IntSeq faces, int level){
        if(level == 0){
            faces.add(v1, v2, v3);
        }else{
            float a1 = (vertices.get(3 * v1) + vertices.get(3 * v2));
            float a2 = (vertices.get(3 * v1 + 1) + vertices.get(3 * v2 + 1));
            float a3 = (vertices.get(3 * v1 + 2) + vertices.get(3 * v2 + 2));
            float length = Vec3.len(a1, a2, a3);
            int indexA = vertices.size / 3;
            vertices.add(a1 / length, a2 / length, a3 / length);

            float b1 = (vertices.get(3 * v3) + vertices.get(3 * v2));
            float b2 = (vertices.get(3 * v3 + 1) + vertices.get(3 * v2 + 1));
            float b3 = (vertices.get(3 * v3 + 2) + vertices.get(3 * v2 + 2));
            length = Vec3.len(b1, b2, b3);
            int indexB = vertices.size / 3;
            vertices.add(b1 / length, b2 / length, b3 / length);

            float c1 = (vertices.get(3 * v1) + vertices.get(3 * v3));
            float c2 = (vertices.get(3 * v1 + 1) + vertices.get(3 * v3 + 1));
            float c3 = (vertices.get(3 * v1 + 2) + vertices.get(3 * v3 + 2));
            length = Vec3.len(c1, c2, c3);
            int indexC = vertices.size / 3;
            vertices.add(c1 / length, c2 / length, c3 / length);

            subdivide(v1, indexA, indexC, vertices, faces, level - 1);
            subdivide(indexA, v2, indexB, vertices, faces, level - 1);
            subdivide(indexC, indexB, v3, vertices, faces, level - 1);
            subdivide(indexA, indexB, indexC, vertices, faces, level - 1);
        }
    }

    private static Vec3 v(float x, float y, float z){
        return new Vec3(x, y, z).nor();
    }

    public static class MeshResult{
        public FloatSeq vertices = new FloatSeq();
        public IntSeq indices = new IntSeq();
    }
}
