package arc.graphics.g2d;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;

import static arc.Core.*;

public class Draw{
    private static ScreenQuad squad;

    private static final Color[] carr = new Color[3];
    private static final float[] vertices = new float[SpriteBatch.spriteSize];
    private static @Nullable FloatFloatf zTransformer;
    private static float actualZ;
    private static Color retColor = new Color(), retPackedColor = new Color();

    public static float scl = 1f;
    public static float xscl = 1f, yscl = 1f;

    private static ScreenQuad getQuad(){
        if(squad == null) squad = new ScreenQuad();
        return squad;
    }

    /** Blits an already-bound texture onto the screen with a shader.
     * This does not use a spritebatch! */
    public static void blit(Shader shader){
        shader.bind();
        shader.apply();
        getQuad().render(shader);
    }

    /** Blits a texture onto the screen with a shader.
     * This does not use a spritebatch! */
    public static void blit(Texture texture, Shader shader){
        texture.bind(0);
        shader.bind();
        shader.apply();
        getQuad().render(shader);
    }

    /** Blits a framebuffer onto the screen with a shader.
     * This does not use a spritebatch! */
    public static void blit(FrameBuffer buffer, Shader shader){
        blit(buffer.texture, shader);
    }

    public static void batch(Batch nextBatch){
        flush();
        Core.batch = nextBatch;
    }

    public static void batch(Batch nextBatch, Runnable run){
        Batch prev = Core.batch;
        prev.flush();

        Core.batch = nextBatch;

        run.run();

        nextBatch.flush();
        Core.batch = prev;
    }

    public static void stencil(Runnable stencil, Runnable contents){
        beginStencil();

        stencil.run();

        beginStenciled();

        contents.run();

        endStencil();
    }

    public static void beginStencil(){
        flush();

        Gl.stencilMask(0xFF);
        Gl.colorMask(false, false, false, false);
        Gl.enable(Gl.stencilTest);
        Gl.stencilFunc(Gl.always, 1, 0xFF);
        Gl.stencilMask(0xFF);
        Gl.stencilOp(Gl.replace, Gl.replace, Gl.replace);
    }

    public static void beginStenciled(){
        flush();

        Gl.stencilOp(Gl.keep, Gl.keep, Gl.keep);
        Gl.colorMask(true, true, true, true);
        Gl.stencilFunc(Gl.equal, 1, 0xFF);
    }

    public static void endStencil(){
        flush();

        Gl.disable(Gl.stencilTest);
    }

    public static void scl(float nscl){
        scl(nscl, nscl);
    }

    public static void scl(float nxscl, float nyscl){
        xscl = nxscl;
        yscl = nyscl;
    }

    public static void scl(){
        xscl = yscl = 1f;
    }

    public static Shader getShader(){
        return Core.batch.getShader();
    }

    public static void shader(Shader shader){
        shader(shader, true);
    }

    public static void shader(Shader shader, boolean apply){
        Core.batch.setShader(shader, apply);
    }

    public static void shader(){
        Core.batch.setShader(null);
    }

    /** Note that sorting is disabled by default, even if it is supported. */
    public static void sort(boolean sort){
        batch.setSort(sort);
    }

    /** Sets a Z-transformer function that will modify subsequent calls to Draw.z. Yep, this is as terrible as it sounds. */
    public static void zTransform(FloatFloatf f){
        zTransformer = f;
    }

    public static void zTransform(){
        zTransform(null);
    }

    public static float z(){
        return actualZ;
    }

    /** Note that this does nothing on most Batch implementations. */
    public static void z(float z){
        Core.batch.z(zTransformer == null ? actualZ = z : zTransformer.get(actualZ = z));
    }

    public static float getColorAlpha(){
        int abgr = Color.floatToIntColor(batch.getPackedColor());
        return ((abgr & 0xff000000) >>> 24) / 255f;
    }

    public static float getColorPacked(){
        return batch.getPackedColor();
    }

    public static float getMixColorPacked(){
        return batch.getPackedMixColor();
    }

    public static Color getColor(){
        return retColor.abgr8888(batch.getPackedColor());
    }

    public static Color getMixColor(){
        return retPackedColor.abgr8888(batch.getPackedMixColor());
    }

    public static void mixcol(Color color, float a){
        Core.batch.setPackedMixColor(Color.toFloatBits(color.r, color.g, color.b, Mathf.clamp(a)));
    }

    public static void mixcol(Color a, Color b, float prog){
        Core.batch.setPackedMixColor(Tmp.c1.set(a).lerp(b, prog).toFloatBits());
    }

    public static void mixcol(){
        Core.batch.setPackedMixColor(Color.clearFloatBits);
    }

    public static void mixcol(float color){
        Core.batch.setPackedMixColor(color);
    }

    public static void tint(Color a, Color b, float s){
        Tmp.c1.set(a).lerp(b, s);
        Tmp.c1.a = getColorAlpha();
        color(Tmp.c1);
    }

    public static void tint(Color color){
        Core.batch.setPackedColor(Color.toFloatBits(color.r, color.g, color.b, getColorAlpha()));
    }

    public static void colorMul(Color color, float mul){
        color(color.r * mul, color.g * mul, color.b * mul, 1f);
    }

    public static void color(Color color){
        Core.batch.setPackedColor(color.toFloatBits());
    }

    public static void color(Color color, float alpha){
        Core.batch.setPackedColor(Color.toFloatBits(color.r, color.g, color.b, Mathf.clamp(alpha)));
    }

    public static void color(int color){
        Core.batch.setPackedColor(Tmp.c1.rgba8888(color).toFloatBits());
    }

    public static void color(float color){
        Core.batch.setPackedColor(color);
    }

    public static void color(Color a, Color b, Color c, float progress){
        carr[0] = a;
        carr[1] = b;
        carr[2] = c;
        color(Tmp.c1.lerp(carr, progress));
    }

    /** Automatically mixes colors. */
    public static void color(Color a, Color b, float s){
        Core.batch.setPackedColor(Tmp.c1.set(a).lerp(b, s).toFloatBits());
    }

    public static void color(){
        Core.batch.setPackedColor(Color.whiteFloatBits);
    }

    public static void color(float r, float g, float b){
        Core.batch.setPackedColor(Color.toFloatBits(r, g, b, 1f));
    }

    public static void color(float r, float g, float b, float a){
        Core.batch.setPackedColor(Color.toFloatBits(r, g, b, Mathf.clamp(a)));
    }

    /** Lightness color. */
    public static void colorl(float l){
        color(l, l, l);
    }

    /** Lightness color, alpha. */
    public static void colorl(float l, float a){
        color(l, l, l, a);
    }

    public static void blend(Blending blending){
        Core.batch.setBlending(blending);
    }

    public static void blend(){
        blend(Blending.normal);
    }

    public static Blending getBlend(){
        return batch.getBlending();
    }

    public static void reset(){
        color();
        mixcol();
        xscl = yscl = 1f;
        Lines.stroke(1f);
    }

    public static void alpha(float alpha){
        int abgr = Color.floatToIntColor(batch.getPackedColor());
        int rawAlpha = (int)(Mathf.clamp(alpha) * 255);

        batch.setPackedColor(Color.intToFloatColor((abgr & 0x00ffffff) | (rawAlpha << 24)));
    }

    /** Draws a portion of a world-sized texture. */
    public static void fbo(FrameBuffer buffer, int worldWidth, int worldHeight, int tilesize){
        fbo(buffer.texture, worldWidth, worldHeight, tilesize);
    }

    /** Draws a portion of a world-sized texture. */
    public static void fbo(Texture texture, int worldWidth, int worldHeight, int tilesize){
        fbo(texture, worldWidth, worldHeight, tilesize, 0f);
    }

    /** Draws a portion of a world-sized texture. */
    public static void fbo(Texture texture, int worldWidth, int worldHeight, int tilesize, float offset){
        float ww = worldWidth * tilesize, wh = worldHeight * tilesize;
        float x = camera.position.x + offset, y = camera.position.y + offset;
        float u = (x - camera.width / 2f) / ww,
        v = (y - camera.height / 2f) / wh,
        u2 = (x + camera.width / 2f) / ww,
        v2 = (y + camera.height / 2f) / wh;

        Tmp.tr1.set(texture);
        Tmp.tr1.set(u, v2, u2, v);

        Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
    }

    /** On a sorting or queued batch implementation, this treats everything inside the runnable as one unit.
     * Thus, it can be used to set shaders and do other special state. */
    public static void draw(float z, Runnable run){
        z(z);
        batch.draw(run);
    }

    /** Applies runnables for the begin and end of a specific Z value.
     * Useful for framebuffers or batched shader begin/ends. */
    public static void drawRange(float z, Runnable begin, Runnable end){
        drawRange(z, 0.001f, begin, end);
    }

    /** Applies runnables for the begin and end of a specific Z value.
     * Useful for framebuffers or batched shader begin/ends. */
    public static void drawRange(float z, float range, Runnable begin, Runnable end){
        draw(z - range, begin);
        draw(z + range, end);
    }

    public static void quad(TextureRegion region, float x1, float y1, float c1, float x2, float y2, float c2, float x3, float y3, float c3, float x4, float y4, float c4){
        float mcolor = Core.batch.getPackedMixColor();
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float depth = region.getDepth();

        vertices[0]  = x1;
        vertices[1]  = y1;
        vertices[2]  = u;
        vertices[3]  = v;
        vertices[4]  = depth;
        vertices[5]  = c1;
        vertices[6]  = mcolor;

        vertices[7]  = x2;
        vertices[8]  = y2;
        vertices[9]  = u;
        vertices[10] = v2;
        vertices[11] = depth;
        vertices[12] = c2;
        vertices[13] = mcolor;

        vertices[14] = x3;
        vertices[15] = y3;
        vertices[16] = u2;
        vertices[17] = v2;
        vertices[18] = depth;
        vertices[19] = c3;
        vertices[20] = mcolor;

        vertices[21] = x4;
        vertices[22] = y4;
        vertices[23] = u2;
        vertices[24] = v;
        vertices[25] = depth;
        vertices[26] = c4;
        vertices[27] = mcolor;

        Draw.vert(region.texture, vertices, 0, vertices.length);
    }

    /** Fill a white quad to the camera. */
    public static void rect(){
        Fill.rect(camera.position.x, camera.position.y, camera.width, camera.height);
    }

    public static void rect(String region, float x, float y, float w, float h){
        rect(Core.atlas.find(region), x, y, w, h);
    }

    public static void rect(TextureRegion region, float x, float y, float w, float h){
        Core.batch.draw(region, x - w /2f, y - h /2f, 0, 0, w, h, 0);
    }

    public static void rect(TextureRegion region, float x, float y){
        rect(region, x, y, region.width * region.scl() * xscl, region.height * region.scl() * yscl);
    }

    public static void rect(String region, float x, float y){
        rect(Core.atlas.find(region), x, y);
    }

    public static void rect(TextureRegion region, float x, float y, float w, float h, float originX, float originY, float rotation){
        Core.batch.draw(region, x - w /2f, y - h /2f, originX, originY, w, h, rotation);
    }

    public static void rect(String region, float x, float y, float w, float h, float originX, float originY, float rotation){
        Core.batch.draw(Core.atlas.find(region), x - w /2f, y - h /2f, originX, originY, w, h, rotation);
    }

    public static void rect(TextureRegion region, float x, float y, float w, float h, float rotation){
        rect(region, x, y, w, h, w/2f, h/2f, rotation);
    }

    public static void rect(String region, float x, float y, float w, float h, float rotation){
        rect(Core.atlas.find(region), x, y, w, h, w/2f, h/2f, rotation);
    }

    public static void rect(TextureRegion region, Position pos, float w, float h){
        rect(region, pos.getX(), pos.getY(), w, h);
    }

    public static void rect(TextureRegion region, Position pos, float w, float h, float rotation){
        rect(region, pos.getX(), pos.getY(), w, h, rotation);
    }

    public static void rect(TextureRegion region, Position pos, float rotation){
        rect(region, pos.getX(), pos.getY(), rotation);
    }

    public static void rect(TextureRegion region, float x, float y, float rotation){
        rect(region, x, y, region.width * region.scl() * xscl, region.height * region.scl() * yscl, rotation);
    }

    public static void rect(String region, float x, float y, float rotation){
        rect(Core.atlas.find(region), x, y, rotation);
    }

    public static void vert(Texture texture, float[] vertices, int offset, int length){
        Core.batch.draw(texture, vertices, offset, length);
    }

    public static void flush(){
        if(batch != null) batch.flush();
    }

    /** Discards any pending batched sprites. */
    public static void discard(){
        batch.discard();
    }

    public static void proj(float x, float y, float w, float h){
        flush();
        batch.getProjection().setOrtho(x, y, w, h);
    }

    public static void proj(Camera proj){
        proj(proj.mat);
    }

    public static void proj(Mat proj){
        Core.batch.setProjection(proj);
    }

    public static Mat proj(){
        return Core.batch.getProjection();
    }

    public static void trans(Mat trans){
        Core.batch.setTransform(trans);
    }

    public static Mat trans(){
        return Core.batch.getTransform();
    }

    public static TextureRegion wrap(Texture texture){
        Tmp.tr2.set(texture);
        return Tmp.tr2;
    }

    public static void rectv(TextureRegion region, float x, float y, float width, float height, Cons<Vec2> tweaker){
        rectv(region, x, y, width, height, 0, tweaker);
    }

    public static void rectv(TextureRegion region, float x, float y, float width, float height, float rotation, Cons<Vec2> tweaker){
        rectv(region, x, y, width, height, width/2, height/2, rotation, tweaker);
    }

    public static void rectv(TextureRegion region, float x, float y, float width, float height, float originX, float originY, float rotation, Cons<Vec2> tweaker){
        x -= width/2f;
        y -= height/2f;

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

        tweaker.get(Tmp.v1.set(x1, y1));
        x1 = Tmp.v1.x;
        y1 = Tmp.v1.y;

        tweaker.get(Tmp.v1.set(x2, y2));
        x2 = Tmp.v1.x;
        y2 = Tmp.v1.y;

        tweaker.get(Tmp.v1.set(x3, y3));
        x3 = Tmp.v1.x;
        y3 = Tmp.v1.y;

        tweaker.get(Tmp.v1.set(x4, y4));
        x4 = Tmp.v1.x;
        y4 = Tmp.v1.y;

        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;

        final float color = batch.getPackedColor();
        final float mixColor = batch.getPackedMixColor();
        final float depth = region.getDepth();

        vertices[0]  = x1;
        vertices[1]  = y1;
        vertices[2]  = u;
        vertices[3]  = v;
        vertices[4]  = depth;
        vertices[5]  = color;
        vertices[6]  = mixColor;

        vertices[7]  = x2;
        vertices[8]  = y2;
        vertices[9]  = u;
        vertices[10] = v2;
        vertices[11] = depth;
        vertices[12] = color;
        vertices[13] = mixColor;

        vertices[14] = x3;
        vertices[15] = y3;
        vertices[16] = u2;
        vertices[17] = v2;
        vertices[18] = depth;
        vertices[19] = color;
        vertices[20] = mixColor;

        vertices[21] = x4;
        vertices[22] = y4;
        vertices[23] = u2;
        vertices[24] = v;
        vertices[25] = depth;
        vertices[26] = color;
        vertices[27] = mixColor;

        Draw.vert(region.texture, vertices, 0, vertices.length);
    }

}
