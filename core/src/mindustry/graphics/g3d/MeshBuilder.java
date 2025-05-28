package mindustry.graphics.g3d;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;

public class MeshBuilder{
    private static final Vec3 v1 = new Vec3(), v2 = new Vec3(), v3 = new Vec3(), v4 = new Vec3();
    private static final boolean gl30 = Core.gl30 != null;
    private static final float[] floats = new float[3 + (gl30 ? 1 : 3) + 1];
    private static float[] tmpHeights = new float[14580]; //highest amount of corners in vanilla
    private static Mesh mesh;

    public static Mesh buildIcosphere(int divisions, float radius, Color color){
        begin(20 * (2 << (2 * divisions - 1)) * 3);

        MeshResult result = Icosphere.create(divisions);
        for(int i = 0; i < result.indices.size; i+= 3){
            v1.set(result.vertices.items, result.indices.items[i] * 3).setLength(radius);
            v2.set(result.vertices.items, result.indices.items[i + 1] * 3).setLength(radius);
            v3.set(result.vertices.items, result.indices.items[i + 2] * 3).setLength(radius);

            verts(v1, v3, v2, normal(v1, v2, v3).scl(-1f), color);
        }

        return end();
    }

    public static Mesh buildIcosphere(int divisions, float radius){
        return buildIcosphere(divisions, radius, Color.white);
    }

    public static Mesh buildPlanetGrid(PlanetGrid grid, Color color, float scale){
        int total = 0;
        for(Ptile tile : grid.tiles){
            total += tile.corners.length * 2;
        }

        begin(total);
        for(Ptile tile : grid.tiles){
            Corner[] c = tile.corners;
            for(int i = 0; i < c.length; i++){
                Vec3 a = v1.set(c[i].v).scl(scale);
                Vec3 b = v2.set(c[(i + 1) % c.length].v).scl(scale);

                vert(a, Vec3.Z, color);
                vert(b, Vec3.Z, color);
            }
        }

        return end();
    }

    public static Mesh buildHex(Color color, int divisions, boolean lines, float radius){
        return buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 0;
            }

            @Override
            public Color getColor(Vec3 position){
                return color;
            }
        }, divisions, lines, radius, 0);
    }

    public static Mesh buildHex(HexMesher mesher, int divisions, boolean lines, float radius, float intensity){
        PlanetGrid grid = PlanetGrid.create(divisions);

        if(mesher instanceof PlanetGenerator generator){
            generator.seed = generator.baseSeed;
        }

        begin(grid.tiles.length * 12);

        float[] heights;

        if(tmpHeights == null || tmpHeights.length < grid.corners.length){
            heights = tmpHeights = new float[grid.corners.length];
        }else{
            heights = tmpHeights;
        }

        //cache heights in an array to prevent redundant calls to getHeight
        for(int i = 0; i < grid.corners.length; i++){
            heights[i] = mesher.getHeight(grid.corners[i].v);
        }

        for(Ptile tile : grid.tiles){
            if(mesher.skip(tile.v)){
                continue;
            }

            Corner[] c = tile.corners;

            for(Corner corner : c){
                corner.v.scl((1f + heights[corner.id] * intensity) * radius);
            }

            Vec3 nor = normal(c[0].v, c[2].v, c[4].v);
            Color color = mesher.getColor(v2.set(tile.v));

            if(lines){
                nor.set(1f, 1f, 1f);

                for(int i = 0; i < c.length; i++){
                    Vec3 v1 = c[i].v;
                    Vec3 v2 = c[(i + 1) % c.length].v;

                    vert(v1, nor, color);
                    vert(v2, nor, color);
                }
            }else{
                verts(c[0].v, c[1].v, c[2].v, nor, color);
                verts(c[0].v, c[2].v, c[3].v, nor, color);
                verts(c[0].v, c[3].v, c[4].v, nor, color);

                if(c.length > 5){
                    verts(c[0].v, c[4].v, c[5].v, nor, color);
                }
            }

            //restore mutated corners
            for(Corner corner : c){
                corner.v.nor();
            }

        }

        return end();
    }

    private static void begin(int count){
        mesh = new Mesh(true, count, 0,
        VertexAttribute.position3,
        !gl30 ? VertexAttribute.normal : VertexAttribute.packedNormal,
        VertexAttribute.color
        );

        mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().capacity());
        mesh.getVerticesBuffer().position(0);
    }

    private static Mesh end(){
        Mesh last = mesh;
        last.getVerticesBuffer().limit(last.getVerticesBuffer().position());
        mesh = null;

        return last;
    }

    private static Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3){
        return v4.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
    }

    private static void verts(Vec3 a, Vec3 b, Vec3 c, Vec3 normal, Color color){
        vert(a, normal, color);
        vert(b, normal, color);
        vert(c, normal, color);
    }

    private static void vert(Vec3 a, Vec3 normal, Color color){
        floats[0] = a.x;
        floats[1] = a.y;
        floats[2] = a.z;

        if(gl30){
            floats[3] = packNormals(normal.x, normal.y, normal.z);

            floats[4] = color.toFloatBits();
        }else{
            floats[3] = normal.x;
            floats[4] = normal.x;
            floats[5] = normal.x;

            floats[6] = color.toFloatBits();
        }

        mesh.getVerticesBuffer().put(floats);
    }

    private static float packNormals(float x, float y, float z){
        int xs = x < 0 ? 1 : 0;
        int ys = y < 0 ? 1 : 0;
        int zs = z < 0 ? 1 : 0;
        int vi =
        zs << 29 | ((int)(z * 511 + (zs << 9)) & 511) << 20 |
        ys << 19 | ((int)(y * 511 + (ys << 9)) & 511) << 10 |
        xs << 9  | ((int)(x * 511 + (xs << 9)) & 511);
        return Float.intBitsToFloat(vi);
    }
}
