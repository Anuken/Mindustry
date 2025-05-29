package mindustry.graphics.g3d;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;

public class MeshBuilder{
    private static final Vec3 v1 = new Vec3(), v2 = new Vec3(), v3 = new Vec3(), v4 = new Vec3();
    private static final boolean gl30 = Core.gl30 != null;
    private static final float[] floats = new float[3 + (gl30 ? 1 : 3) + 1], emissiveFloats = new float[floats.length + 1];
    private static final short[] shorts = new short[3];
    private static float[] tmpHeights = new float[14580]; //highest amount of corners in vanilla
    private static Mesh mesh;

    public static Mesh buildIcosphere(int divisions, float radius, Color color){
        begin(20 * (2 << (2 * divisions - 1)) * 3, 0, false);

        float col = color.toFloatBits();

        MeshResult result = Icosphere.create(divisions);
        for(int i = 0; i < result.indices.size; i+= 3){
            v1.set(result.vertices.items, result.indices.items[i] * 3).setLength(radius);
            v2.set(result.vertices.items, result.indices.items[i + 1] * 3).setLength(radius);
            v3.set(result.vertices.items, result.indices.items[i + 2] * 3).setLength(radius);

            verts(v1, v3, v2, normal(v1, v2, v3).scl(-1f), col, 0f);
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

        float col = color.toFloatBits();

        begin(total, 0, false);
        for(Ptile tile : grid.tiles){
            Corner[] c = tile.corners;
            for(int i = 0; i < c.length; i++){
                Vec3 a = v1.set(c[i].v).scl(scale);
                Vec3 b = v2.set(c[(i + 1) % c.length].v).scl(scale);

                vert(a, Vec3.Z, col, 0f);
                vert(b, Vec3.Z, col, 0f);
            }
        }

        return end();
    }

    public static Mesh buildLineHex(Color color, int divisions){
        PlanetGrid grid = PlanetGrid.create(divisions);

        begin(grid.tiles.length * 12, 0, false);

        Vec3 nor = v4.set(1f, 1f, 1f);

        float col = color.toFloatBits();

        for(Ptile tile : grid.tiles){
            Corner[] c = tile.corners;

            for(int i = 0; i < c.length; i++){
                Vec3 v1 = c[i].v;
                Vec3 v2 = c[(i + 1) % c.length].v;

                vert(v1, nor, col, 0f);
                vert(v2, nor, col, 0f);
            }
        }

        return end();
    }

    public static Mesh buildHex(Color color, int divisions, float radius){
        return buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 0;
            }

            @Override
            public Color getColor(Vec3 position){
                return color;
            }
        }, divisions, radius, 0);
    }

    public static Mesh buildHex(HexMesher mesher, int divisions, float radius, float intensity){
        PlanetGrid grid = PlanetGrid.create(divisions);

        if(mesher instanceof PlanetGenerator generator){
            generator.seed = generator.baseSeed;
        }

        boolean emit = mesher.hasEmissive();

        boolean indexed = grid.tiles.length * 6 < 65535;

        if(indexed){
            begin(grid.tiles.length * 6, grid.tiles.length * 4 * 3, emit);
        }else{
            begin(grid.tiles.length * 12, 0, emit);
        }

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

        int position = 0;

        for(Ptile tile : grid.tiles){
            if(mesher.skip(tile.v)){
                continue;
            }

            Corner[] c = tile.corners;

            for(Corner corner : c){
                corner.v.scl((1f + heights[corner.id] * intensity) * radius);
            }

            Vec3 nor = normal(c[0].v, c[2].v, c[4].v);
            float color = mesher.getColor(tile.v).toFloatBits();
            float emissive = emit ? mesher.getEmissiveColor(tile.v).toFloatBits() : 0f;

            if(indexed){
                for(var corner : c){
                    vert(corner.v, nor, color, emissive);
                }

                indices(position, position + 1, position + 2);
                indices(position, position + 2, position + 3);
                indices(position, position + 3, position + 4);
                if(c.length > 5){
                    indices(position, position + 4, position + 5);
                }

                position += c.length;

            }else{
                verts(c[0].v, c[1].v, c[2].v, nor, color, emissive);
                verts(c[0].v, c[2].v, c[3].v, nor, color, emissive);
                verts(c[0].v, c[3].v, c[4].v, nor, color, emissive);

                if(c.length > 5){
                    verts(c[0].v, c[4].v, c[5].v, nor, color, emissive);
                }
            }

            //restore mutated corners
            for(Corner corner : c){
                corner.v.nor();
            }
        }

        return end();
    }

    private static void begin(int vertices, int indices, boolean emissive){
        Seq<VertexAttribute> attributes = Seq.with(
        VertexAttribute.position3,
        //only GL30 supports GL_INT_2_10_10_10_REV
        gl30 ? VertexAttribute.packedNormal : VertexAttribute.normal,
        VertexAttribute.color
        );

        if(emissive){
            attributes.add(new VertexAttribute(4, GL20.GL_UNSIGNED_BYTE, true, "a_emissive"));
        }

        mesh = new Mesh(true, vertices, indices, attributes.toArray(VertexAttribute.class));

        mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().capacity());
        mesh.getVerticesBuffer().position(0);

        if(indices > 0){
            mesh.getIndicesBuffer().limit(mesh.getIndicesBuffer().capacity());
            mesh.getIndicesBuffer().position(0);
        }
    }

    static int totalBytes;

    private static Mesh end(){
        Mesh last = mesh;
        last.getVerticesBuffer().limit(last.getVerticesBuffer().position());
        if(last.getNumIndices() > 0){
            last.getIndicesBuffer().limit(last.getIndicesBuffer().position());
        }
        mesh = null;

        totalBytes += last.getVerticesBuffer().capacity() * 4;
        totalBytes += last.getIndicesBuffer().capacity() * 2;

        Log.info("total memory used: @ mb", totalBytes / 1000f / 1000f);

        return last;
    }

    private static Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3){
        return v4.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
    }

    private static void indices(int a, int b, int c){
        shorts[0] = (short)a;
        shorts[1] = (short)b;
        shorts[2] = (short)c;
        mesh.getIndicesBuffer().put(shorts);
    }

    private static void verts(Vec3 a, Vec3 b, Vec3 c, Vec3 normal, float color, float emissive){
        vert(a, normal, color, emissive);
        vert(b, normal, color, emissive);
        vert(c, normal, color, emissive);
    }

    private static void vert(Vec3 a, Vec3 normal, float color, float emissive){
        boolean emit = mesh.getVertexSize() == emissiveFloats.length*4;
        float[] floats = emit ? emissiveFloats : MeshBuilder.floats;

        floats[0] = a.x;
        floats[1] = a.y;
        floats[2] = a.z;

        if(gl30){
            floats[3] = packNormals(normal.x, normal.y, normal.z);

            floats[4] = color;
            if(emit) floats[5] = emissive;
        }else{
            floats[3] = normal.x;
            floats[4] = normal.x;
            floats[5] = normal.x;

            floats[6] = color;
            if(emit) floats[7] = emissive;
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
