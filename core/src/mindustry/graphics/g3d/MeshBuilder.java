package mindustry.graphics.g3d;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;

public class MeshBuilder{
    private static final boolean gl30 = Core.gl30 != null;
    private static volatile float[] tmpHeights = new float[14580]; //highest amount of corners in vanilla

    /** Note that the resulting icosphere does not have normals or a color. */
    public static Mesh buildIcosphere(int divisions, float radius){
        MeshResult result = Icosphere.create(divisions);

        Mesh mesh = begin(result.vertices.size / 3, result.indices.size, false, false);

        if(result.vertices.size >= 65535) throw new RuntimeException("Due to index size limits, only meshes with a maximum of 65535 vertices are supported. If you want more than that, make your own non-indexed mesh builder.");

        float[] items = result.vertices.items;
        for(int i = 0; i < result.vertices.size; i ++){
            items[i] *= radius;
        }

        mesh.getVerticesBuffer().put(items, 0, result.vertices.size);

        short[] indices = new short[result.indices.size];
        for(int i = 0; i < result.indices.size; i++){
            indices[i] = (short)result.indices.items[i];
        }

        mesh.getIndicesBuffer().put(indices);

        return end(mesh);
    }

    public static Mesh buildPlanetGrid(PlanetGrid grid, Color color, float scale){
        Mesh mesh = begin(grid.tiles.length * 12, 0, false, false);

        float col = color.toFloatBits();
        float[] floats = new float[8];

        for(Ptile tile : grid.tiles){
            Corner[] c = tile.corners;

            for(int i = 0; i < c.length; i++){
                Vec3 v1 = c[i].v;
                Vec3 v2 = c[(i + 1) % c.length].v;

                floats[0] = v1.x * scale;
                floats[1] = v1.y * scale;
                floats[2] = v1.z * scale;
                floats[3] = col;

                floats[4] = v2.x * scale;
                floats[5] = v2.y * scale;
                floats[6] = v2.z * scale;
                floats[7] = col;

                mesh.getVerticesBuffer().put(floats);
            }
        }

        return end(mesh);
    }

    public static Mesh buildHex(Color color, int divisions, float radius){
        return buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 0;
            }

            @Override
            public void getColor(Vec3 position, Color out){
                out.set(color);
            }
        }, divisions, radius, 0);
    }

    //TODO: in principle this should not be synchronized, but I would rather not realloc tmpHeights every time, and it is unlikely that two planets will be reloading at the same time
    public static synchronized Mesh buildHex(HexMesher mesher, int divisions, float radius, float intensity){
        PlanetGrid grid = PlanetGrid.create(divisions);

        //TODO: this is NOT thread safe, but in practice, it should never cause a problem
        if(mesher instanceof PlanetGenerator generator){
            generator.seed = generator.baseSeed;
        }

        boolean emit = mesher.isEmissive();

        boolean indexed = grid.tiles.length * 6 < 65535;

        Mesh mesh = begin(indexed ? grid.tiles.length * 6 : grid.tiles.length * 12, indexed ? grid.tiles.length * 4 * 3 : 0, true, emit);

        float[] heights;

        if(tmpHeights == null || tmpHeights.length < grid.corners.length){
            heights = tmpHeights = new float[grid.corners.length];
        }else{
            heights = tmpHeights;
        }

        //cache heights in an array to prevent redundant calls to getHeight
        for(int i = 0; i < grid.corners.length; i++){
            heights[i] = (1f + mesher.getHeight(grid.corners[i].v) * intensity) * radius;
        }
        int position = 0;

        short[] shorts = indexed ? new short[12] : null;
        float[] floats = new float[3 + (gl30 ? 1 : 3) + 1 + (emit ? 1 : 0)];
        Vec3 nor = new Vec3();

        Color tmpCol = new Color();

        for(Ptile tile : grid.tiles){
            if(mesher.skip(tile.v)){
                continue;
            }

            Corner[] c = tile.corners;

            float
            h1 = heights[c[0].id],
            h2 = heights[c[2].id],
            h3 = heights[c[4].id];

            Vec3
            v1 = c[0].v,
            v2 = c[2].v,
            v3 = c[4].v;

            normal(
            v1.x * h1, v1.y * h1, v1.z * h1,
            v2.x * h2, v2.y * h2, v2.z * h2,
            v3.x * h3, v3.y * h3, v3.z * h3,
            nor);

            tmpCol.set(1f, 1f, 1f, 1f);
            mesher.getColor(tile.v, tmpCol);
            float color = tmpCol.toFloatBits();

            float emissive = 0f;

            if(emit){
                tmpCol.set(0f, 0f, 0f, 0f);
                mesher.getEmissiveColor(tile.v, tmpCol);
                emissive = tmpCol.toFloatBits();
            }

            if(indexed){
                for(var corner : c){
                    float height = heights[corner.id];

                    vert(mesh, floats, corner.v.x * height, corner.v.y * height, corner.v.z * height, nor, color, emissive);
                }

                shorts[0] = (short)(position);
                shorts[1] = (short)(position + 1);
                shorts[2] = (short)(position + 2);

                shorts[3] = (short)(position);
                shorts[4] = (short)(position + 2);
                shorts[5] = (short)(position + 3);

                shorts[6] = (short)(position);
                shorts[7] = (short)(position + 3);
                shorts[8] = (short)(position + 4);

                if(c.length > 5){
                    shorts[9] = (short)(position);
                    shorts[10] = (short)(position + 4);
                    shorts[11] = (short)(position + 5);
                }

                mesh.getIndicesBuffer().put(shorts, 0, c.length > 5 ? 12 : 9);

                position += c.length;
            }else{
                verts(mesh, floats, c[0].v, heights[c[0].id], c[1].v, heights[c[1].id], c[2].v, heights[c[2].id], nor, color, emissive);
                verts(mesh, floats, c[0].v, heights[c[0].id], c[2].v, heights[c[2].id], c[3].v, heights[c[3].id], nor, color, emissive);
                verts(mesh, floats, c[0].v, heights[c[0].id], c[3].v, heights[c[3].id], c[4].v, heights[c[4].id], nor, color, emissive);

                if(c.length > 5){
                    verts(mesh, floats, c[0].v, heights[c[0].id], c[4].v, heights[c[4].id], c[5].v, heights[c[5].id], nor, color, emissive);
                }
            }
        }

        return end(mesh);
    }

    private static Mesh begin(int vertices, int indices, boolean normal, boolean emissive){
        Seq<VertexAttribute> attributes = Seq.with(
        VertexAttribute.position3
        );

        if(normal){
            //only GL30 supports GL_INT_2_10_10_10_REV
            attributes.add(gl30 ? VertexAttribute.packedNormal : VertexAttribute.normal);
        }

        attributes.add(VertexAttribute.color);

        if(emissive){
            attributes.add(new VertexAttribute(4, GL20.GL_UNSIGNED_BYTE, true, "a_emissive"));
        }

        Mesh mesh = new Mesh(true, vertices, indices, attributes.toArray(VertexAttribute.class));

        mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().capacity());
        mesh.getVerticesBuffer().position(0);

        if(indices > 0){
            mesh.getIndicesBuffer().limit(mesh.getIndicesBuffer().capacity());
            mesh.getIndicesBuffer().position(0);
        }

        return mesh;
    }

    private static Mesh end(Mesh mesh){
        mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().position());
        if(mesh.getNumIndices() > 0){
            mesh.getIndicesBuffer().limit(mesh.getIndicesBuffer().position());
        }

        return mesh;
    }

    private static Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 out){
        return out.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
    }

    private static void normal(float v1x, float v1y, float v1z, float v2x, float v2y, float v2z, float v3x, float v3y, float v3z, Vec3 out){
        float
        x = v2x - v1x,
        y = v2y - v1y,
        z = v2z - v1z,
        vx = v3x - v1x,
        vy = v3y - v1y,
        vz = v3z - v1z;

        float
        cx = y * vz - z * vy,
        cy = z * vx - x * vz,
        cz = x * vy - y * vx;

        out.set(cx, cy, cz).nor();
    }

    private static void verts(Mesh mesh, float[] floats, Vec3 a, float h1, Vec3 b, float h2, Vec3 c, float h3, Vec3 normal, float color, float emissive){
        vert(mesh, floats, a.x * h1, a.y * h1, a.z * h1, normal, color, emissive);
        vert(mesh, floats, b.x * h2, b.y * h2, b.z * h2, normal, color, emissive);
        vert(mesh, floats, c.x * h3, c.y * h3, c.z * h3, normal, color, emissive);
    }

    private static void vert(Mesh mesh, float[] floats, float x, float y, float z, Vec3 normal, float color, float emissive){
        floats[0] = x;
        floats[1] = y;
        floats[2] = z;

        if(gl30){
            floats[3] = packNormals(normal.x, normal.y, normal.z);

            floats[4] = color;
            if(floats.length > 5) floats[5] = emissive;
        }else{
            floats[3] = normal.x;
            floats[4] = normal.x;
            floats[5] = normal.x;

            floats[6] = color;
            if(floats.length > 7) floats[7] = emissive;
        }

        mesh.getVerticesBuffer().put(floats);
    }

    private static float packNormals(float x, float y, float z){
        int xs = x < -1f/512f ? 1 : 0;
        int ys = y < -1f/512f ? 1 : 0;
        int zs = z < -1f/512f ? 1 : 0;

        int vi =
        zs << 29 | ((int)(z * 511 + (zs << 9)) & 511) << 20 |
        ys << 19 | ((int)(y * 511 + (ys << 9)) & 511) << 10 |
        xs << 9  | ((int)(x * 511 + (xs << 9)) & 511);
        return Float.intBitsToFloat(vi);
    }
}
