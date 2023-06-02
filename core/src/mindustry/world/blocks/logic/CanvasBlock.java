package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class CanvasBlock extends Block{
    public float padding = 0f;
    public int canvasSize = 8;
    public int[] palette = {0x362944_ff, 0xc45d9f_ff, 0xe39aac_ff, 0xf0dab1_ff, 0x6461c2_ff, 0x2ba9b4_ff, 0x93d4b5_ff, 0xf0f6e8_ff};
    public int bitsPerPixel;
    public IntIntMap colorToIndex = new IntIntMap();

    public @Load("@-side1") TextureRegion side1;
    public @Load("@-side2") TextureRegion side2;

    public @Load("@-corner1") TextureRegion corner1;
    public @Load("@-corner2") TextureRegion corner2;

    protected @Nullable Pixmap previewPixmap;
    protected @Nullable Texture previewTexture;
    protected int tempBlend = 0;

    public CanvasBlock(String name){
        super(name);

        configurable = true;
        destructible = true;
        canOverdrive = false;
        solid = true;

        config(byte[].class, (CanvasBuild build, byte[] bytes) -> {
            if(build.data.length == bytes.length){
                build.data = bytes;
                build.updateTexture();
            }
        });
    }

    @Override
    public void init(){
        super.init();

        for(int i = 0; i < palette.length; i++){
            colorToIndex.put(palette[i], i);
        }
        bitsPerPixel = Mathf.log2(Mathf.nextPowerOfTwo(palette.length));
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        //only draw the preview in schematics, as it lags otherwise
        if(!plan.worldContext && plan.config instanceof byte[] data){
            Pixmap pix = makePixmap(data);

            if(previewTexture == null){
                previewTexture = new Texture(pix);
            }else{
                previewTexture.draw(pix);
            }

            tempBlend = 0;

            //O(N^2), awful
            list.each(other -> {
                if(other.block == this){
                    for(int i = 0; i < 4; i++){
                        if(other.x == plan.x + Geometry.d4x(i) * size && other.y == plan.y + Geometry.d4y(i) * size){
                            tempBlend |= (1 << i);
                        }
                    }
                }
            });

            int blending = tempBlend;

            float x = plan.drawx(), y = plan.drawy();
            Tmp.tr1.set(previewTexture);
            float pad = blending == 0 ? padding : 0f;

            Draw.rect(Tmp.tr1, x, y, size * tilesize - pad, size * tilesize - pad);
            Draw.flush(); //texture is reused, so flush it now

            //code duplication, awful
            for(int i = 0; i < 4; i ++){
                if((blending & (1 << i)) == 0){
                    Draw.rect(i >= 2 ? side2 : side1, x, y, i * 90);

                    if((blending & (1 << ((i + 1) % 4))) != 0){
                        Draw.rect(i >= 2 ? corner2 : corner1, x, y, i * 90);
                    }

                    if((blending & (1 << (Mathf.mod(i - 1, 4)))) != 0){
                        Draw.yscl = -1f;
                        Draw.rect(i >= 2 ? corner2 : corner1, x, y, i * 90);
                        Draw.yscl = 1f;
                    }
                }
            }

        }else{
            super.drawPlanRegion(plan, list);
        }
    }

    /** returns the same pixmap instance each time, use with care */
    public Pixmap makePixmap(byte[] data){
        if(previewPixmap == null){
            previewPixmap = new Pixmap(canvasSize, canvasSize);
        }

        int bpp = bitsPerPixel;
        int pixels = canvasSize * canvasSize;
        for(int i = 0; i < pixels; i++){
            int bitOffset = i * bpp;
            int pal = getByte(data, bitOffset);
            previewPixmap.set(i % canvasSize, i / canvasSize, palette[pal]);
        }
        return previewPixmap;
    }

    protected int getByte(byte[] data, int bitOffset){
        int result = 0, bpp = bitsPerPixel;
        for(int i = 0; i < bpp; i++){
            int word = i + bitOffset >>> 3;
            result |= (((data[word] & (1 << (i + bitOffset & 7))) == 0 ? 0 : 1) << i);
        }
        return result;
    }

    public class CanvasBuild extends Building{
        public @Nullable Texture texture;
        public byte[] data = new byte[Mathf.ceil(canvasSize * canvasSize * bitsPerPixel / 8f)];
        public int blending;

        public void updateTexture(){
            if(headless) return;

            Pixmap pix = makePixmap(data);
            if(texture != null){
                texture.draw(pix);
            }else{
                texture = new Texture(pix);
            }
        }

        public byte[] packPixmap(Pixmap pixmap){
            byte[] bytes = new byte[data.length];
            int pixels = canvasSize * canvasSize;
            for(int i = 0; i < pixels; i++){
                int color = pixmap.get(i % canvasSize, i / canvasSize);
                int palIndex = colorToIndex.get(color);
                setByte(bytes, i * bitsPerPixel, palIndex);
            }
            return bytes;
        }

        protected void setByte(byte[] bytes, int bitOffset, int value){
            int bpp = bitsPerPixel;
            for(int i = 0; i < bpp; i++){
                int word = i + bitOffset >>> 3;

                if(((value >>> i) & 1) == 0){
                    bytes[word] &= ~(1 << (i + bitOffset & 7));
                }else{
                    bytes[word] |= (1 << (i + bitOffset & 7));
                }
            }
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            blending = 0;
            for(int i = 0; i < 4; i++){
                if(blends(world.tile(tile.x + Geometry.d4[i].x * size, tile.y + Geometry.d4[i].y * size))) blending |= (1 << i);
            }
        }

        boolean blends(Tile other){
            return other != null && other.build != null && other.build.block == block && other.build.tileX() == other.x && other.build.tileY() == other.y;
        }

        @Override
        public void draw(){
            if(!renderer.drawDisplays){
                super.draw();

                return;
            }

            if(blending == 0){
                super.draw();
            }

            if(texture == null){
                updateTexture();
            }
            Tmp.tr1.set(texture);
            float pad = blending == 0 ? padding : 0f;

            Draw.rect(Tmp.tr1, x, y, size * tilesize - pad, size * tilesize - pad);
            for(int i = 0; i < 4; i ++){
                if((blending & (1 << i)) == 0){
                    Draw.rect(i >= 2 ? side2 : side1, x, y, i * 90);

                    if((blending & (1 << ((i + 1) % 4))) != 0){
                        Draw.rect(i >= 2 ? corner2 : corner1, x, y, i * 90);
                    }

                    if((blending & (1 << (Mathf.mod(i - 1, 4)))) != 0){
                        Draw.yscl = -1f;
                        Draw.rect(i >= 2 ? corner2 : corner1, x, y, i * 90);
                        Draw.yscl = 1f;
                    }
                }
            }
        }

        @Override
        public void remove(){
            super.remove();
            if(texture != null){
                texture.dispose();
                texture = null;
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, Styles.cleari, () -> {
                Dialog dialog = new Dialog();

                Pixmap pix = makePixmap(data);
                Texture texture = new Texture(pix);
                int[] curColor = {palette[0]};
                boolean[] modified = {false};

                dialog.resized(dialog::hide);

                dialog.cont.table(Tex.pane, body -> {
                    body.stack(new Element(){
                        int lastX, lastY;

                        {
                            addListener(new InputListener(){
                                int convertX(float ex){
                                    return (int)((ex - x) / width * canvasSize);
                                }

                                int convertY(float ey){
                                    return pix.height - 1 - (int)((ey - y) / height * canvasSize);
                                }

                                @Override
                                public boolean touchDown(InputEvent event, float ex, float ey, int pointer, KeyCode button){
                                    int cx = convertX(ex), cy = convertY(ey);
                                    draw(cx, cy);
                                    lastX = cx;
                                    lastY = cy;
                                    return true;
                                }

                                @Override
                                public void touchDragged(InputEvent event, float ex, float ey, int pointer){
                                    int cx = convertX(ex), cy = convertY(ey);
                                    Bresenham2.line(lastX, lastY, cx, cy, (x, y) -> draw(x, y));
                                    lastX = cx;
                                    lastY = cy;
                                }
                            });
                        }

                        void draw(int x, int y){
                            if(pix.get(x, y) != curColor[0]){
                                pix.set(x, y, curColor[0]);
                                Pixmaps.drawPixel(texture, x, y, curColor[0]);
                                modified[0] = true;
                            }
                        }

                        @Override
                        public void draw(){
                            Tmp.tr1.set(texture);
                            Draw.alpha(parentAlpha);
                            Draw.rect(Tmp.tr1, x + width/2f, y + height/2f, width, height);
                        }
                    }, new GridImage(canvasSize, canvasSize){{
                        touchable = Touchable.disabled;
                    }}).size(mobile && !Core.graphics.isPortrait() ? Math.min(290f, Core.graphics.getHeight() / Scl.scl(1f) - 75f / Scl.scl(1f)) : 480f);
                });

                dialog.cont.row();

                dialog.cont.table(Tex.button, p -> {
                    for(int i = 0; i < palette.length; i++){
                        int fi = i;

                        var button = p.button(Tex.whiteui, Styles.squareTogglei, 30, () -> {
                            curColor[0] = palette[fi];
                        }).size(44).checked(b -> curColor[0] == palette[fi]).get();
                        button.getStyle().imageUpColor = new Color(palette[i]);
                    }
                });

                dialog.closeOnBack();

                dialog.buttons.defaults().size(150f, 64f);
                dialog.buttons.button("@cancel", Icon.cancel, dialog::hide);
                dialog.buttons.button("@ok", Icon.ok, () -> {
                    if(modified[0]){
                        configure(packPixmap(pix));
                        pix.dispose();
                        texture.dispose();
                    }
                    dialog.hide();
                });

                dialog.show();
            }).size(40f);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                deselect();
                return false;
            }

            return true;
        }

        @Override
        public byte[] config(){
            return data;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            //for future canvas resizing events
            write.i(data.length);
            write.b(data);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            int len = read.i();
            if(data.length == len){
                read.b(data);
            }else{
                read.skip(len);
            }
        }
    }
}
