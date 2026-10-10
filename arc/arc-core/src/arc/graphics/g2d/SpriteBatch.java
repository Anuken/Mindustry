package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Class for efficiently batching and sorting sprites.
 *
 * Sorting optimizations written by zxtej.
 * Significant request optimizations done by way-zer.
 * */
public class SpriteBatch extends Batch{
    private static final boolean validateDrawCalls = false;
    //xy + uv + depth + color + mix_color
    public static final int vertexSize = 2 + 2 + 1 + 1 + 1;
    public static final int spriteSize = 4 * vertexSize;

    private static final int initialSize = 10000;
    private static final float[] emptyVertices = new float[0];

    /** Counts up for every draw call. Only ever reset by the user. */
    public static long totalDrawCalls = 0;

    static ForkJoinHolder commonPool;
    boolean multithreaded = !OS.isIos && !OS.isAndroid;

    protected Shader normalShader, arrayShader;
    protected Mesh mesh;
    protected FloatBuffer buffer;

    final float[] tmpVertices = new float[spriteSize];

    float[] requestVerts = new float[initialSize * spriteSize];
    int requestVertOffset = 0;

    protected boolean sort, flushing;
    protected DrawRequest[] requests = new DrawRequest[initialSize], copy = new DrawRequest[0];
    protected int[] requestZ = new int[initialSize];
    protected int numRequests = 0;
    protected int[] contiguous = new int[2048], contiguousCopy = new int[2048];
    protected int intZ = Float.floatToRawIntBits(z + 16f);

    protected static class DrawRequest{
        int verticesOffset, verticesLength;
        Texture texture;
        Blending blending;
        Runnable run;
    }

    /**
     * Constructs a new SpriteBatch with a size of 4096, one buffer, and the default shader.
     */
    public SpriteBatch(){
        this(4096);
    }


    /**
     * Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
     * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
     * respect to the current screen resolution.
     * <p>
     * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
     * the ones expect for shaders set with {@link #setShader(Shader)}.
     * @param size The max number of sprites in a single batch. Max of 8191.
     */
    public SpriteBatch(int size){
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
        if(size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);

        if(size > 0){
            projectionMatrix.setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

            mesh = new Mesh(false, size * 4, size * 6,
            VertexAttribute.position,
            VertexAttribute.texCoords3,
            VertexAttribute.color,
            VertexAttribute.mixColor
            );

            int len = size * 6;
            short[] indices = new short[len];
            short j = 0;
            for(int i = 0; i < len; i += 6, j += 4){
                indices[i] = j;
                indices[i + 1] = (short)(j + 1);
                indices[i + 2] = (short)(j + 2);
                indices[i + 3] = (short)(j + 2);
                indices[i + 4] = (short)(j + 3);
                indices[i + 5] = j;
            }
            mesh.setIndices(indices);
            mesh.getVertices().position(0);
            mesh.getVertices().limit(mesh.getVertices().capacity());

            normalShader = createNormalShader();
            arrayShader = createArrayShader();

            //mark indices as dirty once for GL30
            mesh.getIndices();
            buffer = mesh.getVertices();
        }

        for(int i = 0; i < requests.length; i++){
            requests[i] = new DrawRequest();
        }

        if(multithreaded){
            try{
                commonPool = new ForkJoinHolder();
            }catch(Throwable t){
                multithreaded = false;
            }
        }
    }

    @Override
    public Shader getShader(){
        return customShader != null ? customShader : lastTexture instanceof ArraySliceTexture ? arrayShader : normalShader;
    }

    @Override
    protected void switchTexture(Texture texture){
        flush();
        lastTexture = texture;
    }

    @Override
    public void dispose(){
        if(normalShader != null) normalShader.dispose();
        if(arrayShader != null) arrayShader.dispose();

        if(mesh != null){
            mesh.dispose();
        }
    }

    @Override
    protected void setSort(boolean sort){
        if(this.sort != sort){
            flush();
        }
        this.sort = sort;
    }

    @Override
    protected void setShader(Shader shader, boolean apply){
        if(!flushing && sort){
            throw new IllegalArgumentException("Shaders cannot be set while sorting is enabled. Set shaders inside Draw.run(...).");
        }
        super.setShader(shader, apply);
    }

    @Override
    protected void setBlending(Blending blending){
        this.blending = blending;
    }

    @Override
    protected void z(float z){
        if(z == this.z) return;
        this.z = z;
        intZ = Float.floatToRawIntBits(z + 16f);
    }

    @Override
    protected void discard(){
        super.discard();

        buffer.position(0);
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        if(validateDrawCalls){
            if((count % spriteSize) != 0) throw new IllegalArgumentException("Invalid sprite vertex count (" + count + "); must be multiple of " + spriteSize);
        }
        totalDrawCalls += count / spriteSize;

        if(sort && !flushing){
            int num = numRequests;
            if(num > 0){
                final DrawRequest last = requests[num - 1];
                if(last.run == null && (last.texture.getHandle() == texture.getHandle()) && last.blending == blending && requestZ[num - 1] == intZ){
                    if(spriteVertices != emptyVertices){
                        prepare(count);
                        System.arraycopy(spriteVertices, offset, requestVerts, requestVertOffset, count);
                        requestVertOffset += count;
                    }
                    last.verticesLength += count;
                    return;
                }
            }
            if(num >= this.requests.length) expandRequests();
            final DrawRequest req = requests[num];
            if(spriteVertices != emptyVertices){
                req.verticesOffset = requestVertOffset;
                prepare(count);
                System.arraycopy(spriteVertices, offset, requestVerts, requestVertOffset, count);
                requestVertOffset += count;
            }else{
                req.verticesOffset = offset;
            }
            req.verticesLength = count;
            requestZ[num] = intZ;
            req.texture = texture;
            req.blending = blending;
            req.run = null;
            numRequests++;
        }else{
            drawSuper(texture, spriteVertices, offset, count);
        }
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        if(!sort || flushing){
            drawSuper(region, x, y, originX, originY, width, height, rotation);
            return;
        }
        int pos = this.requestVertOffset;
        this.requestVertOffset += spriteSize;
        prepare(spriteSize);
        constructVertices(this.requestVerts, pos, region, x, y, originX, originY, width, height, rotation);
        draw(region.texture, emptyVertices, pos, spriteSize);
    }

    @Override
    protected void draw(Runnable request){
        totalDrawCalls ++; //not accurate since there can be multiple draws inside the runnable, but it's the best that can be done

        if(sort && !flushing){
            if(numRequests >= requests.length) expandRequests();
            final SpriteBatch.DrawRequest req = requests[numRequests];
            req.run = request;
            req.blending = blending;
            requestZ[numRequests] = intZ;
            req.texture = null;
            numRequests++;
        }else{
            request.run();
        }
    }

    protected void prepare(int i){
        if(requestVertOffset + i >= requestVerts.length) requestVerts = Arrays.copyOf(requestVerts, requestVerts.length << 1);
    }

    protected void expandRequests(){
        final DrawRequest[] requests = this.requests, newRequests = Arrays.copyOf(requests, requests.length * 7 / 4);
        for(int i = requests.length; i < newRequests.length; i++){
            newRequests[i] = new DrawRequest();
        }
        this.requests = newRequests;
        this.requestZ = Arrays.copyOf(requestZ, newRequests.length);
    }

    @Override
    protected void flush(){
        if(!flushing){
            flushing = true;
            flushRequests();
            flushing = false;
        }

        if(idx == 0) return;

        getShader().bind();
        setupMatrices();

        if(customShader != null && apply){
            customShader.apply();
        }

        Gl.depthMask(false);
        int count = idx / spriteSize * 6;

        blending.apply();

        lastTexture.bind();
        Mesh mesh = this.mesh;
        //calling buffer() marks it as dirty, so it gets reuploaded upon render
        mesh.getVertices();

        buffer.position(0);
        buffer.limit(idx);

        mesh.render(getShader(), Gl.triangles, 0, count);

        buffer.limit(buffer.capacity());
        buffer.position(0);

        idx = 0;
    }

    protected void flushRequests(){
        if(numRequests == 0) return;
        sortRequests();
        float preColor = colorPacked, preMixColor = mixColorPacked;
        Blending preBlending = blending;

        float[] vertices = this.requestVerts;
        DrawRequest[] r = copy;
        int num = numRequests;
        for(int j = 0; j < num; j++){
            final DrawRequest req = r[j];

            super.setBlending(req.blending);

            if(req.run != null){
                req.run.run();
                req.run = null;
            }else if(req.texture != null){
                drawSuper(req.texture, vertices, req.verticesOffset, req.verticesLength);
            } // the request is invalid, but crashing wouldn't be very nice, so it is simply ignored
        }

        colorPacked = preColor;
        mixColorPacked = preMixColor;
        blending = preBlending;

        numRequests = 0;
        requestVertOffset = 0;
    }

    protected void drawSuper(Texture texture, float[] spriteVertices, int offset, int count){
        if(validateDrawCalls){
            if((count % spriteSize) != 0) throw new IllegalArgumentException("Invalid sprite vertex count (" + count + "); must be multiple of " + spriteSize);
        }

        int verticesLength = buffer.capacity();
        int remainingVertices = verticesLength;
        if(lastTexture == null || texture.getHandle() != lastTexture.getHandle()){
            switchTexture(texture);
        }else{
            remainingVertices -= idx;
            if(remainingVertices == 0){
                flush();
                remainingVertices = verticesLength;
            }
        }
        int copyCount = Math.min(remainingVertices, count);

        buffer.put(spriteVertices, offset, copyCount);

        idx += copyCount;
        count -= copyCount;
        while(count > 0){
            offset += copyCount;
            flush();
            copyCount = Math.min(verticesLength, count);
            buffer.put(spriteVertices, offset, copyCount);
            idx += copyCount;
            count -= copyCount;
        }
    }

    protected void drawSuper(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){

        Texture texture = region.texture;
        if(lastTexture == null || texture.getHandle() != lastTexture.getHandle()){
            switchTexture(texture);
        }else if(idx == buffer.capacity()){
            flush();
        }

        this.idx += spriteSize;
        constructVertices(this.tmpVertices, 0, region, x, y, originX, originY, width, height, rotation);
        buffer.put(tmpVertices);
    }

    protected final void constructVertices(float[] vertices, int idx, TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float depth = region.getDepth();

        float color = this.colorPacked;
        float mixColor = this.mixColorPacked;

        if(!Mathf.zero(rotation)){
            //bottom left and top right corner points relative to origin
            float worldOriginX = x + originX;
            float worldOriginY = y + originY;
            float fx = -originX;
            float fy = -originY;
            float fx2 = width - originX;
            float fy2 = height - originY;

            // rotate
            float cos = Mathf.cosDeg(rotation);
            float sin = Mathf.sinDeg(rotation);

            float x1 = cos * fx - sin * fy + worldOriginX;
            float y1 = sin * fx + cos * fy + worldOriginY;
            float x2 = cos * fx - sin * fy2 + worldOriginX;
            float y2 = sin * fx + cos * fy2 + worldOriginY;
            float x3 = cos * fx2 - sin * fy2 + worldOriginX;
            float y3 = sin * fx2 + cos * fy2 + worldOriginY;
            float x4 = x1 + (x3 - x2);
            float y4 = y3 - (y2 - y1);

            vertices[idx] = x1;
            vertices[idx + 1] = y1;
            vertices[idx + 2] = u;
            vertices[idx + 3] = v;
            vertices[idx + 4] = depth;
            vertices[idx + 5] = color;
            vertices[idx + 6] = mixColor;

            vertices[idx + 7] = x2;
            vertices[idx + 8] = y2;
            vertices[idx + 9] = u;
            vertices[idx + 10] = v2;
            vertices[idx + 11] = depth;
            vertices[idx + 12] = color;
            vertices[idx + 13] = mixColor;

            vertices[idx + 14] = x3;
            vertices[idx + 15] = y3;
            vertices[idx + 16] = u2;
            vertices[idx + 17] = v2;
            vertices[idx + 18] = depth;
            vertices[idx + 19] = color;
            vertices[idx + 20] = mixColor;

            vertices[idx + 21] = x4;
            vertices[idx + 22] = y4;
            vertices[idx + 23] = u2;
            vertices[idx + 24] = v;
            vertices[idx + 25] = depth;
            vertices[idx + 26] = color;
            vertices[idx + 27] = mixColor;
        }else{
            float fx2 = x + width;
            float fy2 = y + height;

            vertices[idx] = x;
            vertices[idx + 1] = y;
            vertices[idx + 2] = u;
            vertices[idx + 3] = v;
            vertices[idx + 4] = depth;
            vertices[idx + 5] = color;
            vertices[idx + 6] = mixColor;

            vertices[idx + 7] = x;
            vertices[idx + 8] = fy2;
            vertices[idx + 9] = u;
            vertices[idx + 10] = v2;
            vertices[idx + 11] = depth;
            vertices[idx + 12] = color;
            vertices[idx + 13] = mixColor;

            vertices[idx + 14] = fx2;
            vertices[idx + 15] = fy2;
            vertices[idx + 16] = u2;
            vertices[idx + 17] = v2;
            vertices[idx + 18] = depth;
            vertices[idx + 19] = color;
            vertices[idx + 20] = mixColor;

            vertices[idx + 21] = fx2;
            vertices[idx + 22] = y;
            vertices[idx + 23] = u2;
            vertices[idx + 24] = v;
            vertices[idx + 25] = depth;
            vertices[idx + 26] = color;
            vertices[idx + 27] = mixColor;
        }
    }

    public static Shader createNormalShader(){
        return new Shader(
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "attribute vec4 a_mix_color;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec4 v_mix_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "\n" +
        "void main(){\n" +
        "   v_color = a_color;\n" +
        "   v_color.a = v_color.a * (255.0/254.0);\n" +
        "   v_mix_color = a_mix_color;\n" +
        "   v_mix_color.a *= (255.0/254.0);\n" +
        "   v_texCoords = a_texCoord0;\n" +
        "   gl_Position = u_projTrans * a_position;\n" +
        "}",

        "\n" +
        "varying lowp vec4 v_color;\n" +
        "varying lowp vec4 v_mix_color;\n" +
        "varying highp vec2 v_texCoords;\n" +
        "uniform highp sampler2D u_texture;\n" +
        "\n" +
        "void main(){\n" +
        "  vec4 c = texture2D(u_texture, v_texCoords);\n" +
        "  gl_FragColor = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);\n" +
        "}"
        );
    }

    public static Shader createArrayShader(){
        return new Shader(
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec3 a_texCoord0;\n" +
        "attribute vec4 a_mix_color;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec4 v_mix_color;\n" +
        "varying vec3 v_texCoords;\n" +
        "\n" +
        "void main(){\n" +
        "   v_color = a_color;\n" +
        "   v_color.a = v_color.a * (255.0/254.0);\n" +
        "   v_mix_color = a_mix_color;\n" +
        "   v_mix_color.a *= (255.0/254.0);\n" +
        "   v_texCoords = a_texCoord0;\n" +
        "   gl_Position = u_projTrans * a_position;\n" +
        "}",

        "\n" +
        "varying lowp vec4 v_color;\n" +
        "varying lowp vec4 v_mix_color;\n" +
        "varying highp vec3 v_texCoords;\n" +
        "uniform highp sampler2DArray u_texture;\n" +
        "\n" +
        "void main(){\n" +
        "  vec4 c = texture(u_texture, v_texCoords);\n" +
        "  gl_FragColor = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);\n" +
        "}"
        );
    }

    //region request sorting

    protected void sortRequests(){
        if(multithreaded){
            sortRequestsThreaded();
        }else{
            sortRequestsStandard();
        }
    }

    protected void sortRequestsThreaded(){
        final int numRequests = this.numRequests;
        final int[] itemZ = requestZ;

        int[] contiguous = this.contiguous;
        int ci = 0, cl = contiguous.length;
        int z = itemZ[0];
        int startI = 0;
        // Point3: <z, index, length>
        for(int i = 1; i < numRequests; i++){
            if(itemZ[i] != z){ // if contiguous section should end
                contiguous[ci] = z;
                contiguous[ci + 1] = startI;
                contiguous[ci + 2] = i - startI;
                ci += 3;
                if(ci + 3 > cl){
                    contiguous = Arrays.copyOf(contiguous, cl <<= 1);
                }
                z = itemZ[startI = i];
            }
        }
        contiguous[ci] = z;
        contiguous[ci + 1] = startI;
        contiguous[ci + 2] = numRequests - startI;
        this.contiguous = contiguous;

        final int L = (ci / 3) + 1;

        if(contiguousCopy.length < contiguous.length) this.contiguousCopy = new int[contiguous.length];

        final int[] sorted = CountingSort.countingSortMapMT(contiguous, contiguousCopy, L);


        final int[] locs = contiguous;
        locs[0] = 0;
        for(int i = 0, ptr = 0; i < L; i++){
            ptr += sorted[i * 3 + 2];
            locs[i + 1] = ptr;
        }
        if(copy.length < requests.length) copy = new DrawRequest[requests.length];
        PopulateTask.tasks = sorted;
        PopulateTask.src = requests;
        PopulateTask.dest = copy;
        PopulateTask.locs = locs;
        commonPool.pool.invoke(new PopulateTask(0, L));
    }

    protected void sortRequestsStandard(){ // Non-threaded implementation for weak devices
        final int numRequests = this.numRequests;
        final int[] itemZ = requestZ;
        int[] contiguous = this.contiguous;
        int ci = 0, cl = contiguous.length;
        int z = itemZ[0];
        int startI = 0;
        // Point3: <z, index, length>
        for(int i = 1; i < numRequests; i++){
            if(itemZ[i] != z){ // if contiguous section should end
                contiguous[ci] = z;
                contiguous[ci + 1] = startI;
                contiguous[ci + 2] = i - startI;
                ci += 3;
                if(ci + 3 > cl){
                    contiguous = Arrays.copyOf(contiguous, cl <<= 1);
                }
                z = itemZ[startI = i];
            }
        }
        contiguous[ci] = z;
        contiguous[ci + 1] = startI;
        contiguous[ci + 2] = numRequests - startI;
        this.contiguous = contiguous;

        final int L = (ci / 3) + 1;

        if(contiguousCopy.length < contiguous.length) contiguousCopy = new int[contiguous.length];

        final int[] sorted = CountingSort.countingSortMap(contiguous, contiguousCopy, L);

        if(copy.length < numRequests) copy = new DrawRequest[numRequests + (numRequests >> 3)];
        int ptr = 0;
        final DrawRequest[] items = requests, dest = copy;
        for(int i = 0; i < L * 3; i += 3){
            final int pos = sorted[i + 1], length = sorted[i + 2];
            if(length < 10){
                final int end = pos + length;
                for(int sj = pos, dj = ptr; sj < end; sj++, dj++){
                    dest[dj] = items[sj];
                }
            }else System.arraycopy(items, pos, dest, ptr, Math.min(length, dest.length - ptr));
            ptr += length;
        }
    }

    static class CountingSort{
        private static final int processors = Runtime.getRuntime().availableProcessors() * 8;

        static int[] locs = new int[100];
        static final int[][] locses = new int[processors][100];

        static final IntIntMap[] countses = new IntIntMap[processors];

        private static int[] entries3 = new int[300], entries3a = new int[300];

        //packed (key << 32 | payload) longs, used to order unique z-keys without boxing
        private static long[] packedKeys = new long[100];
        private static long[] packedKeysMT = new long[100];

        private static final CountingSort.CountingSortTask[] tasks = new CountingSort.CountingSortTask[processors];
        private static final CountingSort.CountingSortTask2[] task2s = new CountingSort.CountingSortTask2[processors];
        private static final Future<?>[] futures = new Future<?>[processors];

        static{
            for(int i = 0; i < countses.length; i++) countses[i] = new IntIntMap();

            for(int i = 0; i < processors; i++){
                tasks[i] = new CountingSort.CountingSortTask();
                task2s[i] = new CountingSort.CountingSortTask2();
            }
        }

        static class CountingSortTask implements Runnable{
            static int[] arr;
            int start, end, id;

            public void set(int start, int end, int id){
                this.start = start;
                this.end = end;
                this.id = id;
            }

            @Override
            public void run(){
                final int id = this.id, start = this.start, end = this.end;
                int[] locs = locses[id];
                final int[] arr = CountingSort.CountingSortTask.arr;
                final IntIntMap counts = countses[id];
                counts.clear();
                int unique = 0;
                for(int i = start; i < end; i++){
                    int loc = counts.putMissing(arr[i * 3], unique);
                    arr[i * 3] = loc;
                    if(loc == unique){
                        if(unique >= locs.length){
                            locs = Arrays.copyOf(locs, unique * 3 / 2);
                        }
                        locs[unique++] = 1;
                    }else{
                        locs[loc]++;
                    }
                }
                locses[id] = locs;
            }
        }

        static class CountingSortTask2 implements Runnable{
            static int[] src, dest;
            int start, end, id;

            public void set(int start, int end, int id){
                this.start = start;
                this.end = end;
                this.id = id;
            }

            @Override
            public void run(){
                final int start = this.start, end = this.end;
                final int[] locs = locses[id];
                final int[] src = CountingSort.CountingSortTask2.src, dest = CountingSort.CountingSortTask2.dest;
                for(int i = end - 1, i3 = i * 3; i >= start; i--, i3 -= 3){
                    final int destPos = --locs[src[i3]] * 3;
                    dest[destPos] = src[i3];
                    dest[destPos + 1] = src[i3 + 1];
                    dest[destPos + 2] = src[i3 + 2];
                }
            }
        }

        static int[] countingSortMapMT(final int[] arr, final int[] swap, final int end){
            final IntIntMap[] countses = CountingSort.countses;
            final int[][] locs = CountingSort.locses;
            final int threads = Math.min(processors, (end + 4095) / 4096); // 4096 Point3s to process per thread
            final int thread_size = end / threads + 1;
            final CountingSort.CountingSortTask[] tasks = CountingSort.tasks;
            final CountingSort.CountingSortTask2[] task2s = CountingSort.task2s;
            final Future<?>[] futures = CountingSort.futures;
            CountingSort.CountingSortTask.arr = CountingSort.CountingSortTask2.src = arr;
            CountingSort.CountingSortTask2.dest = swap;

            for(int s = 0, thread = 0; thread < threads; thread++, s += thread_size){
                CountingSort.CountingSortTask task = tasks[thread];
                final int stop = Math.min(s + thread_size, end);
                task.set(s, stop, thread);
                task2s[thread].set(s, stop, thread);
                futures[thread] = commonPool.pool.submit(task);
            }

            int unique = 0;
            for(int i = 0; i < threads; i++){
                try{
                    futures[i].get();
                }catch(ExecutionException | InterruptedException e){
                    commonPool.pool.execute(tasks[i]);
                }
                unique += countses[i].size;
            }

            final int L = unique;
            if(entries3.length < L * 3){
                entries3 = new int[L * 3 * 3 / 2];
                entries3a = new int[L * 3 * 3 / 2];
            }
            if(packedKeysMT.length < L){
                packedKeysMT = new long[L * 3 / 2];
            }
            final int[] entries = CountingSort.entries3, entries3a = CountingSort.entries3a;
            final long[] packed = CountingSort.packedKeysMT;

            int j = 0;
            for(int i = 0; i < threads; i++){
                if(countses[i].size == 0) continue;
                final IntIntMap.Entries countEntries = countses[i].entries();
                final IntIntMap.Entry entry = countEntries.next();
                entries[j] = entry.key;
                entries[j + 1] = entry.value;
                entries[j + 2] = i;
                j += 3;
                while(countEntries.hasNext){
                    countEntries.next();
                    entries[j] = entry.key;
                    entries[j + 1] = entry.value;
                    entries[j + 2] = i;
                    j += 3;
                }
            }

            //argsort the L unique entries by key: pack (key, original index) into one long
            for(int i = 0; i < L; i++){
                packed[i] = (((long)entries[i * 3]) << 32) | (i & 0xFFFFFFFFL);
            }
            Arrays.sort(packed, 0, L);

            for(int i = 0; i < L; i++){
                int from = (int)packed[i] * 3, to = i * 3;
                entries3a[to] = entries[from];
                entries3a[to + 1] = entries[from + 1];
                entries3a[to + 2] = entries[from + 2];
            }

            for(int i = 0, pos = 0; i < L * 3; i += 3){
                pos = (locs[entries3a[i + 2]][entries3a[i + 1]] += pos);
            }

            for(int thread = 0; thread < threads; thread++){
                futures[thread] = commonPool.pool.submit(task2s[thread]);
            }
            for(int i = 0; i < threads; i++){
                try{
                    futures[i].get();
                }catch(ExecutionException | InterruptedException e){
                    commonPool.pool.execute(task2s[i]);
                }
            }
            return swap;
        }

        static int[] countingSortMap(final int[] arr, final int[] swap, final int end){
            int[] locs = CountingSort.locs;
            final IntIntMap counts = CountingSort.countses[0];
            counts.clear();

            int unique = 0;
            final int end3 = end * 3;
            for(int i = 0; i < end3; i += 3){
                int loc = counts.putMissing(arr[i], unique);
                arr[i] = loc;
                if(loc == unique){
                    if(unique >= locs.length){
                        locs = Arrays.copyOf(locs, unique * 3 / 2);
                    }
                    locs[unique++] = 1;
                }else{
                    locs[loc]++;
                }
            }
            CountingSort.locs = locs;

            if(packedKeys.length < unique){
                packedKeys = new long[unique * 3 / 2];
            }
            final long[] packed = CountingSort.packedKeys;

            //pack (key, compactIndex) for every unique z-key
            final IntIntMap.Entries countEntries = counts.entries();
            final IntIntMap.Entry entry = countEntries.next();
            packed[0] = (((long)entry.key) << 32) | (entry.value & 0xFFFFFFFFL);
            int j = 1;
            while(countEntries.hasNext){
                countEntries.next(); // it returns the same entry over and over again.
                packed[j++] = (((long)entry.key) << 32) | (entry.value & 0xFFFFFFFFL);
            }

            //primitive sort: high 32 bits (key) fully determine order since keys are unique
            Arrays.sort(packed, 0, unique);

            int prev = (int)packed[0], next;
            for(int i = 1; i < unique; i++){
                next = (int)packed[i];
                locs[next] += locs[prev];
                prev = next;
            }
            for(int i = end - 1, i3 = i * 3; i >= 0; i--, i3 -= 3){
                final int destPos = --locs[arr[i3]] * 3;
                swap[destPos] = arr[i3];
                swap[destPos + 1] = arr[i3 + 1];
                swap[destPos + 2] = arr[i3 + 2];
            }
            return swap;
        }
    }

    static class PopulateTask extends RecursiveAction{
        int from, to;
        static int[] tasks;
        static DrawRequest[] src;
        static DrawRequest[] dest;
        static int[] locs;

        //private static final int threshold = 256;
        PopulateTask(int from, int to){
            this.from = from;
            this.to = to;
        }

        public PopulateTask(){
        }

        @Override
        protected void compute(){
            final int[] locs = PopulateTask.locs;
            if(to - from > 1 && locs[to] - locs[from] > 2048){
                final int half = (locs[to] + locs[from]) >> 1;
                int mid = Arrays.binarySearch(locs, from, to, half);
                if(mid < 0) mid = -mid - 1;
                if(mid != from && mid != to){
                    invokeAll(new PopulateTask(from, mid), new PopulateTask(mid, to));
                    return;
                }
            }
            final DrawRequest[] src = PopulateTask.src, dest = PopulateTask.dest;
            final int[] tasks = PopulateTask.tasks;
            for(int i = from; i < to; i++){
                final int point = i * 3, pos = tasks[point + 1], length = tasks[point + 2];
                if(length < 10){
                    final int end = pos + length;
                    for(int sj = pos, dj = locs[i]; sj < end; sj++, dj++){
                        dest[dj] = src[sj];
                    }
                }else{
                    System.arraycopy(src, pos, dest, locs[i], Math.min(length, dest.length - locs[i]));
                }
            }
        }
    }

    //endregion
}
