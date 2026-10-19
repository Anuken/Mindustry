package arc.graphics;

import arc.*;
import arc.files.*;
import arc.graphics.gl.*;
import arc.math.geom.*;

/**
 * Wraps a standard OpenGL ES Cubemap. Must be disposed when it is no longer used.
 * @author Xoppa
 */
public class Cubemap extends GLTexture{

    public Cubemap(){
        super(Gl.textureCubeMap);
    }

    public Cubemap(String base){
        this(Core.files.internal(base + "right.png"),
            Core.files.internal(base + "left.png"),
            Core.files.internal(base + "top.png"),
            Core.files.internal(base + "bottom.png"),
            Core.files.internal(base + "front.png"),
            Core.files.internal(base + "back.png")
        );
    }

    /** Construct a Cubemap with the specified texture files for the sides, does not generate mipmaps. */
    public Cubemap(Fi positiveX, Fi negativeX, Fi positiveY, Fi negativeY, Fi positiveZ, Fi negativeZ){
        this(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ, false);
    }

    /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
    public Cubemap(Fi positiveX, Fi negativeX, Fi positiveY, Fi negativeY, Fi positiveZ, Fi negativeZ, boolean useMipMaps){
        this();

        load(new Pixmap[]{
            new Pixmap(positiveX),
            new Pixmap(negativeX),
            new Pixmap(positiveY),
            new Pixmap(negativeY),
            new Pixmap(positiveZ),
            new Pixmap(negativeZ)
        }, useMipMaps, true);
    }

    /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, optionally generating mipmaps. */
    public Cubemap(Pixmap positiveX, Pixmap negativeX, Pixmap positiveY, Pixmap negativeY, Pixmap positiveZ, Pixmap negativeZ, boolean useMipMaps){
        this();

        load(new Pixmap[]{positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ}, useMipMaps, false);
    }

    public void load(Pixmap[] pixmaps, boolean useMipMaps, boolean disposePixmaps){
        this.width = pixmaps[0].getWidth();
        this.height = pixmaps[0].getHeight();
        bind();
        setFilter(minFilter, magFilter);
        setWrap(uWrap, vWrap);

        for(int i = 0; i < pixmaps.length; i++){
            Pixmap pixmap = pixmaps[i];
            Gl.texImage2D(Gl.textureCubeMapPositiveX + i, 0, pixmap.getGLInternalFormat(), pixmap.width, pixmap.height, 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.pixels);
            if(disposePixmaps) pixmap.dispose();
        }

        if(useMipMaps) Gl.generateMipmap(glTarget);
    }

    /** Enum to identify each side of a Cubemap */
    public enum CubemapSide{
        /** The positive X and first side of the cubemap */
        positiveX(0, Gl.textureCubeMapPositiveX, 0, -1, 0, 1, 0, 0),
        /** The negative X and second side of the cubemap */
        negativeX(1, Gl.textureCubeMapNegativeX, 0, -1, 0, -1, 0, 0),
        /** The positive Y and third side of the cubemap */
        positiveY(2, Gl.textureCubeMapPositiveY, 0, 0, 1, 0, 1, 0),
        /** The negative Y and fourth side of the cubemap */
        negativeY(3, Gl.textureCubeMapNegativeY, 0, 0, -1, 0, -1, 0),
        /** The positive Z and fifth side of the cubemap */
        positiveZ(4, Gl.textureCubeMapPositiveZ, 0, -1, 0, 0, 0, 1),
        /** The negative Z and sixth side of the cubemap */
        negativeZ(5, Gl.textureCubeMapNegativeZ, 0, -1, 0, 0, 0, -1);

        /** Cached {@link CubemapSide#values()} for performance and ergonomics. */
        public static final CubemapSide[] all = values();

        /** The zero based index of the side in the cubemap */
        public final int index;
        /** The OpenGL target (used for glTexImage2D) of the side. */
        public final int glEnum;
        /** The up vector to target the side. */
        public final Vec3 up;
        /** The direction vector to target the side. */
        public final Vec3 direction;

        CubemapSide(int index, int glEnum, float upX, float upY, float upZ, float directionX, float directionY, float directionZ){
            this.index = index;
            this.glEnum = glEnum;
            this.up = new Vec3(upX, upY, upZ);
            this.direction = new Vec3(directionX, directionY, directionZ);
        }

        /** @return The up vector of the side. */
        public Vec3 getUp(Vec3 out){
            return out.set(up);
        }

        /** @return The direction vector of the side. */
        public Vec3 getDirection(Vec3 out){
            return out.set(direction);
        }
    }

}
