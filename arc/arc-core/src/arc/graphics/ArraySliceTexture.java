package arc.graphics;

import arc.graphics.gl.*;

/**
 * This class exists to wrap Texture in a way that can be used in TextureRegions, where it actually refers to a specific level of a texture array.
 * This is used in batches, and needs special instanceof checks to work properly. It doesn't have a real handle. The whole abstraction falls apart, really.
 * */
public class ArraySliceTexture extends Texture{
    public TextureArray array;
    public int index;

    public ArraySliceTexture(TextureArray array, int index){
        super(Gl.texture2dArray, array.getHandle());
        if(index >= array.getDepth()) throw new IllegalArgumentException("Array slice texture index out of bounds: " + index + " >= " + array.getDepth());

        this.array = array;
        this.index = index;
        this.width = array.width;
        this.height = array.height;
    }

    public void overwrite(TextureArray array, int index){
        this.array = array;
        this.index = index;
        this.glHandle = array.getHandle();
        this.width = array.width;
        this.height = array.height;
    }

    @Override
    public TextureFilter getMinFilter(){
        return array.getMinFilter();
    }

    @Override
    public TextureWrap getUWrap(){
        return array.getUWrap();
    }

    @Override
    public TextureFilter getMagFilter(){
        return array.getMagFilter();
    }

    @Override
    public TextureWrap getVWrap(){
        return array.getVWrap();
    }

    @Override
    public void setWrap(TextureWrap u, TextureWrap v){
        array.setWrap(u, v);
    }

    @Override
    public void setFilter(TextureFilter minFilter, TextureFilter magFilter){
        array.setFilter(minFilter, magFilter);
    }

    @Override
    public void draw(Pixmap pixmap, int x, int y){
        bind();
        Gl.texSubImage3D(glTarget, 0, x, y, index, pixmap.width, pixmap.height, 1, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.pixels);
    }

    @Override
    public void bind(){
        array.bind();
    }

    @Override
    public void bind(int unit){
        array.bind(unit);
    }

    @Override
    public int getDepth(){
        return index;
    }

    @Override
    public void dispose(){
        //slices shouldn't need to be disposed, dispose the whole array instead
    }

    @Override
    public boolean isDisposed(){
        return array.isDisposed();
    }
}
