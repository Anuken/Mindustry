package arc.graphics.g2d;

import arc.graphics.*;
import arc.graphics.g2d.PixmapPacker.SkylineStrategy.SkylinePage.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

import java.util.*;

/**
 * Packs {@link Pixmap pixmaps} into one or more {@link Page pages} to generate an atlas of pixmap instances. Provides means to
 * directly convert the pixmap atlas to a {@link TextureAtlas}. The packer supports padding and border pixel duplication,
 * specified during construction. The packer supports incremental inserts and updates of TextureAtlases generated with this class.
 * How bin packing is performed can be customized via {@link PackStrategy}.
 * <p>
 * All methods can be called from any thread unless otherwise noted.
 * <p>
 * One-off usage:
 *
 * <pre>
 * // 512x512 pixel pages, RGB565 format, 2 pixels of padding, border duplication
 * PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, true);
 * packer.pack(&quot;First Pixmap&quot;, pixmap1);
 * packer.pack(&quot;Second Pixmap&quot;, pixmap2);
 * TextureAtlas atlas = packer.generateTextureAtlas(TextureFilter.nearest, TextureFilter.nearest, false);
 * packer.dispose();
 * // ...
 * atlas.dispose();
 * </pre>
 * <p>
 * With this usage pattern, disposing the packer will not dispose any pixmaps used by the texture atlas. The texture atlas must
 * also be disposed when no longer needed.
 * <p>
 * Incremental texture atlas usage:
 *
 * <pre>
 * // 512x512 pixel pages, RGB565 format, 2 pixels of padding, no border duplication
 * PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, false);
 * TextureAtlas atlas = new TextureAtlas();
 *
 * // potentially on a separate thread, e.g. downloading thumbnails
 * packer.pack(&quot;thumbnail&quot;, thumbnail);
 *
 * // on the rendering thread, every frame
 * packer.updateTextureAtlas(atlas, TextureFilter.Linear, TextureFilter.Linear, false);
 *
 * // once the atlas is no longer needed, make sure you get the final additions. This might
 * // be more elaborate depending on your threading model.
 * packer.updateTextureAtlas(atlas, TextureFilter.Linear, TextureFilter.Linear, false);
 * // ...
 * atlas.dispose();
 * </pre>
 * <p>
 * Pixmap-only usage:
 *
 * <pre>
 * PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, true);
 * packer.pack(&quot;First Pixmap&quot;, pixmap1);
 * packer.pack(&quot;Second Pixmap&quot;, pixmap2);
 *
 * // do something interesting with the resulting pages
 * for (Page page : packer.getPages()) {
 * 	// ...
 * }
 *
 * packer.dispose();
 * </pre>
 * @author mzechner
 * @author Nathan Sweet
 * @author Rob Rendell
 */
public class PixmapPacker implements Disposable{
    final Seq<Page> pages = new Seq<>();
    boolean packToTexture;
    boolean disposed;
    int pageWidth, pageHeight;
    int padding;
    boolean duplicateBorder;
    boolean stripWhitespaceX, stripWhitespaceY;
    Color transparentColor = new Color(0f, 0f, 0f, 0f);
    PackStrategy packStrategy;
    @Nullable Texture targetTexture;

    /**
     * Uses {@link GuillotineStrategy}.
     * @see PixmapPacker#PixmapPacker(int, int, int, boolean, boolean, boolean, PackStrategy)
     */
    public PixmapPacker(int pageWidth, int pageHeight, int padding, boolean duplicateBorder){
        this(pageWidth, pageHeight, padding, duplicateBorder, false, false, new GuillotineStrategy());
    }

    /**
     * Uses {@link GuillotineStrategy}.
     * @see PixmapPacker#PixmapPacker(int, int, int, boolean, boolean, boolean, PackStrategy)
     */
    public PixmapPacker(int pageWidth, int pageHeight, int padding, boolean duplicateBorder, PackStrategy packStrategy){
        this(pageWidth, pageHeight, padding, duplicateBorder, false, false, packStrategy);
    }

    /**
     * Creates a new ImagePacker which will insert all supplied pixmaps into one or more <code>pageWidth</code> by
     * <code>pageHeight</code> pixmaps using the specified strategy.
     * @param padding the number of blank pixels to insert between pixmaps.
     * @param duplicateBorder duplicate the border pixels of the inserted images to avoid seams when rendering with bi-Linear
     * filtering on.
     * @param stripWhitespaceX strip whitespace in x axis
     * @param stripWhitespaceY strip whitespace in y axis
     */
    public PixmapPacker(int pageWidth, int pageHeight, int padding, boolean duplicateBorder, boolean stripWhitespaceX, boolean stripWhitespaceY, PackStrategy packStrategy){
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.padding = padding;
        this.duplicateBorder = duplicateBorder;
        this.stripWhitespaceX = stripWhitespaceX;
        this.stripWhitespaceY = stripWhitespaceY;
        this.packStrategy = packStrategy;
    }

    public void setTargetTexture(Texture targetTexture){
        this.targetTexture = targetTexture;
    }

    public @Nullable Texture getTargetTexture(){
        return targetTexture;
    }

    /**
     * Sorts the images to the optimal order they should be packed. Some packing strategies rely heavily on the images being
     * sorted.
     */
    public void sort(Seq<PixmapRegion> images){
        packStrategy.sort(images);
    }

    /**
     * Inserts the pixmap without a name. It cannot be looked up by name.
     * @see #pack(String, Pixmap)
     */
    public synchronized Rect pack(Pixmap image){
        return pack(null, image);
    }

    /**
     * Inserts the pixmap. If name was not null, you can later retrieve the image's position in the output image via
     * {@link #getRect(String)}. Duplicate names will replace older rects.
     * @param name If null, the image cannot be looked up by name.
     * @return Rectangle describing the area the pixmap was rendered to.
     * @throws ArcRuntimeException in case the image did not fit due to the page size being too small or providing a duplicate
     * name.
     */
    public synchronized Rect pack(String name, Pixmap image){
        return pack(name, new PixmapRegion(image));
    }

    /**
     * Inserts the pixmap. If name was not null, you can later retrieve the image's position in the output image via
     * {@link #getRect(String)}. Duplicate names will replace older rects.
     * @param name If null, the image cannot be looked up by name.
     * @return Rectangle describing the area the pixmap was rendered to.
     * @throws ArcRuntimeException in case the image did not fit due to the page size being too small.
     */
    public synchronized Rect pack(String name, PixmapRegion image){
        return pack(name, image, null, null);
    }

    /** Inserts a named empty rectangle. */
    public synchronized Rect pack(String name, int width, int height){
        if(disposed) return null;

        PixmapPackerRect prev = null;
        Page prevPage = null;
        if(name != null){
            PixmapPackerRect stored = (PixmapPackerRect)getRect(name);
            if(stored != null && (int)stored.width == width && (int)stored.height == height){
                prev = stored;
                prevPage = getPage(name);
            }
        }

        PixmapPackerRect rect = new PixmapPackerRect(0, 0, width, height);
        if(rect.width > pageWidth || rect.height > pageHeight){
            if(name == null) throw new ArcRuntimeException("Page size too small for pixmap.");
            throw new ArcRuntimeException("Page size too small for pixmap: " + name);
        }

        Page page;
        if(prev != null && prevPage != null){
            page = prevPage;
            rect = prev;

            page.dirty = true;
            for(int y = (int)rect.y, ey = y + (int)rect.height; y < ey; y++){
                for(int x = (int)rect.x, ex = x + (int)rect.width; x < ex; x++){
                    page.image.setRaw(x, y, Color.clearRgba);
                }
            }
        }else{
            page = packStrategy.pack(this, name, rect);
            if(name != null){
                page.rects.put(name, rect);
                page.addedRects.add(name);
            }
        }

        return rect;
    }

    public synchronized Rect pack(@Nullable String name, PixmapRegion image, int[] splits, int[] pads){
        if(disposed) return null;

        //store previous rect to replace it; this saves space
        PixmapPackerRect prev = null;
        Page prevPage = null;

        if(name != null){
            PixmapPackerRect next = (PixmapPackerRect)getRect(name);
            if(next != null && (int)next.width == image.width && (int)next.height == image.height){
                prev = next;
                prevPage = getPage(name);
            }
        }

        boolean isPatch = name != null && name.endsWith(".9");

        PixmapPackerRect rect;
        Pixmap pixmapToDispose = null;
        if(isPatch && splits == null){
            rect = new PixmapPackerRect(0, 0, image.width - 2, image.height - 2);
            pixmapToDispose = new Pixmap(image.width - 2, image.height - 2);
            rect.splits = getSplits(image);
            rect.pads = getPads(image, rect.splits);
            pixmapToDispose.draw(image, 0, 0, 1, 1, image.width - 1, image.height - 1);
            image = new PixmapRegion(pixmapToDispose);
        }else{
            rect = new PixmapPackerRect(0, 0, image.width, image.height);
            rect.splits = splits;
            rect.pads = pads;
        }

        if(isPatch){
            name = name.split("\\.")[0];
        }

        if(rect.width > pageWidth || rect.height > pageHeight){
            if(name == null) throw new ArcRuntimeException("Page size too small for pixmap.");
            throw new ArcRuntimeException("Page size too small for pixmap: " + name);
        }

        Page page;

        //try to use the old rect if possible
        if(prev != null && prevPage != null && !isPatch){
            page = prevPage;
            rect = prev;
        }else{
            page = packStrategy.pack(this, name, rect);
            if(name != null){
                page.rects.put(name, rect);
                page.addedRects.add(name);
            }
        }

        int rectX = (int)rect.x, rectY = (int)rect.y, rectWidth = (int)rect.width, rectHeight = (int)rect.height;

        if(packToTexture && !duplicateBorder && page.texture != null && !page.dirty){
            //TODO this will not work correctly since the pixmap is only a region!
            page.texture.bind();
            Gl.texSubImage2D(page.texture.glTarget, 0, rectX, rectY, rectWidth, rectHeight, image.pixmap.getGLFormat(),
                image.pixmap.getGLType(), image.pixmap.pixels);
        }else
            page.dirty = true;

        page.image.draw(image, rectX, rectY);

        if(duplicateBorder){
            int imageWidth = image.width, imageHeight = image.height;
            // Copy corner pixels to fill corners of the padding.
            page.image.draw(image, 0, 0, 1, 1, rectX - 1, rectY - 1, 1, 1);
            page.image.draw(image, imageWidth - 1, 0, 1, 1, rectX + rectWidth, rectY - 1, 1, 1);
            page.image.draw(image, 0, imageHeight - 1, 1, 1, rectX - 1, rectY + rectHeight, 1, 1);
            page.image.draw(image, imageWidth - 1, imageHeight - 1, 1, 1, rectX + rectWidth, rectY + rectHeight, 1, 1);
            // Copy edge pixels into padding.
            page.image.draw(image, 0, 0, imageWidth, 1, rectX, rectY - 1, rectWidth, 1);
            page.image.draw(image, 0, imageHeight - 1, imageWidth, 1, rectX, rectY + rectHeight, rectWidth, 1);
            page.image.draw(image, 0, 0, 1, imageHeight, rectX - 1, rectY, 1, rectHeight);
            page.image.draw(image, imageWidth - 1, 0, 1, imageHeight, rectX + rectWidth, rectY, 1, rectHeight);
        }

        if(pixmapToDispose != null){
            pixmapToDispose.dispose();
        }

        return rect;
    }

    /**
     * @return the {@link Page} instances created so far. If multiple threads are accessing the packer, iterating over the pages
     * must be done only after synchronizing on the packer.
     */
    public Seq<Page> getPages(){
        return pages;
    }

    /**
     * @param name the name of the image
     * @return the rectangle for the image in the page it's stored in or null
     */
    public synchronized Rect getRect(String name){
        for(Page page : pages){
            Rect rect = page.rects.get(name);
            if(rect != null) return rect;
        }
        return null;
    }

    /** @return the newly allocated region for this name, or null. */
    public synchronized PixmapRegion getRegion(String name){
        for(Page page : pages){
            Rect rect = page.rects.get(name);
            if(rect != null) return new PixmapRegion(page.getPixmap(), (int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);
        }
        return null;
    }

    /**
     * @param name the name of the image
     * @return the page the image is stored in or null
     */
    public synchronized Page getPage(String name){
        for(Page page : pages){
            Rect rect = page.rects.get(name);
            if(rect != null) return page;
        }
        return null;
    }

    /**
     * Returns the index of the page containing the given packed rectangle.
     * @param name the name of the image
     * @return the index of the page the image is stored in or -1
     */
    public synchronized int getPageIndex(String name){
        for(int i = 0; i < pages.size; i++){
            Rect rect = pages.get(i).rects.get(name);
            if(rect != null) return i;
        }
        return -1;
    }

    /**
     * Disposes any pixmap pages which don't have a texture. Page pixmaps that have a texture will not be disposed until their texture is disposed.
     */
    @Override
    public synchronized void dispose(){
        for(Page page : pages){
            if(page.texture == null){
                page.image.dispose();
            }
        }
        disposed = true;
    }

    /** Disposes all images, regardless of whether they have a texture. */
    public void forceDispose(){
        for(Page page : pages){
            if(page.image != null){
                page.image.dispose();
            }
        }
        disposed = true;
    }

    /**
     * Updates the {@link TextureAtlas}, adding any new {@link Pixmap} instances packed since the last call to this method. This
     * can be used to insert Pixmap instances on a separate thread via {@link #pack(String, Pixmap)} and update the TextureAtlas on
     * the rendering thread. This method must be called on the rendering thread. After calling this method, disposing the packer
     * will no longer dispose the page pixmaps.
     */
    public synchronized TextureAtlas generateTextureAtlas(TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps, boolean clearRects, int extraPages){
        TextureAtlas atlas = new TextureAtlas();
        TextureArray array = new TextureArray(getPageWidth(), getPageHeight(), pages.size + extraPages);
        atlas.setTexture(array);
        array.setFilter(minFilter, magFilter);

        int i = 0;
        for(Page page : pages){
            if(page.addedRects.size > 0){
                ArraySliceTexture tex = new ArraySliceTexture(array, i);
                tex.draw(page.image);

                for(String name : page.addedRects){
                    PixmapPackerRect rect = page.rects.get(name);
                    TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion(tex, (int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);

                    if(rect.splits != null){
                        region.splits = rect.splits;
                        region.pads = rect.pads;
                    }

                    region.name = name;
                    region.offsetX = rect.offsetX;
                    region.offsetY = (int)(rect.originalHeight - rect.height - rect.offsetY);
                    region.originalWidth = rect.originalWidth;
                    region.originalHeight = rect.originalHeight;

                    atlas.getRegions().add(region);
                    atlas.getRegionMap().put(name, region);
                }
                if(clearRects) page.addedRects.clear();
            }

            i ++;
        }

        return atlas;
    }

    /**
     * Calls {@link Page#updateTexture(TextureFilter, TextureFilter, boolean) updateTexture} for each page and adds a region to
     * the specified array for each page texture.
     */
    public synchronized void updateTextureRegions(Seq<TextureRegion> regions, TextureFilter minFilter, TextureFilter magFilter,
                                                  boolean useMipMaps){
        updatePageTextures(minFilter, magFilter, useMipMaps);
        while(regions.size < pages.size)
            regions.add(new TextureRegion(pages.get(regions.size).texture));
    }

    /** Calls {@link Page#updateTexture(TextureFilter, TextureFilter, boolean) updateTexture} for each page. */
    public synchronized void updatePageTextures(TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps){
        for(Page page : pages)
            page.updateTexture(minFilter, magFilter, useMipMaps);
    }

    public int getPageWidth(){
        return pageWidth;
    }

    public void setPageWidth(int pageWidth){
        this.pageWidth = pageWidth;
    }

    public int getPageHeight(){
        return pageHeight;
    }

    public void setPageHeight(int pageHeight){
        this.pageHeight = pageHeight;
    }

    public int getPadding(){
        return padding;
    }

    public void setPadding(int padding){
        this.padding = padding;
    }

    public boolean getDuplicateBorder(){
        return duplicateBorder;
    }

    public void setDuplicateBorder(boolean duplicateBorder){
        this.duplicateBorder = duplicateBorder;
    }

    public boolean getPackToTexture(){
        return packToTexture;
    }

    /**
     * If true, when a pixmap is packed to a page that has a texture, the portion of the texture where the pixmap was packed is
     * updated using glTexSubImage2D. Note if packing many pixmaps, this may be slower than reuploading the whole texture. This
     * setting is ignored if {@link #getDuplicateBorder()} is true.
     */
    public void setPackToTexture(boolean packToTexture){
        this.packToTexture = packToTexture;
    }

    /** @see PixmapPacker#setTransparentColor(Color color) */
    public Color getTransparentColor(){
        return this.transparentColor;
    }

    /**
     * Sets the default <code>color</code> of the whole {@link PixmapPacker.Page} when a new one created. Helps to avoid texture
     * bleeding or to highlight the page for debugging.
     * @see Page#Page(PixmapPacker packer)
     */
    public void setTransparentColor(Color color){
        this.transparentColor.set(color);
    }

    private int[] getSplits(PixmapRegion raster){

        int startX = getSplitPoint(raster, 1, 0, true, true);
        int endX = getSplitPoint(raster, startX, 0, false, true);
        int startY = getSplitPoint(raster, 0, 1, true, false);
        int endY = getSplitPoint(raster, 0, startY, false, false);

        // Ensure pixels after the end are not invalid.
        getSplitPoint(raster, endX + 1, 0, true, true);
        getSplitPoint(raster, 0, endY + 1, true, false);

        // No splits, or all splits.
        if(startX == 0 && endX == 0 && startY == 0 && endY == 0) return null;

        // Subtraction here is because the coordinates were computed before the 1px border was stripped.
        if(startX != 0){
            startX--;
            endX = raster.width - 2 - (endX - 1);
        }else{
            // If no start point was ever found, we assume full stretch.
            endX = raster.width - 2;
        }
        if(startY != 0){
            startY--;
            endY = raster.height - 2 - (endY - 1);
        }else{
            // If no start point was ever found, we assume full stretch.
            endY = raster.height - 2;
        }

        return new int[]{startX, endX, startY, endY};
    }

    private int[] getPads(PixmapRegion raster, int[] splits){

        int bottom = raster.height - 1;
        int right = raster.width - 1;

        int startX = getSplitPoint(raster, 1, bottom, true, true);
        int startY = getSplitPoint(raster, right, 1, true, false);

        // No need to hunt for the end if a start was never found.
        int endX = 0;
        int endY = 0;
        if(startX != 0) endX = getSplitPoint(raster, startX + 1, bottom, false, true);
        if(startY != 0) endY = getSplitPoint(raster, right, startY + 1, false, false);

        // Ensure pixels after the end are not invalid.
        getSplitPoint(raster, endX + 1, bottom, true, true);
        getSplitPoint(raster, right, endY + 1, true, false);

        // No pads.
        if(startX == 0 && endX == 0 && startY == 0 && endY == 0){
            return null;
        }

        // -2 here is because the coordinates were computed before the 1px border was stripped.
        if(startX == 0 && endX == 0){
            startX = -1;
            endX = -1;
        }else{
            if(startX > 0){
                startX--;
                endX = raster.width - 2 - (endX - 1);
            }else{
                // If no start point was ever found, we assume full stretch.
                endX = raster.width - 2;
            }
        }
        if(startY == 0 && endY == 0){
            startY = -1;
            endY = -1;
        }else{
            if(startY > 0){
                startY--;
                endY = raster.height- 2 - (endY - 1);
            }else{
                // If no start point was ever found, we assume full stretch.
                endY = raster.height - 2;
            }
        }

        int[] pads = {startX, endX, startY, endY};

        if(splits != null && Arrays.equals(pads, splits)){
            return null;
        }

        return pads;
    }

    private int getSplitPoint(PixmapRegion raster, int startX, int startY, boolean startPoint, boolean xAxis){
        int next = xAxis ? startX : startY;
        int end = xAxis ? raster.width : raster.height;
        int breakA = startPoint ? 255 : 0;

        int x = startX;
        int y = startY;
        while(next != end){
            if(xAxis)
                x = next;
            else
                y = next;

            int a = raster.getA(x, y);
            if(a == breakA) return next;

            next++;
        }

        return 0;
    }

    /**
     * Choose the page and location for each rectangle.
     * @author Nathan Sweet
     */
    public interface PackStrategy{
        void sort(Seq<PixmapRegion> images);

        /** Returns the page the rectangle should be placed in and modifies the specified rectangle position. */
        Page pack(PixmapPacker packer, String name, Rect rect);
    }

    /**
     * @author mzechner
     * @author Nathan Sweet
     * @author Rob Rendell
     */
    public static class Page{
        public final Seq<String> addedRects = new Seq<>();
        public OrderedMap<String, PixmapPackerRect> rects = new OrderedMap<>();
        public Pixmap image;
        public Texture texture;
        boolean dirty;

        /** Creates a new page filled with the color provided by the {@link PixmapPacker#getTransparentColor()} */
        public Page(PixmapPacker packer){
            image = new Pixmap(packer.pageWidth, packer.pageHeight);
            Color transparentColor = packer.getTransparentColor();
            if(transparentColor.rgba() != 0){
                this.image.fill(transparentColor);
            }

            if(packer.targetTexture != null){
                this.texture = packer.targetTexture;
            }
        }

        public void setDirty(boolean dirty){
            this.dirty = dirty;
        }

        public Pixmap getPixmap(){
            return image;
        }

        public OrderedMap<String, PixmapPackerRect> getRects(){
            return rects;
        }

        /**
         * Returns the texture for this page, or null if the texture has not been created.
         * @see #updateTexture(TextureFilter, TextureFilter, boolean)
         */
        public Texture getTexture(){
            return texture;
        }

        /**
         * Creates the texture if it has not been created, else reuploads the entire page pixmap to the texture if the pixmap has
         * changed since this method was last called.
         * @return true if the texture was created or reuploaded.
         */
        public boolean updateTexture(TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps){
            if(texture != null){
                if(!dirty) return false;

                //this doesn't update mipmaps, but for UI, I don't care
                texture.draw(image);
            }else{
                texture = new Texture(image, useMipMaps);
                texture.setFilter(minFilter, magFilter);
            }
            dirty = false;
            return true;
        }
    }

    /**
     * Does bin packing by inserting to the right or below previously packed rectangles. This is good at packing arbitrarily sized
     * images.
     * @author mzechner
     * @author Nathan Sweet
     * @author Rob Rendell
     */
    public static class GuillotineStrategy implements PackStrategy{

        @Override
        public void sort(Seq<PixmapRegion> pixmaps){
            pixmaps.sort(Structs.comparingInt(o -> Math.max(o.width, o.height)));
        }

        @Override
        public Page pack(PixmapPacker packer, String name, Rect rect){
            GuillotinePage page;
            if(packer.pages.size == 0){
                // Add a page if empty.
                page = new GuillotinePage(packer);
                packer.pages.add(page);
            }else{
                // Always try to pack into the last page.
                page = (GuillotinePage)packer.pages.peek();
            }

            int padding = packer.padding;
            rect.width += padding;
            rect.height += padding;
            Node node = insert(page.root, rect);
            if(node == null){
                // Didn't fit, pack into a new page.
                page = new GuillotinePage(packer);
                packer.pages.add(page);
                node = insert(page.root, rect);
            }
            if(node == null) throw new ArcRuntimeException("Failed to pack sprite '" + name + "' of size " + (int)rect.width + "x" + (int)rect.height + " into page of size " + (int)page.root.rect.width + "x" + (int)page.root.rect.height);
            node.full = true;
            rect.set(node.rect.x, node.rect.y, node.rect.width - padding, node.rect.height - padding);
            return page;
        }

        private Node insert(Node node, Rect rect){
            if(!node.full && node.leftChild != null && node.rightChild != null){
                Node newNode = insert(node.leftChild, rect);
                if(newNode == null) newNode = insert(node.rightChild, rect);
                return newNode;
            }else{
                if(node.full) return null;
                if(node.rect.width == rect.width && node.rect.height == rect.height) return node;
                if(node.rect.width < rect.width || node.rect.height < rect.height) return null;

                node.leftChild = new Node();
                node.rightChild = new Node();

                int deltaWidth = (int)node.rect.width - (int)rect.width;
                int deltaHeight = (int)node.rect.height - (int)rect.height;
                if(deltaWidth > deltaHeight){
                    node.leftChild.rect.x = node.rect.x;
                    node.leftChild.rect.y = node.rect.y;
                    node.leftChild.rect.width = rect.width;
                    node.leftChild.rect.height = node.rect.height;

                    node.rightChild.rect.x = node.rect.x + rect.width;
                    node.rightChild.rect.y = node.rect.y;
                    node.rightChild.rect.width = node.rect.width - rect.width;
                    node.rightChild.rect.height = node.rect.height;
                }else{
                    node.leftChild.rect.x = node.rect.x;
                    node.leftChild.rect.y = node.rect.y;
                    node.leftChild.rect.width = node.rect.width;
                    node.leftChild.rect.height = rect.height;

                    node.rightChild.rect.x = node.rect.x;
                    node.rightChild.rect.y = node.rect.y + rect.height;
                    node.rightChild.rect.width = node.rect.width;
                    node.rightChild.rect.height = node.rect.height - rect.height;
                }

                return insert(node.leftChild, rect);
            }
        }

        static final class Node{
            public final Rect rect = new Rect();
            public Node leftChild;
            public Node rightChild;
            public boolean full;
        }

        public static class GuillotinePage extends Page{
            Node root;

            public GuillotinePage(PixmapPacker packer){
                super(packer);
                root = new Node();
                root.rect.x = packer.padding;
                root.rect.y = packer.padding;
                root.rect.width = packer.pageWidth - packer.padding * 2;
                root.rect.height = packer.pageHeight - packer.padding * 2;
            }
        }
    }

    /**
     * Does bin packing by inserting in rows. This is good at packing images that have similar heights.
     * @author Nathan Sweet
     */
    public static class SkylineStrategy implements PackStrategy{

        @Override
        public void sort(Seq<PixmapRegion> images){
            images.sort((o1, o2) -> o1.height - o2.height);
        }

        public Page pack(PixmapPacker packer, String name, Rect rect){
            int padding = packer.padding;
            int pageWidth = packer.pageWidth - padding * 2, pageHeight = packer.pageHeight - padding * 2;
            int rectWidth = (int)rect.width + padding, rectHeight = (int)rect.height + padding;
            for(int i = 0, n = packer.pages.size; i < n; i++){
                SkylinePage page = (SkylinePage)packer.pages.get(i);
                Row bestRow = null;
                // Fit in any row before the last.
                for(int ii = 0, nn = page.rows.size - 1; ii < nn; ii++){
                    Row row = page.rows.get(ii);
                    if(row.x + rectWidth >= pageWidth) continue;
                    if(row.y + rectHeight >= pageHeight) continue;
                    if(rectHeight > row.height) continue;
                    if(bestRow == null || row.height < bestRow.height) bestRow = row;
                }
                if(bestRow == null){
                    // Fit in last row, increasing height.
                    Row row = page.rows.peek();
                    if(row.y + rectHeight >= pageHeight) continue;
                    if(row.x + rectWidth < pageWidth){
                        row.height = Math.max(row.height, rectHeight);
                        bestRow = row;
                    }else if(row.y + row.height + rectHeight < pageHeight){
                        // Fit in new row.
                        bestRow = new Row();
                        bestRow.y = row.y + row.height;
                        bestRow.height = rectHeight;
                        page.rows.add(bestRow);
                    }
                }
                if(bestRow != null){
                    rect.x = bestRow.x;
                    rect.y = bestRow.y;
                    bestRow.x += rectWidth;
                    return page;
                }
            }
            // Fit in new page.
            SkylinePage page = new SkylinePage(packer);
            packer.pages.add(page);
            Row row = new Row();
            row.x = padding + rectWidth;
            row.y = padding;
            row.height = rectHeight;
            page.rows.add(row);
            rect.x = padding;
            rect.y = padding;
            return page;
        }

        static class SkylinePage extends Page{
            Seq<Row> rows = new Seq<>();

            public SkylinePage(PixmapPacker packer){
                super(packer);

            }

            static class Row{
                int x, y, height;
            }
        }
    }

    public static class PixmapPackerRect extends Rect{
        public int[] splits;
        public int[] pads;
        public int offsetX, offsetY;
        public int originalWidth, originalHeight;

        public PixmapPackerRect(int x, int y, int width, int height){
            super(x, y, width, height);
            this.offsetX = 0;
            this.offsetY = 0;
            this.originalWidth = width;
            this.originalHeight = height;
        }

        public PixmapPackerRect(int x, int y, int width, int height, int left, int top, int originalWidth, int originalHeight){
            super(x, y, width, height);
            this.offsetX = left;
            this.offsetY = top;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }
    }

}
