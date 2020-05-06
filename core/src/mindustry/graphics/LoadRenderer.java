package mindustry.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.GLVersion.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.graphics.g3d.*;

import static arc.Core.*;

public class LoadRenderer{
    private static final Color color = new Color(Pal.accent).lerp(Color.black, 0.5f);
    private static final Color colorRed = Pal.breakInvalid.lerp(Color.black, 0.3f);
    private static final String red = "[#" + colorRed + "]";
    private static final String orange = "[#" + color + "]";
    private static final FloatArray floats = new FloatArray();

    private float testprogress = 0f;
    private float smoothProgress;

    private StringBuilder assetText = new StringBuilder();
    private Bar[] bars;
    private Mesh mesh = MeshBuilder.buildHex(colorRed, 2, true, 1f);//MeshBuilder.buildIcosphere(2, 1f, colorRed);
    private Camera3D cam = new Camera3D();
    private int lastLength = -1;

    {
        bars = new Bar[]{
            new Bar("s_proc#", OS.cores / 16f, OS.cores < 4),
            new Bar("c_aprog", () -> assets != null, () -> assets.getProgress(), () -> false),
            new Bar("g_vtype", graphics.getGLVersion().getType() == Type.GLES ? 0.5f : 1f, graphics.getGLVersion().getType() == Type.GLES),
            new Bar("s_mem#", () -> true, () -> Core.app.getJavaHeap() / 1024f / 1024f / 200f, () -> Core.app.getJavaHeap() > 1024*1024*110),
            new Bar("v_ver#", () -> Version.build != 0, () -> Version.build == -1 ? 0.3f : (Version.build - 103f) / 10f, () -> !Version.modifier.equals("release")),
            new Bar("s_osv", OS.isWindows ? 0.35f : OS.isLinux ? 0.9f : OS.isMac ? 0.5f : 0.2f, OS.isMac),
            new Bar("v_worlds#", () -> Vars.control != null && Vars.control.saves != null, () -> Vars.control.saves.getSaveSlots().size / 30f, () -> Vars.control.saves.getSaveSlots().size > 30),
            new Bar("c_datas#", () -> settings.keySize() > 0, () -> settings.keySize() / 50f, () -> settings.keySize() > 20),
            new Bar("v_alterc", () -> Vars.mods != null, () -> (Vars.mods.list().size + 1) / 6f, () -> Vars.mods.list().size > 0),
            new Bar("g_vcomp#", (graphics.getGLVersion().getMajorVersion() + graphics.getGLVersion().getMinorVersion() / 10f) / 4.6f, !graphics.getGLVersion().isVersionEqualToOrHigher(3, 2)),
        };
    }

    public void draw(){
        if(assets.getLoadedAssets() != lastLength){
            assetText.setLength(0);
            for(String name : assets.getAssetNames()){
                boolean isRed = name.toLowerCase().contains("mod") || assets.getAssetType(name).getSimpleName().toLowerCase().contains("mod") || name.contains("preview");
                assetText
                .append(isRed ? red : orange)
                .append(name.replace(OS.username, "<<host>>").replace("/", "::")).append(red).append("::[]")
                .append(assets.getAssetType(name).getSimpleName()).append("\n");
            }

            lastLength = assets.getLoadedAssets();
        }

        smoothProgress = Mathf.lerpDelta(smoothProgress, assets.getProgress(), 0.1f);

        Core.graphics.clear(Color.black);

        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight(), s = Scl.scl();
        Lines.precise(true);

        Draw.proj().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

        int lightVerts = 20;
        float lightRad = Math.max(w, h)*0.6f;
        float stroke = 5f * s;

        //light
        if(false){
            Fill.light(w/2, h/2, lightVerts, lightRad, Tmp.c1.set(colorRed).a(0.5f), Color.clear);
        }

        float space = Scl.scl(60);
        float progress = assets.getProgress();
        int dotw = (int)(w / space)/2 + 1;
        int doth = (int)(h / space)/2 + 1;

        //TODO remove
        if(true){
            testprogress += Time.delta() / (60f * 3);
            progress = testprogress;
            if(input.keyTap(KeyCode.space)){
                testprogress = 0;
            }
        }

        //dot matrix
        if(false){

            Draw.color(Pal.accent);

            Draw.alpha(0.3f);

            for(int cx = -dotw; cx <= dotw; cx++){
                for(int cy = -doth; cy <= doth; cy++){
                    float dx = cx * space + w/2f, dy = cy * space + h/2f;

                    Fill.square(dx, dy, 1.5f*s, 45);
                }
            }

            Draw.reset();
        }

        //square matrix
        if(true){
            if(true){
                //solid color
                Draw.color(Pal.accent, Color.black, 0.9f);
            }else{
                //alpha color
                Draw.color(Pal.accent, 0.1f);
            }


            Lines.stroke(stroke);

            for(int cx = -dotw; cx <= dotw; cx++){
                for(int cy = -doth; cy <= doth; cy++){
                    float dx = cx * space + w/2f, dy = cy * space + h/2f;

                    Lines.poly(dx, dy, 4, space/2f);
                }
            }
        }

        //bars
        if(false){
            Draw.color(Pal.accent, Color.black, 0.7f);

            for(int cx = -dotw; cx <= dotw; cx++){
                float height = 400f * s * Mathf.randomSeed(cx);

                float dx = cx * space + w/2f, dy = 0;
                Lines.rect(dx - space/2f, dy, space, height, 1*s, 2*s);
            }

            Draw.reset();
        }

        //background text and indicator
        if(true){
            float rads = 110*s;
            float rad = Math.min(Math.min(w, h) / 3.1f, Math.min(w, h)/2f - rads);
            float rad2 = rad + rads;
            float epad = 60f * s;
            float mpad = 100f*s;

            Draw.color(color);
            Lines.stroke(stroke);

            Lines.poly(w/2, h/2, 4, rad);
            Lines.poly(w/2, h/2, 4, rad2);

            int panei = 0;

            for(int sx : Mathf.signs){
                for(int sy : Mathf.signs){
                    float y1 = h/2f + sy*rad2, y2 = h/2f + sy*120f;
                    //Lines.beginLine();
                    floats.clear();

                    if(w > h){ //non-portrait
                        floats.add(w/2f + sx*mpad, y1);
                        floats.add(w/2f + (w/2f-epad)*sx, y1);
                        floats.add(w/2f + (w/2f-epad)*sx, y2);
                        floats.add(w/2f + sx*mpad + sx*Math.abs(y2-y1), y2);
                    }else{ //portrait
                        floats.add(w/2f + sx*mpad, y1);
                        floats.add(w/2f + sx*mpad, h/2f + (h/2f-epad)*sy);
                        floats.add(w/2f + sx*mpad + sx*Math.abs(y2-y1), h/2f + (h/2f-epad)*sy);
                        floats.add(w/2f + sx*mpad + sx*Math.abs(y2-y1), y2);
                    }

                    float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, maxx = 0, maxy = 0;
                    for(int i = 0; i < floats.size; i+= 2){
                        float x = floats.items[i], y = floats.items[i + 1];
                        minx = Math.min(x, minx);
                        miny = Math.min(y, miny);

                        maxx = Math.max(x, maxx);
                        maxy = Math.max(y, maxy);
                    }

                    Draw.flush();
                    Gl.clear(Gl.stencilBufferBit);
                    Draw.beginStencil();

                    Fill.poly(floats);

                    Draw.beginStenciled();

                    if(assets.isLoaded("tech")){
                        BitmapFont font = assets.get("tech");
                        font.getData().markupEnabled = true;

                        GlyphLayout layout = GlyphLayout.obtain();
                        float pad = 4;

                        if(panei == 0){
                            layout.setText(font, assetText);
                            font.draw(assetText, minx + pad, maxy - pad + Math.max(0, layout.height - (maxy - miny)));
                        }else if(panei == 1){
                            float height = maxy - miny;
                            float barpad = s*8f;
                            float barspace = (height - barpad) / bars.length;
                            float barheight = barspace * 0.8f;

                            for(int i = 0; i < bars.length; i++){
                                Bar bar = bars[i];
                                if(bar.valid()){
                                    Draw.color(bar.red() ? colorRed : color);
                                    float y = maxy - i * barspace - barpad - barheight;
                                    float width = Mathf.clamp(bar.value());
                                    float baseWidth = (maxx - minx) - (maxy - y) - barpad*2f - s*4;
                                    float cx = minx + barpad, cy = y, topY = cy + barheight, botY = cy;

                                    Lines.square(cx + barheight/2f, botY + barheight/2f, barheight/2f);

                                    Fill.quad(
                                    cx + barheight, cy,
                                    cx + barheight, topY,
                                    cx + width * baseWidth + barheight, topY,
                                    cx + width * baseWidth, botY
                                    );

                                    Draw.color(Color.black);

                                    Fill.quad(
                                    cx + width * baseWidth + barheight, topY,
                                    cx + width * baseWidth, botY,
                                    cx + baseWidth, botY,
                                    cx + baseWidth + barheight, topY);

                                    font.setColor(Color.black);
                                    layout.setText(font, bar.text);
                                    font.draw(bar.text, cx + barheight*1.5f, botY + barheight/2f + layout.height/2f);
                                }
                            }

                            Draw.color(color);

                            //layout.setText(font, systemInfo);
                            //font.draw(systemInfo, minx + pad, maxy - pad + Math.max(0, layout.height - (maxy - miny)));
                        }else if(panei == 3){
                            Draw.flush();

                            float vx = floats.get(6), vy = floats.get(7), vw = (maxx - vx), vh = (maxy - vy), cx = vx + vw/2f, cy = vy + vh/2f;
                            float vpad = 30*s;
                            float vcont = Math.min(vw, vh);
                            float vsize = vcont - vpad*2;
                            int rx = (int)(vx + vw/2f - vsize/2f), ry = (int)(vy + vh/2f - vsize/2f), rw = (int)vsize, rh = (int)vsize;

                            float vrad = vsize/2f + vpad / 1f;
                            Lines.circle(cx, cy, vsize/2f);

                            if(rw > 0 && rh > 0){
                                Gl.viewport(rx, ry, rw, rh);

                                cam.position.set(2, 0, 2);
                                cam.resize(rw, rh);
                                cam.lookAt(0, 0, 0);
                                cam.fov = 42f;
                                cam.update();
                                Shaders.mesh.bind();
                                Shaders.mesh.setUniformMatrix4("u_proj", cam.combined.val);
                                mesh.render(Shaders.mesh, Gl.lines);

                                //restore viewport
                                Gl.viewport(0, 0, graphics.getWidth(), graphics.getHeight());
                            }

                            int points = 4;
                            for(int i = 0; i < points; i++){
                                float ang = i * 360f/points + 45;
                                Fill.poly(cx + Angles.trnsx(ang, vrad), cy + Angles.trnsy(ang, vrad), 3, 20*s, ang);
                            }

                            String text = "<<ready>>";
                            Draw.color(Color.black);

                            layout.setText(font, text);
                            Fill.rect(cx, cy, layout.width + 14f*s, layout.height + 14f*s);

                            font.setColor(color);
                            font.draw(text, cx - layout.width/2f, cy + layout.height/2f);

                            Draw.color(color);

                            Lines.square(cx, cy, vcont/2f);

                            Lines.line(vx, vy, vx, vy + vh);


                            float pspace = 70f*s;
                            int pcount = (int)(vh / pspace / 2) + 2;
                            float pw = (vw - vcont)/2f;
                            float slope = pw/2f;

                            //side bars for planet
                            for(int i : Mathf.signs){

                                float px = cx + i*(vcont/2f + pw/2f);
                                float xleft = px - pw/2f, xright = px + pw/2f;
                                float offx = minx - xleft, offy = (minx - xleft)/2f;
                                if(i > 0){
                                    offx = 0;
                                    offy = 0;
                                }

                                for(int j = -2; j < pcount*2; j++){
                                    float py = vy + j*pspace*2, ybot = py - slope, ytop = py + slope;
                                    Fill.quad(
                                        xleft, ybot,
                                        xleft, ybot + pspace,
                                        xright, ytop + pspace,
                                        xright, ytop
                                    );
                                }
                            }
                        }

                        layout.free();
                    }else{
                        Core.assets.finishLoadingAsset("tech");
                    }

                    Draw.endStencil();

                    Lines.polyline(floats, true);

                    panei ++;

                }
            }
        }

        //middle display
        if(true){
            float bspace = s * 100f;
            float bsize = s * 80f;
            int bars = (int)(w / bspace / 2) + 1;
            float pscale = 1f / bars;
            float barScale = 1.5f;

            Draw.color(Color.black);
            Fill.rect(w/2, h/2, w, bsize * barScale);
            Lines.stroke(stroke);
            Draw.color(color);
            Lines.rect(0, h/2 - bsize * barScale/2f, w, bsize * barScale, 10, 0);

            for(int i = 1; i < bars; i++){
                float cx = i * bspace;
                float fract = 1f - (i - 1) / (float)(bars - 1);
                float alpha = progress >= fract ? 1f : Mathf.clamp((pscale - (fract - progress)) / pscale);
                Draw.color(Color.black, color, alpha);

                for(int dir : Mathf.signs){
                    float width = bsize/1.7f;
                    float skew = bsize/2f;

                    Fill.rects(w/2 + cx*dir - width/2f + dir*skew, h/2f - bsize/2f + bsize/2f, width, bsize/2f, -dir*skew);
                    Fill.rects(w/2 + cx*dir - width/2f, h/2f - bsize/2f, width, bsize/2f, dir*skew);
                    //Lines.poly(w/2 + cx*dir, h/2f, 3, bsize, 90 + dir*90);
                }

            }
        }


        if(assets.isLoaded("tech")){
            BitmapFont font = assets.get("tech");
            font.setColor(Pal.accent);
            Draw.color(Color.black);
            font.draw(System.getProperty("java.version") + "\n\n[scarlet][[ready]", w/2f, h/2f + 120, Align.center);
        }else{

        }

        /*

        float height = Scl.scl(50f);

        Draw.color(Color.black);
        Fill.poly(graphics.getWidth()/2f, graphics.getHeight()/2f, 6, Mathf.dst(graphics.getWidth()/2f, graphics.getHeight()/2f) * smoothProgress);
        Draw.reset();

        float w = graphics.getWidth()*0.6f;

        Draw.color(Color.black);
        Fill.rect(graphics.getWidth()/2f, graphics.getHeight()/2f, w, height);

        Draw.color(Pal.accent);
        Fill.crect(graphics.getWidth()/2f-w/2f, graphics.getHeight()/2f - height/2f, w * smoothProgress, height);

        for(int i : Mathf.signs){
            Fill.tri(graphics.getWidth()/2f + w/2f*i, graphics.getHeight()/2f + height/2f, graphics.getWidth()/2f + w/2f*i, graphics.getHeight()/2f - height/2f, graphics.getWidth()/2f + w/2f*i + height/2f*i, graphics.getHeight()/2f);
        }

        if(assets.isLoaded("outline")){
            BitmapFont font = assets.get("outline");
            font.draw((int)(assets.getProgress() * 100) + "%", graphics.getWidth() / 2f, graphics.getHeight() / 2f + Scl.scl(10f), Align.center);
            font.draw(bundle.get("loading", "").replace("[accent]", ""), graphics.getWidth() / 2f, graphics.getHeight() / 2f + height / 2f + Scl.scl(20), Align.center);

            if(assets.getCurrentLoading() != null){
                String name = assets.getCurrentLoading().fileName.toLowerCase();
                String key = name.contains("script") ? "scripts" : name.contains("content") ? "content" : name.contains("mod") ? "mods" : name.contains("msav") ||
                    name.contains("maps") ? "map" : name.contains("ogg") || name.contains("mp3") ? "sound" : name.contains("png") ? "image" : "system";
                font.draw(bundle.get("load." + key, ""), graphics.getWidth() / 2f, graphics.getHeight() / 2f - height / 2f - Scl.scl(10f), Align.center);
            }
        }
         */
        Lines.precise(false);
        Draw.flush();
    }

    static class Bar{
        final Floatp value;
        final Boolp red, valid;
        final String text;

        public Bar(String text, float value, boolean red){
            this.value = () -> value;
            this.red = () -> red;
            this.valid = () -> true;
            this.text = text;
        }

        public Bar(String text, Boolp valid, Floatp value, Boolp red){
            this.valid = valid;
            this.value = value;
            this.red = red;
            this.text = text;
        }

        boolean valid(){
            return valid.get();
        }

        boolean red(){
            return red.get();
        }

        float value(){
            return Mathf.clamp(value.get());
        }
    }
}
