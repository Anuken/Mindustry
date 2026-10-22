package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

import java.nio.*;
import java.util.*;

/**
 * Draws 2D images, optimized for geometry that does not change. Sprites and/or textures are cached and given an ID, which can
 * later be used for drawing. The size, color, and texture region for each cached image cannot be modified. This information is
 * stored in video memory and does not have to be sent to the GPU each time it is drawn.<br>
 * <br>
 * To cache {@link Texture textures}, first call {@link SpriteCache#beginCache()}, then call the
 * appropriate add method to define the images. To complete the cache, call {@link SpriteCache#endCache()} and store the returned
 * cache ID.<br>
 * <br>
 * To draw with SpriteCache, first call {@link #begin()}, then call {@link #draw(int)} with a cache ID. When SpriteCache drawing
 * is complete, call {@link #end()}.<br>
 * <br>
 * By default, SpriteCache draws using screen coordinates and uses an x-axis pointing to the right, an y-axis pointing upwards and
 * the origin is the bottom left corner of the screen. The default transformation and projection matrices can be changed. If the
 * screen is {@link ApplicationListener#resize(int, int) resized}, the SpriteCache's matrices must be updated. For example:<br>
 * <code>cache.getProjection().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());</code><br>
 * <br>
 * Note that SpriteCache does not manage blending. You will need to enable blending (<i>Gl.enable(Gl.blend);</i>) and
 * set the blend func as needed before or between calls to {@link #draw(int)}.<br>
 * <br>
 * SpriteCache must be disposed once it is no longer needed.
 * @author Nathan Sweet
 */
public class SpriteCache implements Disposable{
    //xy + uv + depth + color
    static final int vertexSize = 2 + 2 + 1 + 1;

    private static final float[] tempVertices = new float[vertexSize * 6];

    private final Mesh mesh;
    private final Mat transformMatrix = new Mat();
    private final Mat projectionMatrix = new Mat();
    private final Mat combinedMatrix = new Mat();
    private final Shader shader;
    private final Seq<Texture> textures = new Seq<>(8);
    private final IntSeq counts = new IntSeq(8);
    /** Number of render calls since the last {@link #begin()}. **/
    public int renderCalls = 0;
    /** Number of rendering calls, ever. Will not be reset unless set manually. **/
    public int totalRenderCalls = 0;
    private boolean drawing;
    private Seq<Cache> caches;
    private Cache currentCache;
    private float colorPacked = Color.whiteFloatBits;
    private Shader customShader = null;

    /** Creates a cache that uses indexed geometry and can contain up to 1000 images. */
    public SpriteCache(){
        this(1000, false);
    }

    /**
     * Creates a cache with the specified size, using a default shader.
     * @param size The maximum number of images this cache can hold. The memory required to hold the images is allocated up front.
     * Max of 8191 if indices are used.
     * @param useIndices If true, indexed geometry will be used.
     */
    public SpriteCache(int size, boolean useIndices){
        this(size, 16, getDefaultShader(), useIndices);
    }

    public SpriteCache(int size, int cacheSize, boolean useIndices){
        this(size, cacheSize, getDefaultShader(), useIndices);
    }

    /**
     * Creates a cache with the specified size and OpenGL ES 2.0 shader.
     * @param size The maximum number of images this cache can hold. The memory required to hold the images is allocated up front.
     * Max of 8191 if indices are used.
     * @param useIndices If true, indexed geometry will be used.
     */
    public SpriteCache(int size, int cacheSize, Shader shader, boolean useIndices){
        this.shader = shader;

        if(useIndices && size > 16382)
            throw new IllegalArgumentException("Can't have more than 16382 sprites per batch: " + size);

        mesh = new Mesh(true, size * (useIndices ? 4 : 6), 0,
        VertexAttribute.position,
        VertexAttribute.texCoords3,
        VertexAttribute.color
        );
        if(useIndices){
            mesh.indices = SpriteIndices.get();
        }

        caches = new Seq<>(cacheSize);

        projectionMatrix.setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
    }

    private static @Nullable Shader defaultShader;

    public static Shader getDefaultShader(){
        if(defaultShader != null) return defaultShader;
        String vertexShader = "attribute vec4 " + Shader.positionAttribute + ";\n" //
        + "attribute vec4 " + Shader.colorAttribute + ";\n" //
        + "attribute vec3 " + Shader.texcoordAttribute + "0;\n" //
        + "uniform mat4 u_projectionViewMatrix;\n" //
        + "varying vec4 v_color;\n" //
        + "varying vec3 v_texCoords;\n" //
        + "\n" //
        + "void main(){\n" //
        + "   v_color = " + Shader.colorAttribute + ";\n" //
        + "   v_color.a = v_color.a * (255.0/254.0);\n" //
        + "   v_texCoords = " + Shader.texcoordAttribute + "0;\n" //
        + "   gl_Position =  u_projectionViewMatrix * " + Shader.positionAttribute + ";\n" //
        + "}\n";
        String fragmentShader =
          "varying lowp vec4 v_color;\n" //
        + "varying highp vec3 v_texCoords;\n" //
        + "uniform highp sampler2DArray u_texture;\n"
        + "void main(){\n" //
        + "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
        + "}";
        return defaultShader = new Shader(vertexShader, fragmentShader);
    }

    public Seq<Cache> getCaches(){
        return caches;
    }

    /** @see #setColor(Color) */
    public void setColor(float r, float g, float b, float a){
        colorPacked = Color.toFloatBits(r, g, b, a);
    }

    /** Sets the color used to tint images when they are added to the SpriteCache. Default is {@link Color#white}. */
    public void setColor(Color tint){
        colorPacked = tint.toFloatBits();
    }

    public float getPackedColor(){
        return colorPacked;
    }

    /**
     * Sets the color of this sprite cache, expanding the alpha from 0-254 to 0-255.
     * @see Color#toFloatBits()
     */
    public void setPackedColor(float packedColor){
        colorPacked = packedColor;
    }

    /** Starts the definition of a new cache, allowing the add and {@link #endCache()} methods to be called. */
    public void beginCache(){
        if(drawing) throw new IllegalStateException("end must be called before beginCache");
        if(currentCache != null) throw new IllegalStateException("endCache must be called before begin.");
        mesh.getVertices().position(caches.isEmpty() ? 0 : caches.peek().offset + caches.peek().maxCount);
        currentCache = new Cache(caches.size, mesh.getVertices().position());
        caches.add(currentCache);
        mesh.getVertices().limit(mesh.getVertices().capacity());
    }

    /**
     * Starts the redefinition of an existing cache, allowing the add and {@link #endCache()} methods to be called. It cannot have more entries added to it than when it was first created.
     * To do that, use {@link #clear()} and then {@link #begin()}.
     */
    public void beginCache(int cacheID){
        if(drawing) throw new IllegalStateException("end must be called before beginCache");
        if(currentCache != null) throw new IllegalStateException("endCache must be called before begin.");
        currentCache = caches.get(cacheID);
        Arrays.fill(currentCache.counts, 0);
        mesh.getVertices().position(currentCache.offset);
    }

    /** Ends the definition of a cache, returning the cache ID to be used with {@link #draw(int)}. */
    public int endCache(){
        if(currentCache == null) throw new IllegalStateException("beginCache must be called before endCache.");
        Cache cache = currentCache;
        int cacheCount = mesh.getVertices().position() - cache.offset;
        if(cache.textures == null){
            // New cache.
            cache.maxCount = cacheCount;
            cache.textureCount = textures.size;
            cache.textures = textures.toArray(Texture.class);
            cache.counts = new int[cache.textureCount];
            for(int i = 0, n = counts.size; i < n; i++)
                cache.counts[i] = counts.get(i);
        }else{
            // Redefine existing cache.
            if(cacheCount > cache.maxCount){
                throw new ArcRuntimeException(
                "If a cache is not the last created, it cannot be redefined with more entries than when it was first created: "
                + cacheCount + " (" + cache.maxCount + " max)");
            }

            cache.textureCount = textures.size;

            if(cache.textures.length < cache.textureCount) cache.textures = new Texture[cache.textureCount];
            for(int i = 0, n = cache.textureCount; i < n; i++)
                cache.textures[i] = textures.get(i);

            if(cache.counts.length < cache.textureCount) cache.counts = new int[cache.textureCount];
            for(int i = 0, n = cache.textureCount; i < n; i++)
                cache.counts[i] = counts.get(i);

            FloatBuffer vertices = mesh.getVertices();
            vertices.position(0);
            Cache lastCache = caches.get(caches.size - 1);
            vertices.limit(lastCache.offset + lastCache.maxCount);
        }

        currentCache = null;
        textures.clear();
        counts.clear();

        //fixes teaVM bug, since it draws based on offset apparently
        if(Core.app.isWeb()){
            mesh.getVertices().position(0);
        }

        return cache.id;
    }

    /** Invalidates all cache IDs and resets the SpriteCache so new caches can be added. */
    public void clear(){
        caches.clear();
        mesh.getVertices().clear().flip();
    }

    /** Ensures that this cache can hold this amount of sprites. Only call at the end of cache.
     * @return number of new sprites actually reserved. */
    public int reserve(int sprites){
        if(currentCache == null) throw new IllegalStateException("beginCache must be called before ensureSize.");

        //size of each sprite
        int spriteSize = vertexSize * (mesh.getNumIndices() > 0 ? 4 : 6);
        //currently used vertices
        int currentUsed = currentCache.maxCount;
        //vertices that need to be guaranteed
        int required = sprites * spriteSize;
        //number of extra vertices to reserve
        int toAdd = required - currentUsed;
        if(toAdd > 0){
            currentCache.maxCount += toAdd;
            mesh.getVertices().position(currentCache.offset + currentCache.maxCount);
            return toAdd / spriteSize;
        }
        return 0;
    }

    public int getSpritesUsed(){
        return (caches.isEmpty() ? 0 : caches.peek().offset + caches.peek().maxCount) / (vertexSize * (mesh.getNumIndices() > 0 ? 4 : 6));
    }

    public int getSpriteCapacity(){
        return mesh.getVertices().limit() / (vertexSize * (mesh.getNumIndices() > 0 ? 4 : 6));
    }

    /**
     * Adds the specified vertices to the cache. Each vertex should have 5 elements, one for each of the attributes: x, y, color,
     * u, and v. If indexed geometry is used, each image should be specified as 4 vertices, otherwise each image should be
     * specified as 6 vertices.
     */
    public void add(Texture texture, float[] vertices, int offset, int length){
        if(currentCache == null) throw new IllegalStateException("beginCache must be called before add.");
        if(mesh.getVertices().position() + length >= mesh.getVertices().limit())
            throw new IllegalStateException("Out of vertex space! Size: " + mesh.getVertices().capacity() + " Required: " + (mesh.getVertices().position() + length));

        int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
        int count = length / (verticesPerImage * vertexSize) * 6;
        int lastIndex = textures.size - 1;
        if(lastIndex < 0 || textures.get(lastIndex).getHandle() != texture.getHandle()){
            textures.add(texture);
            counts.add(count);
        }else
            counts.incr(lastIndex, count);

        mesh.getVertices().put(vertices, offset, length);
    }

    /** Adds the specified region to the cache. */
    public void add(TextureRegion region, float x, float y){
        add(region, x, y, region.width, region.height);
    }

    /** Adds the specified region to the cache. */
    public void add(TextureRegion region, float x, float y, float width, float height){
        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;
        float depth = region.getDepth();

        tempVertices[0] = x;
        tempVertices[1] = y;
        tempVertices[2] = u;
        tempVertices[3] = v;
        tempVertices[4] = depth;
        tempVertices[5] = colorPacked;

        tempVertices[6] = x;
        tempVertices[7] = fy2;
        tempVertices[8] = u;
        tempVertices[9] = v2;
        tempVertices[10] = depth;
        tempVertices[11] = colorPacked;

        tempVertices[12] = fx2;
        tempVertices[13] = fy2;
        tempVertices[14] = u2;
        tempVertices[15] = v2;
        tempVertices[16] = depth;
        tempVertices[17] = colorPacked;

        if(mesh.getNumIndices() > 0){
            tempVertices[18] = fx2;
            tempVertices[19] = y;
            tempVertices[20] = u2;
            tempVertices[21] = v;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;
            add(region.texture, tempVertices, 0, 24);
        }else{
            tempVertices[18] = fx2;
            tempVertices[19] = fy2;
            tempVertices[20] = u2;
            tempVertices[21] = v2;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;

            tempVertices[24] = fx2;
            tempVertices[25] = y;
            tempVertices[26] = u2;
            tempVertices[27] = v;
            tempVertices[28] = depth;
            tempVertices[29] = colorPacked;

            tempVertices[30] = x;
            tempVertices[31] = y;
            tempVertices[32] = u;
            tempVertices[33] = v;
            tempVertices[34] = depth;
            tempVertices[35] = colorPacked;
            add(region.texture, tempVertices, 0, 36);
        }
    }

    /** Adds the specified region to the cache. */
    public void add(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                    float scaleX, float scaleY, float rotation){

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if(scaleX != 1 || scaleY != 1){
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if(rotation != 0){
            final float cos = Mathf.cosDeg(rotation);
            final float sin = Mathf.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        }else{
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
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
        float depth = region.getDepth();

        tempVertices[0] = x1;
        tempVertices[1] = y1;
        tempVertices[2] = u;
        tempVertices[3] = v;
        tempVertices[4] = depth;
        tempVertices[5] = colorPacked;

        tempVertices[6] = x2;
        tempVertices[7] = y2;
        tempVertices[8] = u;
        tempVertices[9] = v2;
        tempVertices[10] = depth;
        tempVertices[11] = colorPacked;

        tempVertices[12] = x3;
        tempVertices[13] = y3;
        tempVertices[14] = u2;
        tempVertices[15] = v2;
        tempVertices[16] = depth;
        tempVertices[17] = colorPacked;

        if(mesh.getNumIndices() > 0){
            tempVertices[18] = x4;
            tempVertices[19] = y4;
            tempVertices[20] = u2;
            tempVertices[21] = v;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;
            add(region.texture, tempVertices, 0, 24);
        }else{
            tempVertices[18] = x3;
            tempVertices[19] = y3;
            tempVertices[20] = u2;
            tempVertices[21] = v2;
            tempVertices[22] = depth;
            tempVertices[23] = colorPacked;

            tempVertices[24] = x4;
            tempVertices[25] = y4;
            tempVertices[26] = u2;
            tempVertices[27] = v;
            tempVertices[28] = depth;
            tempVertices[29] = colorPacked;

            tempVertices[30] = x1;
            tempVertices[31] = y1;
            tempVertices[32] = u;
            tempVertices[33] = v;
            tempVertices[34] = depth;
            tempVertices[35] = colorPacked;
            add(region.texture, tempVertices, 0, 36);
        }
    }

    /** Prepares the OpenGL state for SpriteCache rendering. */
    public void begin(){
        begin(true);
    }

    /** Prepares the OpenGL state for SpriteCache rendering. */
    public void begin(boolean writeUniforms){
        if(drawing) throw new IllegalStateException("end must be called before begin.");
        if(currentCache != null) throw new IllegalStateException("endCache must be called before begin");
        renderCalls = 0;
        Shader shader = customShader != null ? customShader : this.shader;
        shader.bind();

        if(writeUniforms){
            combinedMatrix.set(projectionMatrix).mul(transformMatrix);
            shader.setUniformMatrix4("u_projectionViewMatrix", combinedMatrix);
        }

        mesh.bind(shader);
        drawing = true;
    }

    /** Completes rendering for this SpriteCache. */
    public void end(){
        if(!drawing) throw new IllegalStateException("begin must be called before end.");
        drawing = false;

        if(customShader != null)
            mesh.unbind(customShader);
        else
            mesh.unbind(shader);
    }

    /** Draws all the images defined for the specified cache ID. */
    public void draw(int cacheID){
        if(!drawing) throw new IllegalStateException("SpriteCache.begin must be called before draw.");

        Cache cache = caches.get(cacheID);
        int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
        int offset = cache.offset / (verticesPerImage * vertexSize) * 6;
        Texture[] textures = cache.textures;
        int[] counts = cache.counts;
        int textureCount = cache.textureCount;
        Shader shader = customShader != null ? customShader : this.shader;
        for(int i = 0; i < textureCount; i++){
            int count = counts[i];
            textures[i].bind();

            mesh.render(shader, Gl.triangles, offset, count, false);
            offset += count;
        }
        renderCalls += textureCount;
        totalRenderCalls += textureCount;
    }

    /**
     * Draws a subset of images defined for the specified cache ID.
     * @param offset The first image to render.
     * @param length The number of images from the first image (inclusive) to render.
     */
    public void draw(int cacheID, int offset, int length){
        if(!drawing) throw new IllegalStateException("SpriteCache.begin must be called before draw.");

        Cache cache = caches.get(cacheID);
        offset = offset * 6 + cache.offset;
        length *= 6;
        Texture[] textures = cache.textures;
        int[] counts = cache.counts;
        int textureCount = cache.textureCount;
        for(int i = 0; i < textureCount; i++){
            textures[i].bind();
            int count = counts[i];
            if(count > length){
                i = textureCount;
                count = length;
            }else
                length -= count;
            if(customShader != null)
                mesh.render(customShader, Gl.triangles, offset, count, false);
            else
                mesh.render(shader, Gl.triangles, offset, count, false);
            offset += count;
        }
        renderCalls += cache.textureCount;
        totalRenderCalls += textureCount;
    }

    /** Releases all resources held by this SpriteCache. */
    @Override
    public void dispose(){
        mesh.dispose();
        if(shader != null && shader != getDefaultShader()) shader.dispose();
    }

    public Mat getProjectionMatrix(){
        return projectionMatrix;
    }

    public void setProjectionMatrix(Mat projection){
        if(drawing) throw new IllegalStateException("Can't set the matrix within begin/end.");
        projectionMatrix.set(projection);
    }

    public Mat getTransformMatrix(){
        return transformMatrix;
    }

    public void setTransformMatrix(Mat transform){
        if(drawing) throw new IllegalStateException("Can't set the matrix within begin/end.");
        transformMatrix.set(transform);
    }

    /**
     * Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture
     * coordinates attribute is called called "a_texCoords", the color attribute is called "a_color". The projection matrix is
     * uploaded via a mat4 uniform called "u_proj", the transform matrix is uploaded via a uniform called "u_trans", the combined
     * transform and projection matrx is is uploaded via a mat4 uniform called "u_projTrans". The texture sampler is passed via a
     * uniform called "u_texture".
     * <p>
     * Call this method with a null argument to use the default shader.
     * @param shader the {@link Shader} or null to use the default shader.
     */
    public void setShader(Shader shader){
        customShader = shader;
    }

    public boolean isDrawing(){
        return drawing;
    }

    private static class Cache{
        final int id;
        final int offset;
        int maxCount;
        int textureCount;
        Texture[] textures;
        int[] counts;

        public Cache(int id, int offset){
            this.id = id;
            this.offset = offset;
        }
    }
}