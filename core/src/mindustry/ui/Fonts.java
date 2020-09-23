package mindustry.ui;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.assets.loaders.resolvers.*;
import arc.files.*;
import arc.freetype.*;
import arc.freetype.FreeTypeFontGenerator.*;
import arc.freetype.FreetypeFontLoader.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.graphics.g2d.PixmapPacker.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;

import java.util.*;

public class Fonts{
    private static final String mainFont = "fonts/font.woff";
    private static ObjectIntMap<String> unicodeIcons = new ObjectIntMap<>();
    private static ObjectMap<String, String> stringIcons = new ObjectMap<>();

    public static Font def;
    public static Font outline;
    public static Font chat;
    public static Font icon;
    public static Font tech;

    public static int getUnicode(String content){
        return unicodeIcons.get(content, 0);
    }

    public static String getUnicodeStr(String content){
        return stringIcons.get(content, "");
    }

    /** Called from a static context to make the cursor appear immediately upon startup.*/
    public static void loadSystemCursors(){
        SystemCursor.arrow.set(Core.graphics.newCursor("cursor", cursorScale()));
        SystemCursor.hand.set(Core.graphics.newCursor("hand", cursorScale()));
        SystemCursor.ibeam.set(Core.graphics.newCursor("ibeam", cursorScale()));

        Core.graphics.restoreCursor();
    }

    public static int cursorScale(){
        return Math.max(1, Mathf.round(Scl.scl(1f)));
    }

    public static void loadFonts(){
        FreeTypeFontParameter param = fontParameter();

        Core.assets.load("default", Font.class, new FreeTypeFontLoaderParameter(mainFont, param)).loaded = f -> Fonts.def = (Font)f;
        Core.assets.load("chat", Font.class, new FreeTypeFontLoaderParameter(mainFont, param)).loaded = f -> Fonts.chat = (Font)f;
        Core.assets.load("icon", Font.class, new FreeTypeFontLoaderParameter("fonts/icon.ttf", new FreeTypeFontParameter(){{
            size = 30;
            incremental = true;
            characters = "\0";
        }})).loaded = f -> Fonts.icon = (Font)f;
    }

    public static void loadContentIcons(){
        Seq<Font> fonts = Seq.with(Fonts.chat, Fonts.def, Fonts.outline);
        Texture uitex = Core.atlas.find("logo").texture;
        int size = (int)(Fonts.def.getData().lineHeight/Fonts.def.getData().scaleY);

        try(Scanner scan = new Scanner(Core.files.internal("icons/icons.properties").read(512))){
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                String[] split = line.split("=");
                String[] nametex = split[1].split("\\|");
                String character = split[0], texture = nametex[1];
                int ch = Integer.parseInt(character);
                TextureRegion region = Core.atlas.find(texture);

                if(region.texture != uitex){
                    continue;
                    //throw new IllegalArgumentException("Font icon '" + texture + "' is not in the UI texture.");
                }

                unicodeIcons.put(nametex[0], ch);
                stringIcons.put(nametex[0], ((char)ch) + "");

                Glyph glyph = new Glyph();
                glyph.id = ch;
                glyph.srcX = 0;
                glyph.srcY = 0;
                glyph.width = size;
                glyph.height = size;
                glyph.u = region.u;
                glyph.v = region.v2;
                glyph.u2 = region.u2;
                glyph.v2 = region.v;
                glyph.xoffset = 0;
                glyph.yoffset = -size;
                glyph.xadvance = size;
                glyph.kerning = null;
                glyph.fixedWidth = true;
                glyph.page = 0;
                fonts.each(f -> f.getData().setGlyph(ch, glyph));
            }
        }
    }

    /** Called from a static context for use in the loading screen.*/
    public static void loadDefaultFont(){
        int max = Gl.getInt(Gl.maxTextureSize);

        UI.packer = new PixmapPacker(max >= 4096 ? 4096 : 2048, 2048, Format.rgba8888, 2, true);
        FileHandleResolver resolver = new InternalFileHandleResolver();
        Core.assets.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        Core.assets.setLoader(Font.class, null, new FreetypeFontLoader(resolver){
            ObjectSet<FreeTypeFontParameter> scaled = new ObjectSet<>();

            @Override
            public Font loadSync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
                if(fileName.equals("outline")){
                    parameter.fontParameters.borderWidth = Scl.scl(2f);
                    parameter.fontParameters.spaceX -= parameter.fontParameters.borderWidth;
                }
                if(!scaled.contains(parameter.fontParameters)){
                    parameter.fontParameters.size = (int)(Scl.scl(parameter.fontParameters.size));
                    scaled.add(parameter.fontParameters);
                }

                parameter.fontParameters.magFilter = TextureFilter.linear;
                parameter.fontParameters.minFilter = TextureFilter.linear;
                parameter.fontParameters.packer = UI.packer;
                return super.loadSync(manager, fileName, file, parameter);
            }
        });

        FreeTypeFontParameter param = new FreeTypeFontParameter(){{
            borderColor = Color.darkGray;
            incremental = true;
            size = 18;
        }};

        Core.assets.load("outline", Font.class, new FreeTypeFontLoaderParameter(mainFont, param)).loaded = t -> Fonts.outline = (Font)t;

        Core.assets.load("tech", Font.class, new FreeTypeFontLoaderParameter("fonts/tech.ttf", new FreeTypeFontParameter(){{
            size = 18;
        }})).loaded = f -> {
            Fonts.tech = (Font)f;
            ((Font)f).getData().down *= 1.5f;
        };
    }

    /** Merges the UI and font atlas together for better performance. */
    public static void mergeFontAtlas(TextureAtlas atlas){
        //grab all textures from the ui page, remove all the regions assigned to it, then copy them over to Fonts.packer and replace the texture in this atlas.

        //grab old UI texture and regions...
        Texture texture = atlas.find("logo").texture;

        Page page = UI.packer.getPages().first();

        Seq<AtlasRegion> regions = atlas.getRegions().select(t -> t.texture == texture);
        for(AtlasRegion region : regions){
            //get new pack rect
            page.setDirty(false);
            Rect rect = UI.packer.pack(region.name + (region.splits != null ? ".9" : ""), atlas.getPixmap(region));
            //set new texture
            region.texture = UI.packer.getPages().first().getTexture();
            //set its new position
            region.set((int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);
            //add old texture
            atlas.getTextures().add(region.texture);
        }

        //remove old texture, it will no longer be used
        atlas.getTextures().remove(texture);
        texture.dispose();
        atlas.disposePixmap(texture);

        page.setDirty(true);
        page.updateTexture(TextureFilter.linear, TextureFilter.linear, false);
    }

    public static TextureRegionDrawable getGlyph(Font font, char glyph){
        Glyph g = font.getData().getGlyph(glyph);
        if(g == null) throw new IllegalArgumentException("No glyph: " + glyph + " (" + (int)glyph + ")");

        float size = Math.max(g.width, g.height);
        TextureRegionDrawable draw = new TextureRegionDrawable(new TextureRegion(font.getRegion().texture, g.u, g.v2, g.u2, g.v)){
            @Override
            public void draw(float x, float y, float width, float height){
                Draw.color(Tmp.c1.set(tint).mul(Draw.getColor()).toFloatBits());
                float cx = x + width/2f - g.width/2f, cy = y + height/2f - g.height/2f;
                cx = (int)cx;
                cy = (int)cy;
                Draw.rect(region, cx + g.width/2f, cy + g.height/2f, g.width, g.height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
                width *= scaleX;
                height *= scaleY;
                Draw.color(Tmp.c1.set(tint).mul(Draw.getColor()).toFloatBits());
                float cx = x + width/2f - g.width/2f, cy = y + height/2f - g.height/2f;
                cx = (int)cx;
                cy = (int)cy;
                originX = g.width/2f;
                originY = g.height/2f;
                Draw.rect(region, cx + g.width/2f, cy + g.height/2f, g.width, g.height, originX, originY, rotation);
            }

            @Override
            public float imageSize(){
                return size;
            }
        };

        draw.setMinWidth(size);
        draw.setMinHeight(size);
        return draw;
    }

    static FreeTypeFontParameter fontParameter(){
        return new FreeTypeFontParameter(){{
            size = 18;
            shadowColor = Color.darkGray;
            shadowOffsetY = 2;
            incremental = true;
        }};
    }
}
