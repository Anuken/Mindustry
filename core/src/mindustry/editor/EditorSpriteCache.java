package mindustry.editor;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

public class EditorSpriteCache implements Disposable{
    //packed xy + color + packed uv
    static final int vertexSize = 1 + 1 + 1;

    private @Nullable Mesh mesh;
    private final Seq<Texture> textures = new Seq<>(8);
    private final IntSeq counts = new IntSeq(8);
    private final float packX, packY, packW, packH;

    private float[] tmpVertices;

    /** Index in tmpVertices of current vertex data. */
    private int index;

    /** @param tmpVertices Temporary buffer to hold vertices while building up sprites. Should be large enough to hold all sprite data this cache will contain. */
    public EditorSpriteCache(float[] tmpVertices, float packX, float packY, float packW, float packH){
        this.tmpVertices = tmpVertices;
        this.packX = packX;
        this.packY = packY;
        this.packW = packW;
        this.packH = packH;
    }

    /** @return whether anything was added to the cache. */
    public boolean isEmpty(){
        return index == 0;
    }

    /**
     * Builds this cache into a mesh that can be used for rendering. Use after calling {@link #draw(TextureRegion, float, float, float, float, float, float, float, float)}.
     * Until this method is called, no mesh is created.
     *
     * @param indices The shared index data in standard quad format, as seen in SpriteBatch.
     * Since this data is static, it should be the same across all caches, and be large enough to accommodate all sprites.
     * */
    public void build(IndexData indices){
        if(mesh != null) mesh.dispose();

        mesh = new Mesh(true, index / vertexSize, 0,
        VertexAttribute.packedPosition,
        VertexAttribute.color,
        VertexAttribute.packedTexCoords
        );
        mesh.indices = indices;
        mesh.setVertices(tmpVertices, 0, index);
    }

    /** Adds the specified region to the cache. */
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation, float colorPacked){
        if(mesh != null) throw new IllegalStateException("This cache is already built. Call #clear() before drawing new sprites.");

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        float x1, y1, x2, y2, x3, y3, x4, y4;

        // rotate
        if(rotation != 0){
            final float cos = Mathf.cosDeg(rotation);
            final float sin = Mathf.sinDeg(rotation);

            x1 = cos * fx - sin * fy;
            y1 = sin * fx + cos * fy;

            x2 = cos * fx - sin * fy2;
            y2 = sin * fx + cos * fy2;

            x3 = cos * fx2 - sin * fy2;
            y3 = sin * fx2 + cos * fy2;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        }else{
            x1 = fx;
            y1 = fy;

            x2 = fx;
            y2 = fy2;

            x3 = fx2;
            y3 = fy2;

            x4 = fx2;
            y4 = fy;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;

        int idx = index;
        float[] verts = tmpVertices;
        Texture texture = region.texture;

        verts[idx + 0] = pack(x1, y1);
        verts[idx + 1] = colorPacked;
        verts[idx + 2] = Pack.packUv(u, v);

        verts[idx + 3] = pack(x2, y2);
        verts[idx + 4] = colorPacked;
        verts[idx + 5] = Pack.packUv(u, v2);

        verts[idx + 6] = pack(x3, y3);
        verts[idx + 7] = colorPacked;
        verts[idx + 8] = Pack.packUv(u2, v2);

        verts[idx + 9] = pack(x4, y4);
        verts[idx + 10] = colorPacked;
        verts[idx + 11] = Pack.packUv(u2, v);

        int lastIndex = textures.size - 1;
        if(lastIndex < 0 || textures.get(lastIndex) != texture){
            textures.add(texture);
            counts.add(6);
        }else{
            counts.incr(lastIndex, 6);
        }

        index += vertexSize * 4;
    }

    private float pack(float x, float y){
        return Pack.packUv((x + packX) / packW, (y + packY) / packH);
    }

    /** Renders the cached mesh. The shader must already have the correct view matrix set as a uniform. */
    public void render(Shader shader){
        if(mesh == null) throw new IllegalStateException("Cache is empty, call build() first.");

        int offset = 0;

        for(int i = 0; i < textures.size; i++){
            int count = counts.items[i];
            textures.get(i).bind();

            mesh.render(shader, Gl.triangles, offset, count);
            offset += count;
        }
    }

    @Override
    public void dispose(){
        if(mesh != null){
            mesh.dispose();
            mesh = null;
        }
    }
}