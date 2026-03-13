package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.CanvasBlock.*;

import static mindustry.Vars.*;

public class CanvasEditDialog extends BaseDialog{
    static final float refreshTime = 60f * 2f;

    int curColor;
    boolean fill, modified;
    float time;
    CanvasBuild canvas;
    Pixmap pix;

    public CanvasEditDialog(CanvasBuild canvas){
        super("");
        titleTable.remove();
        this.canvas = canvas;
        CanvasBlock block = (CanvasBlock)canvas.block;
        int size = block.canvasSize;
        pix = block.makePixmap(canvas.data, new Pixmap(size, size));
        Texture texture = new Texture(pix);
        curColor = block.palette[0];

        addCloseButton();

        hidden(() -> {
            save();

            texture.dispose();
            pix.dispose();
        });

        resized(this::hide);

        //update at an interval so that people can see what is being drawn
        update(() -> {
            time += Time.delta;

            if(time >= refreshTime){
                save();
                time = 0f;
            }
        });

        cont.table(Tex.pane, body -> {
            body.add(new Element(){
                int lastX, lastY;
                IntSeq stack = new IntSeq();

                int convertX(float ex){
                    return (int)((ex) / (width / size));
                }

                int convertY(float ey){
                    return pix.height - 1 - (int)((ey) / (height / size));
                }

                {
                    addListener(new InputListener(){

                        @Override
                        public boolean touchDown(InputEvent event, float ex, float ey, int pointer, KeyCode button){
                            int cx = convertX(ex), cy = convertY(ey);

                            if(button == KeyCode.mouseLeft){
                                if(fill){
                                    stack.clear();
                                    int src = curColor;
                                    int dst = pix.get(cx, cy);
                                    if(src != dst){
                                        stack.add(Point2.pack(cx, cy));
                                        while(!stack.isEmpty()){
                                            int current = stack.pop();
                                            int x = Point2.x(current), y = Point2.y(current);
                                            draw(x, y);
                                            for(int i = 0; i < 4; i++){
                                                int nx = x + Geometry.d4x(i), ny = y + Geometry.d4y(i);
                                                if(nx >= 0 && ny >= 0 && nx < pix.width && ny < pix.height && pix.getRaw(nx, ny) == dst){
                                                    stack.add(Point2.pack(nx, ny));
                                                }
                                            }
                                        }
                                    }

                                    return false;
                                }else{
                                    draw(cx, cy);
                                    lastX = cx;
                                    lastY = cy;
                                }
                            }else if(button == KeyCode.mouseMiddle){
                                curColor = pix.get(cx, cy);
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public void touchDragged(InputEvent event, float ex, float ey, int pointer){
                            if(fill) return;
                            int cx = convertX(ex), cy = convertY(ey);
                            Bresenham2.line(lastX, lastY, cx, cy, (x, y) -> draw(x, y));
                            lastX = cx;
                            lastY = cy;
                        }
                    });
                }

                void draw(int x, int y){
                    if(pix.get(x, y) != curColor){
                        pix.set(x, y, curColor);
                        Pixmaps.drawPixel(texture, x, y, curColor);
                        modified = true;
                    }
                }

                @Override
                public void draw(){
                    Tmp.tr1.set(texture);
                    Draw.alpha(parentAlpha);
                    Draw.rect(Tmp.tr1, x + width/2f, y + height/2f, width, height);

                    //draw grid
                    {
                        float xspace = (getWidth() / size);
                        float yspace = (getHeight() / size);
                        float s = 1f;

                        int minspace = 10;

                        int jumpx = (int)(Math.max(minspace, xspace) / xspace);
                        int jumpy = (int)(Math.max(minspace, yspace) / yspace);

                        for(int x = 0; x <= size; x += jumpx){
                            Fill.crect((int)(this.x + xspace * x - s), y - s, 2, getHeight() + (x == size ? 1 : 0));
                        }

                        for(int y = 0; y <= size; y += jumpy){
                            Fill.crect(x - s, (int)(this.y + y * yspace - s), getWidth(), 2);
                        }
                    }

                    if(!mobile){
                        Vec2 s = screenToLocalCoordinates(Core.input.mouse());
                        if(s.x >= 0 && s.y >= 0 && s.x < width && s.y < height){
                            float sx = Mathf.round(s.x, width / size), sy = Mathf.round(s.y, height / size);

                            Lines.stroke(Scl.scl(6f));
                            Draw.color(Pal.accent);
                            Lines.rect(sx + x, sy + y, width / size, height / size, Lines.getStroke() - 1f);

                            Draw.reset();
                        }
                    }
                }
            }).size(mobile && !Core.graphics.isPortrait() ? Math.min(290f, Core.graphics.getHeight() / Scl.scl(1f) - 75f / Scl.scl(1f)) : 480f);
        }).colspan(3);

        cont.row();

        cont.add().size(60f);

        cont.table(Tex.button, p -> {
            for(int i = 0; i < block.palette.length; i++){
                int fi = i;

                var button = p.button(Tex.whiteui, Styles.squareTogglei, 30, () -> {
                    curColor = block.palette[fi];
                }).size(44).checked(b -> curColor == block.palette[fi]).get();
                button.getStyle().imageUpColor = new Color(block.palette[i]);
            }
        });

        cont.table(Tex.button, t -> {
            t.button(Icon.fill, Styles.clearNoneTogglei, () -> fill = !fill).size(44f);
        });

        closeOnBack();

        buttons.defaults().size(150f, 64f);
    }

    void save(){
        if(modified){
            canvas.configure(canvas.packPixmap(pix));
            modified = false;
        }
    }
}
