/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Matthias Mann nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package arc.graphics.font;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.Seq;
import arc.struct.FloatSeq;
import arc.files.Fi;
import arc.graphics.TextureFilter;
import arc.graphics.g2d.GlyphLayout.GlyphRun;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.util.ArcRuntimeException;
import arc.util.Disposable;
import arc.util.io.Streams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders bitmap fonts. The font consists of 2 files: an image file or {@link TextureRegion} containing the glyphs and a file in
 * the AngleCode BMFont text format that describes where each glyph is on the image.
 * <p>
 * Text is drawn using a {@link Batch}. Text can be cached in a {@link FontCache} for faster rendering of static text, which
 * saves needing to compute the location of each glyph each frame.
 * <p>
 * * The texture for a BitmapFont loaded from a file is managed. {@link #dispose()} must be called to free the texture when no
 * longer needed. Disposing the BitmapFont disposes the region's texture, which may not be desirable if the texture is still being used elsewhere.
 * <p>
 * The code was originally based on Matthias Mann's TWL BitmapFont class. Thanks for sharing, Matthias! :)
 * @author Nathan Sweet
 * @author Matthias Mann
 */
public class Font implements Disposable{
    private static final int LOG2_PAGE_SIZE = 9;
    private static final int PAGE_SIZE = 1 << LOG2_PAGE_SIZE;
    private static final int PAGES = 0x10000 / PAGE_SIZE;

    public final FontData data;
    private final FontCache cache;
    Seq<TextureRegion> regions;
    boolean integer;
    private boolean flipped;
    private boolean ownsTexture;

    /**
     * Creates a BitmapFont with the glyphs relative to the specified region. If the region is null, the glyph textures are loaded
     * from the image file given in the font file. The {@link #dispose()} method will not dispose the region's texture in this
     * case!
     * <p>
     * The font data is not flipped.
     * @param fontFile the font definition file
     * @param region The texture region containing the glyphs. The glyphs must be relative to the lower left corner (ie, the region
     * should not be flipped). If the region is null the glyph images are loaded from the image path in the font file.
     */
    public Font(Fi fontFile, TextureRegion region){
        this(fontFile, region, false);
    }

    /**
     * Creates a BitmapFont with the glyphs relative to the specified region. If the region is null, the glyph textures are loaded
     * from the image file given in the font file. The {@link #dispose()} method will not dispose the region's texture in this
     * case!
     * @param region The texture region containing the glyphs. The glyphs must be relative to the lower left corner (ie, the region
     * should not be flipped). If the region is null the glyph images are loaded from the image path in the font file.
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public Font(Fi fontFile, TextureRegion region, boolean flip){
        this(new FontData(fontFile, flip), region, true);
    }

    /**
     * Creates a BitmapFont from a BMFont file. The image file name is read from the BMFont file and the image is loaded from the
     * same directory. The font data is not flipped.
     */
    public Font(Fi fontFile){
        this(fontFile, false);
    }

    /**
     * Creates a BitmapFont from a BMFont file. The image file name is read from the BMFont file and the image is loaded from the
     * same directory.
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public Font(Fi fontFile, boolean flip){
        this(new FontData(fontFile, flip), (TextureRegion)null, true);
    }

    /**
     * Creates a BitmapFont from a BMFont file, using the specified image for glyphs. Any image specified in the BMFont file is
     * ignored.
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public Font(Fi fontFile, Fi imageFile, boolean flip){
        this(fontFile, imageFile, flip, true);
    }

    /**
     * Creates a BitmapFont from a BMFont file, using the specified image for glyphs. Any image specified in the BMFont file is
     * ignored.
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    public Font(Fi fontFile, Fi imageFile, boolean flip, boolean integer){
        this(new FontData(fontFile, flip), new TextureRegion(new Texture(imageFile, false)), integer);
        ownsTexture = true;
    }

    /**
     * Constructs a new BitmapFont from the given {@link FontData} and {@link TextureRegion}. If the TextureRegion is null,
     * the image path(s) will be read from the BitmapFontData. The dispose() method will not dispose the texture of the region(s)
     * if the region is != null.
     * <p>
     * Passing a single TextureRegion assumes that your font only needs a single texture page. If you need to support multiple
     * pages, either let the Font read the images themselves (by specifying null as the TextureRegion), or by specifying each page
     * manually with the TextureRegion[] constructor.
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    public Font(FontData data, TextureRegion region, boolean integer){
        this(data, region != null ? Seq.with(region) : null, integer);
    }

    /**
     * Constructs a new BitmapFont from the given {@link FontData} and array of {@link TextureRegion}. If the TextureRegion
     * is null or empty, the image path(s) will be read from the BitmapFontData. The dispose() method will not dispose the texture
     * of the region(s) if the regions array is != null and not empty.
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    public Font(FontData data, Seq<TextureRegion> pageRegions, boolean integer){
        this.flipped = data.flipped;
        this.data = data;
        this.integer = integer;

        if(pageRegions == null || pageRegions.size == 0){
            if(data.imagePaths == null)
                throw new IllegalArgumentException("If no regions are specified, the font data must have an images path.");

            // Load each path.
            int n = data.imagePaths.length;
            regions = new Seq<>(n);
            for(int i = 0; i < n; i++){
                Fi file;
                if(data.fontFile == null)
                    file = Core.files.internal(data.imagePaths[i]);
                else
                    file = Core.files.get(data.imagePaths[i], data.fontFile.type());
                regions.add(new TextureRegion(new Texture(file, false)));
            }
            ownsTexture = true;
        }else{
            regions = pageRegions;
            ownsTexture = false;
        }

        cache = newFontCache();

        load(data);
    }

    static int indexOf(CharSequence text, char ch, int start){
        final int n = text.length();
        for(; start < n; start++)
            if(text.charAt(start) == ch) return start;
        return n;
    }

    protected void load(FontData data){
        for(Glyph[] page : data.glyphs){
            if(page == null) continue;
            for(Glyph glyph : page)
                if(glyph != null) data.setGlyphRegion(glyph, regions.get(glyph.page));
        }
        if(data.missingGlyph != null) data.setGlyphRegion(data.missingGlyph, regions.get(data.missingGlyph.page));
    }

    /**
     * Draws text at the specified position.
     * @see FontCache#addText(CharSequence, float, float)
     */
    public GlyphLayout draw(CharSequence str, float x, float y){
        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y);
        cache.draw();
        return layout;
    }

    public GlyphLayout draw(CharSequence str, float x, float y, Color color, float scale, boolean integer, int halign){
        float pscale = getData().scaleX;
        boolean pint = usesIntegerPositions();
        setColor(color);
        getData().setScale(scale);
        setUseIntegerPositions(integer);
        GlyphLayout result =  draw(str, x, y, 0, halign, false);
        getData().setScale(pscale);
        setUseIntegerPositions(pint);
        setColor(Color.white);
        return result;
    }

    public GlyphLayout draw(CharSequence str, float x, float y, int halign){
        return draw(str, x, y, 0, halign, false);
    }

    /**
     * Draws text at the specified position.
     * @see FontCache#addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout draw(CharSequence str, float x, float y, float targetWidth, int halign, boolean wrap){
        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y, targetWidth, halign, wrap);
        cache.draw();
        return layout;
    }

    /**
     * Draws text at the specified position.
     * @see FontCache#addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout draw(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign, boolean wrap){
        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y, start, end, targetWidth, halign, wrap);
        cache.draw();
        return layout;
    }

    /**
     * Draws text at the specified position.
     * @see FontCache#addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public GlyphLayout draw(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                            boolean wrap, String truncate){
        cache.clear();
        GlyphLayout layout = cache.addText(str, x, y, start, end, targetWidth, halign, wrap, truncate);
        cache.draw();
        return layout;
    }

    /**
     * Draws text at the specified position.
     * @see FontCache#addText(CharSequence, float, float, int, int, float, int, boolean, String)
     */
    public void draw(GlyphLayout layout, float x, float y){
        cache.clear();
        cache.addText(layout, x, y);
        cache.draw();
    }

    /** Returns the color of text drawn with this font. */
    public Color getColor(){
        return cache.getColor();
    }

    /** A convenience method for setting the font color. The color can also be set by modifying {@link #getColor()}. */
    public void setColor(Color color){
        cache.getColor().set(color);
    }

    /** A convenience method for setting the font color. The color can also be set by modifying {@link #getColor()}. */
    public void setColor(float r, float g, float b, float a){
        cache.getColor().set(r, g, b, a);
    }

    public float getScaleX(){
        return data.scaleX;
    }

    public float getScaleY(){
        return data.scaleY;
    }

    /**
     * Returns the first texture region. This is included for backwards compatibility, and for convenience since most fonts only
     * use one texture page. For multi-page fonts, use {@link #getRegions()}.
     * @return the first texture region
     */
    public TextureRegion getRegion(){
        return regions.first();
    }

    /**
     * Returns the array of TextureRegions that represents each texture page of glyphs.
     * @return the array of texture regions; modifying it may produce undesirable results
     */
    public Seq<TextureRegion> getRegions(){
        return regions;
    }

    /**
     * Returns the texture page at the given index.
     * @return the texture page at the given index
     */
    public TextureRegion getRegion(int index){
        return regions.get(index);
    }

    /** Returns the line height, which is the distance from one line of text to the next. */
    public float getLineHeight(){
        return data.lineHeight;
    }

    /** Returns the x-advance of the space character. */
    public float getSpaceXadvance(){
        return data.spaceXadvance;
    }

    /** Returns the x-height, which is the distance from the top of most lowercase characters to the baseline. */
    public float getXHeight(){
        return data.xHeight;
    }

    /**
     * Returns the cap height, which is the distance from the top of most uppercase characters to the baseline. Since the drawing
     * position is the cap height of the first line, the cap height can be used to get the location of the baseline.
     */
    public float getCapHeight(){
        return data.capHeight;
    }

    /** Returns the ascent, which is the distance from the cap height to the top of the tallest glyph. */
    public float getAscent(){
        return data.ascent;
    }

    /**
     * Returns the descent, which is the distance from the bottom of the glyph that extends the lowest to the baseline. This
     * number is negative.
     */
    public float getDescent(){
        return data.descent;
    }

    /** Returns true if this BitmapFont has been flipped for use with a y-down coordinate system. */
    public boolean isFlipped(){
        return flipped;
    }

    /** Disposes the texture used by this BitmapFont's region IF this BitmapFont created the texture. */
    @Override
    public void dispose(){
        if(ownsTexture){
            for(int i = 0; i < regions.size; i++)
                regions.get(i).texture.dispose();
        }
    }

    @Override
    public boolean isDisposed(){
        if(ownsTexture){
            //it's a fair assumption to say that if one region is disposed, the whole font is disposed
            return regions.contains(t -> t.texture.isDisposed());
        }
        return false;
    }

    /**
     * Makes the specified glyphs fixed width. This can be useful to make the numbers in a font fixed width. Eg, when horizontally
     * centering a score or loading percentage text, it will not jump around as different numbers are shown.
     */
    public void setFixedWidthGlyphs(CharSequence glyphs){
        FontData data = this.data;
        int maxAdvance = 0;
        for(int index = 0, end = glyphs.length(); index < end; index++){
            Glyph g = data.getGlyph(glyphs.charAt(index));
            if(g != null && g.xadvance > maxAdvance) maxAdvance = g.xadvance;
        }
        for(int index = 0, end = glyphs.length(); index < end; index++){
            Glyph g = data.getGlyph(glyphs.charAt(index));
            if(g == null) continue;
            g.xoffset += Math.round((maxAdvance - g.xadvance) / 2);
            g.xadvance = maxAdvance;
            g.kerning = null;
            g.fixedWidth = true;
        }
    }

    /** Specifies whether to use integer positions. Default is to use them so filtering doesn't kick in as badly. */
    public void setUseIntegerPositions(boolean integer){
        this.integer = integer;
        cache.setUseIntegerPositions(integer);
    }

    /** Checks whether this font uses integer positions for drawing. */
    public boolean usesIntegerPositions(){
        return integer;
    }

    /**
     * For expert usage -- returns the BitmapFontCache used by this font, for rendering to a sprite batch. This can be used, for
     * example, to manipulate glyph colors within a specific index.
     * @return the bitmap font cache used by this font
     */
    public FontCache getCache(){
        return cache;
    }

    /** Gets the underlying {@link FontData} for this BitmapFont. */
    public FontData getData(){
        return data;
    }

    /** @return whether the texture is owned by the font, font disposes the texture itself if true */
    public boolean ownsTexture(){
        return ownsTexture;
    }

    /**
     * Sets whether the font owns the texture. In case it does, the font will also dispose of the texture when {@link #dispose()}
     * is called. Use with care!
     * @param ownsTexture whether the font owns the texture
     */
    public void setOwnsTexture(boolean ownsTexture){
        this.ownsTexture = ownsTexture;
    }

    /**
     * Creates a new BitmapFontCache for this font. Using this method allows the font to provide the BitmapFontCache
     * implementation to customize rendering.
     * <p>
     * Note this method is called by the BitmapFont constructors. If a subclass overrides this method, it will be called before the
     * subclass constructors.
     */
    public FontCache newFontCache(){
        return new FontCache(this, integer);
    }

    public String toString(){
        if(data.fontFile != null) return data.fontFile.nameWithoutExtension();
        return super.toString();
    }

    /**
     * Adds a fallback font. If this font is missing characters, the fallback will be used instead.
     * This is experimental, and only tested and functional for freetype fonts using a single shared page. Expect immediate crashes in other scenarios!
     * */
    public void addFallback(Font other){
        data.addFallback(other.data);
    }

    /** Represents a single character in a font page. */
    public static class Glyph{
        public int id;
        public int srcX;
        public int srcY;
        public int width, height;
        public Texture texture;
        public float u, v, u2, v2;
        public int xoffset, yoffset;
        public int xadvance;
        public byte[][] kerning;
        public boolean fixedWidth;

        /** The index to the texture page that holds this glyph. */
        public int page = 0;

        public int getKerning(char ch){
            if(kerning != null){
                byte[] page = kerning[ch >>> LOG2_PAGE_SIZE];
                if(page != null) return page[ch & PAGE_SIZE - 1];
            }
            return 0;
        }

        public void setKerning(int ch, int value){
            if(kerning == null) kerning = new byte[PAGES][];
            byte[] page = kerning[ch >>> LOG2_PAGE_SIZE];
            if(page == null) kerning[ch >>> LOG2_PAGE_SIZE] = page = new byte[PAGE_SIZE];
            page[ch & PAGE_SIZE - 1] = (byte)value;
        }

        public String toString(){
            return Character.toString((char)id);
        }
    }

    /** Backing data for a {@link Font}. */
    public static class FontData{
        public final Glyph[][] glyphs = new Glyph[PAGES][];
        /** An array of the image paths, for multiple texture pages. */
        public String[] imagePaths;
        public Fi fontFile;
        public boolean flipped;
        public float padTop, padRight, padBottom, padLeft;
        /** The distance from one line of text to the next. To set this value, use {@link #setLineHeight(float)}. */
        public float lineHeight;
        /**
         * The distance from the top of most uppercase characters to the baseline. Since the drawing position is the cap height of
         * the first line, the cap height can be used to get the location of the baseline.
         */
        public float capHeight = 1;
        /** The distance from the cap height to the top of the tallest glyph. */
        public float ascent;
        /** The distance from the bottom of the glyph that extends the lowest to the baseline. This number is negative. */
        public float descent;
        /** The distance to move down when \n is encountered. */
        public float down;
        /**
         * Multiplier for the line height of blank lines. down * blankLineHeight is used as the distance to move down for a blank
         * line.
         */
        public float blankLineScale = 1;
        public float scaleX = 1, scaleY = 1;
        public boolean markupEnabled;
        /**
         * The amount to add to the glyph X position when drawing a cursor between glyphs. This field is not set by the BMFont
         * file, it needs to be set manually depending on how the glyphs are rendered on the backing textures.
         */
        public float cursorX;
        /** The glyph to display for characters not in the font. May be null. */
        public Glyph missingGlyph;

        /** The width of the space character. */
        public float spaceXadvance;
        /** The x-height, which is the distance from the top of most lowercase characters to the baseline. */
        public float xHeight = 1;

        /** Additional characters besides whitespace where text is wrapped. Eg, a hypen (-). */
        public char[] breakChars;
        public char[] xChars = {'x', 'e', 'a', 'o', 'n', 's', 'r', 'c', 'u', 'm', 'v', 'w', 'z', '\uF15C', ' '};
        public char[] capChars = {'M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q', 'R', 'S',
        'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        /**
         * Creates an empty BitmapFontData for configuration before calling {@link #load(Fi, boolean)}, to subclass, or to
         * populate yourself, e.g. using stb-truetype or FreeType.
         */
        public FontData(){
        }

        public FontData(Fi fontFile, boolean flip){
            this.fontFile = fontFile;
            this.flipped = flip;
            load(fontFile, flip);
        }

        public void setOverride(FontData override){
            //not implemented for non-freetype fonts
        }

        public void addFallback(FontData data){
            //not implemented for non-freetype fonts
        }

        public void load(Fi fontFile, boolean flip){
            if(imagePaths != null) throw new IllegalStateException("Already loaded.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(fontFile.read()), 512);
            try{
                String line = reader.readLine(); // info
                if(line == null) throw new ArcRuntimeException("File is empty.");

                line = line.substring(line.indexOf("padding=") + 8);
                String[] padding = line.substring(0, line.indexOf(' ')).split(",", 4);
                if(padding.length != 4) throw new ArcRuntimeException("Invalid padding.");
                padTop = Integer.parseInt(padding[0]);
                padRight = Integer.parseInt(padding[1]);
                padBottom = Integer.parseInt(padding[2]);
                padLeft = Integer.parseInt(padding[3]);
                float padY = padTop + padBottom;

                line = reader.readLine();
                if(line == null) throw new ArcRuntimeException("Missing common header.");
                String[] common = line.split(" ", 7); // At most we want the 6th element; i.e. "page=N"

                // At least lineHeight and base are required.
                if(common.length < 3) throw new ArcRuntimeException("Invalid common header.");

                if(!common[1].startsWith("lineHeight=")) throw new ArcRuntimeException("Missing: lineHeight");
                lineHeight = Integer.parseInt(common[1].substring(11));

                if(!common[2].startsWith("base=")) throw new ArcRuntimeException("Missing: base");
                float baseLine = Integer.parseInt(common[2].substring(5));

                int pageCount = 1;
                if(common.length >= 6 && common[5] != null && common[5].startsWith("pages=")){
                    try{
                        pageCount = Math.max(1, Integer.parseInt(common[5].substring(6)));
                    }catch(NumberFormatException ignored){ // Use one page.
                    }
                }

                imagePaths = new String[pageCount];

                // Read each page definition.
                for(int p = 0; p < pageCount; p++){
                    // Read each "page" info line.
                    line = reader.readLine();
                    if(line == null) throw new ArcRuntimeException("Missing additional page definitions.");

                    // Expect ID to mean "index".
                    Matcher matcher = Pattern.compile(".*id=(\\d+)").matcher(line);
                    if(matcher.find()){
                        String id = matcher.group(1);
                        try{
                            int pageID = Integer.parseInt(id);
                            if(pageID != p)
                                throw new ArcRuntimeException("Page IDs must be indices starting at 0: " + id);
                        }catch(NumberFormatException ex){
                            throw new ArcRuntimeException("Invalid page id: " + id, ex);
                        }
                    }

                    matcher = Pattern.compile(".*file=\"?([^\"]+)\"?").matcher(line);
                    if(!matcher.find()) throw new ArcRuntimeException("Missing: file");
                    String fileName = matcher.group(1);

                    imagePaths[p] = fontFile.parent().child(fileName).path().replaceAll("\\\\", "/");
                }
                descent = 0;

                while(true){
                    line = reader.readLine();
                    if(line == null) break; // EOF
                    if(line.startsWith("kernings ")) break; // Starting kernings block.
                    if(!line.startsWith("char ")) continue;

                    Glyph glyph = new Glyph();

                    StringTokenizer tokens = new StringTokenizer(line, " =");
                    tokens.nextToken();
                    tokens.nextToken();
                    int ch = Integer.parseInt(tokens.nextToken());
                    if(ch <= 0)
                        missingGlyph = glyph;
                    else if(ch <= Character.MAX_VALUE)
                        setGlyph(ch, glyph);
                    else
                        continue;
                    glyph.id = ch;
                    tokens.nextToken();
                    glyph.srcX = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.srcY = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.width = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.height = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.xoffset = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    if(flip)
                        glyph.yoffset = Integer.parseInt(tokens.nextToken());
                    else
                        glyph.yoffset = -(glyph.height + Integer.parseInt(tokens.nextToken()));
                    tokens.nextToken();
                    glyph.xadvance = Integer.parseInt(tokens.nextToken());

                    // Check for page safely, it could be omitted or invalid.
                    if(tokens.hasMoreTokens()) tokens.nextToken();
                    if(tokens.hasMoreTokens()){
                        try{
                            glyph.page = Integer.parseInt(tokens.nextToken());
                        }catch(NumberFormatException ignored){
                        }
                    }

                    if(glyph.width > 0 && glyph.height > 0) descent = Math.min(baseLine + glyph.yoffset, descent);
                }
                descent += padBottom;

                while(true){
                    line = reader.readLine();
                    if(line == null) break;
                    if(!line.startsWith("kerning ")) break;

                    StringTokenizer tokens = new StringTokenizer(line, " =");
                    tokens.nextToken();
                    tokens.nextToken();
                    int first = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    int second = Integer.parseInt(tokens.nextToken());
                    if(first < 0 || first > Character.MAX_VALUE || second < 0 || second > Character.MAX_VALUE) continue;
                    Glyph glyph = getGlyph((char)first);
                    tokens.nextToken();
                    int amount = Integer.parseInt(tokens.nextToken());
                    if(glyph != null){ // Kernings may exist for glyph pairs not contained in the font.
                        glyph.setKerning(second, amount);
                    }
                }

                Glyph spaceGlyph = getGlyph(' ');
                if(spaceGlyph == null){
                    spaceGlyph = new Glyph();
                    spaceGlyph.id = ' ';
                    Glyph xadvanceGlyph = getGlyph('l');
                    if(xadvanceGlyph == null) xadvanceGlyph = getFirstGlyph();
                    spaceGlyph.xadvance = xadvanceGlyph.xadvance;
                    setGlyph(' ', spaceGlyph);
                }
                if(spaceGlyph.width == 0){
                    spaceGlyph.width = (int)(padLeft + spaceGlyph.xadvance + padRight);
                    spaceGlyph.xoffset = (int)-padLeft;
                }
                spaceXadvance = spaceGlyph.xadvance;

                Glyph xGlyph = null;
                for(char xChar : xChars){
                    xGlyph = getGlyph(xChar);
                    if(xGlyph != null) break;
                }
                if(xGlyph == null) xGlyph = getFirstGlyph();
                xHeight = xGlyph.height - padY;

                Glyph capGlyph = null;
                for(char capChar : capChars){
                    capGlyph = getGlyph(capChar);
                    if(capGlyph != null) break;
                }
                if(capGlyph == null){
                    for(Glyph[] page : this.glyphs){
                        if(page == null) continue;
                        for(Glyph glyph : page){
                            if(glyph == null || glyph.height == 0 || glyph.width == 0) continue;
                            capHeight = Math.max(capHeight, glyph.height);
                        }
                    }
                }else
                    capHeight = capGlyph.height;
                capHeight -= padY;

                ascent = baseLine - capHeight;
                down = -lineHeight;
                if(flip){
                    ascent = -ascent;
                    down = -down;
                }
            }catch(Exception ex){
                throw new ArcRuntimeException("Error loading font file: " + fontFile, ex);
            }finally{
                Streams.close(reader);
            }
        }

        public void setGlyphRegion(Glyph glyph, TextureRegion region){
            Texture texture = region.texture;
            float invTexWidth = 1.0f / texture.width;
            float invTexHeight = 1.0f / texture.height;

            float offsetX = 0, offsetY = 0;
            float u = region.u;
            float v = region.v;
            float regionWidth = region.width;
            float regionHeight = region.height;
            if(region instanceof AtlasRegion){
                // Compensate for whitespace stripped from left and top edges.
                AtlasRegion atlasRegion = (AtlasRegion)region;
                offsetX = atlasRegion.offsetX;
                offsetY = atlasRegion.originalHeight - atlasRegion.packedHeight - atlasRegion.offsetY;
            }

            float x = glyph.srcX;
            float x2 = glyph.srcX + glyph.width;
            float y = glyph.srcY;
            float y2 = glyph.srcY + glyph.height;

            // Shift glyph for left and top edge stripped whitespace. Clip glyph for right and bottom edge stripped whitespace.
            // Note if the font region has padding, whitespace stripping must not be used.
            if(offsetX > 0){
                x -= offsetX;
                if(x < 0){
                    glyph.width += x;
                    glyph.xoffset -= x;
                    x = 0;
                }
                x2 -= offsetX;
                if(x2 > regionWidth){
                    glyph.width -= x2 - regionWidth;
                    x2 = regionWidth;
                }
            }
            if(offsetY > 0){
                y -= offsetY;
                if(y < 0){
                    glyph.height += y;
                    if(glyph.height < 0) glyph.height = 0;
                    y = 0;
                }
                y2 -= offsetY;
                if(y2 > regionHeight){
                    float amount = y2 - regionHeight;
                    glyph.height -= amount;
                    glyph.yoffset += amount;
                    y2 = regionHeight;
                }
            }

            glyph.texture = texture;
            glyph.u = u + x * invTexWidth;
            glyph.u2 = u + x2 * invTexWidth;
            if(flipped){
                glyph.v = v + y * invTexHeight;
                glyph.v2 = v + y2 * invTexHeight;
            }else{
                glyph.v2 = v + y * invTexHeight;
                glyph.v = v + y2 * invTexHeight;
            }
        }

        /** Sets the line height, which is the distance from one line of text to the next. */
        public void setLineHeight(float height){
            lineHeight = height * scaleY;
            down = flipped ? lineHeight : -lineHeight;
        }

        public void setGlyph(int ch, Glyph glyph){
            Glyph[] page = glyphs[ch / PAGE_SIZE];
            if(page == null) glyphs[ch / PAGE_SIZE] = page = new Glyph[PAGE_SIZE];
            page[ch & PAGE_SIZE - 1] = glyph;
        }

        public Glyph getFirstGlyph(){
            for(Glyph[] page : this.glyphs){
                if(page == null) continue;
                for(Glyph glyph : page){
                    if(glyph == null || glyph.height == 0 || glyph.width == 0) continue;
                    return glyph;
                }
            }
            throw new ArcRuntimeException("No glyphs found.");
        }

        /** Returns true if the font has the glyph, or if the font has a {@link #missingGlyph}. */
        public boolean hasGlyph(char ch){
            if(missingGlyph != null) return true;
            return getGlyph(ch) != null;
        }

        /**
         * Returns the glyph for the specified character, or null if no such glyph exists. Note that
         * {@link #getGlyphs(GlyphRun, CharSequence, int, int, Glyph)} should be be used to shape a string of characters into a list
         * of glyphs.
         */
        public Glyph getGlyph(char ch){
            Glyph[] page = glyphs[ch / PAGE_SIZE];
            if(page != null) return page[ch & PAGE_SIZE - 1];
            return null;
        }

        /**
         * Using the specified string, populates the glyphs and positions of the specified glyph run.
         * @param str Characters to convert to glyphs. Will not contain newline or color tags. May contain "[[" for an escaped left
         * square bracket.
         * @param lastGlyph The glyph immediately before this run, or null if this is run is the first on a line of text.
         */
        public void getGlyphs(GlyphRun run, CharSequence str, int start, int end, Glyph lastGlyph){
            boolean markupEnabled = this.markupEnabled;
            float scaleX = this.scaleX;
            Glyph missingGlyph = this.missingGlyph;
            Seq<Glyph> glyphs = run.glyphs;
            FloatSeq xAdvances = run.xAdvances;

            // Guess at number of glyphs needed.
            glyphs.ensureCapacity(end - start);
            xAdvances.ensureCapacity(end - start + 1);

            while(start < end){
                char ch = str.charAt(start++);
                Glyph glyph = getGlyph(ch);
                if(glyph == null){
                    if(missingGlyph == null) continue;
                    glyph = missingGlyph;
                }

                glyphs.add(glyph);

                if(lastGlyph == null) // First glyph on line, adjust the position so it isn't drawn left of 0.
                    xAdvances.add(glyph.fixedWidth ? 0 : -glyph.xoffset * scaleX - padLeft);
                else
                    xAdvances.add((lastGlyph.xadvance + lastGlyph.getKerning(ch)) * scaleX);
                lastGlyph = glyph;

                // "[[" is an escaped left square bracket, skip second character.
                if(markupEnabled && ch == '[' && start < end && str.charAt(start) == '[') start++;
            }
            if(lastGlyph != null){
                float lastGlyphWidth = lastGlyph.fixedWidth ? lastGlyph.xadvance * scaleX
                : (lastGlyph.width + lastGlyph.xoffset) * scaleX - padRight;
                xAdvances.add(lastGlyphWidth);
            }
        }

        /**
         * Returns the first valid glyph index to use to wrap to the next line, starting at the specified start index and
         * (typically) moving toward the beginning of the glyphs array.
         */
        public int getWrapIndex(Seq<Glyph> glyphs, int start){
            int i = start - 1;
            if(isWhitespace((char)glyphs.get(i).id)) return i;
            for(; i > 0; i--)
                if(!isWhitespace((char)glyphs.get(i).id)) break;
            for(; i > 0; i--){
                char ch = (char)glyphs.get(i).id;
                if(isWhitespace(ch) || isBreakChar(ch)) return i + 1;
            }
            return 0;
        }

        public boolean isBreakChar(char c){
            if(breakChars == null) return false;
            for(char br : breakChars)
                if(c == br) return true;
            return false;
        }

        public boolean isWhitespace(char c){
            switch(c){
                case '\n':
                case '\r':
                case '\t':
                case ' ':
                    return true;
                default:
                    return false;
            }
        }

        /** Returns the image path for the texture page at the given index (the "id" in the BMFont file). */
        public String getImagePath(int index){
            return imagePaths[index];
        }

        public String[] getImagePaths(){
            return imagePaths;
        }

        public Fi getFontFile(){
            return fontFile;
        }

        /**
         * Scales the font by the specified amounts on both axes
         * <p>
         * Note that smoother scaling can be achieved if the texture backing the BitmapFont is using {@link TextureFilter#linear}.
         * The default is nearest, so use a BitmapFont constructor that takes a {@link TextureRegion}.
         * @throws IllegalArgumentException if scaleX or scaleY is zero.
         */
        public void setScale(float scaleX, float scaleY){
            if(scaleX == 0) throw new IllegalArgumentException("scaleX cannot be 0.");
            if(scaleY == 0) throw new IllegalArgumentException("scaleY cannot be 0.");
            float x = scaleX / this.scaleX;
            float y = scaleY / this.scaleY;
            lineHeight *= y;
            spaceXadvance *= x;
            xHeight *= y;
            capHeight *= y;
            ascent *= y;
            descent *= y;
            down *= y;
            padLeft *= x;
            padRight *= x;
            padTop *= y;
            padBottom *= y;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        /**
         * Scales the font by the specified amount in both directions.
         * @throws IllegalArgumentException if scaleX or scaleY is zero.
         * @see #setScale(float, float)
         */
        public void setScale(float scaleXY){
            setScale(scaleXY, scaleXY);
        }

        /**
         * Sets the font's scale relative to the current scale.
         * @throws IllegalArgumentException if the resulting scale is zero.
         * @see #setScale(float, float)
         */
        public void scale(float amount){
            setScale(scaleX + amount, scaleY + amount);
        }
    }
}
