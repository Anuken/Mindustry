package mindustry.ui.dialogs;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.gen.*;

public class ColorPicker extends BaseDialog{
    private static Texture hueTex;

    private Cons<Color> cons = c -> {};
    Color current = new Color();
    float h, s, v, a;
    TextField hexField;
    Slider hSlider, sSlider, vSlider, aSlider;

    public ColorPicker(){
        super("@pickcolor");
    }

    public void show(Color color, Cons<Color> consumer){
        show(color, true, consumer);
    }

    public void show(Color color, boolean alpha, Cons<Color> consumer){
        this.current.set(color);
        this.cons = consumer;
        show();

        if(hueTex == null){
            hueTex = Pixmaps.hueTexture(128, 1);
            hueTex.setFilter(TextureFilter.linear);
        }

        float[] values = color.toHsv(new float[3]);
        h = values[0];
        s = values[1];
        v = values[2];
        a = color.a;

        cont.clear();
        cont.pane(t -> {
            t.table(Tex.pane, i -> {
                i.stack(new Image(Tex.alphaBg), new Image(){{
                    setColor(current);
                    update(() -> setColor(current));
                }}).size(200f);
            }).colspan(2).padBottom(5);

            t.row();

            t.defaults().padBottom(6).width(370f).height(44f);

            t.stack(new Image(new TextureRegion(hueTex)), hSlider = new Slider(0f, 360f, 0.3f, false){{
                setValue(h);
                moved(value -> {
                    h = value;
                    updateColor();
                });
            }}).row();

            t.stack(new Element(){
                @Override
                public void draw(){
                    float first = Tmp.c1.set(current).saturation(0f).a(parentAlpha).toFloatBits();
                    float second = Tmp.c1.set(current).saturation(1f).a(parentAlpha).toFloatBits();

                    Fill.quad(
                        x, y, first,
                        x + width, y, second,
                        x + width, y + height, second,
                        x, y + height, first
                    );
                }
            }, sSlider = new Slider(0f, 1f, 0.001f, false){{
                setValue(s);
                moved(value -> {
                    s = value;
                    updateColor();
                });
            }}).row();

            t.stack(new Element(){
                @Override
                public void draw(){
                    float first = Tmp.c1.set(current).value(0f).a(parentAlpha).toFloatBits();
                    float second = Tmp.c1.fromHsv(h, s, 1f).a(parentAlpha).toFloatBits();

                    Fill.quad(
                    x, y, first,
                    x + width, y, second,
                    x + width, y + height, second,
                    x, y + height, first
                    );
                }
            }, vSlider = new Slider(0f, 1f, 0.001f, false){{
                setValue(v);

                moved(value -> {
                    v = value;
                    updateColor();
                });
            }}).row();

            if(alpha){
                t.stack(new Image(Tex.alphaBgLine), new Element(){
                    @Override
                    public void draw(){
                        float first = Tmp.c1.set(current).a(0f).toFloatBits();
                        float second = Tmp.c1.set(current).a(parentAlpha).toFloatBits();

                        Fill.quad(
                        x, y, first,
                        x + width, y, second,
                        x + width, y + height, second,
                        x, y + height, first
                        );
                    }
                }, aSlider = new Slider(0f, 1f, 0.001f, false){{
                    setValue(a);

                    moved(value -> {
                        a = value;
                        updateColor();
                    });
                }}).row();
            }

            hexField = t.field(current.toString(), value -> {
                try{
                    current.set(Color.valueOf(value).a(a));
                    current.toHsv(values);
                    h = values[0];
                    s = values[1];
                    v = values[2];
                    a = current.a;

                    hSlider.setValue(h);
                    sSlider.setValue(s);
                    vSlider.setValue(v);
                    if(aSlider != null){
                        aSlider.setValue(a);
                    }

                    updateColor(false);
                }catch(Exception ignored){
                }
            }).size(130f, 40f).valid(text -> {
                //garbage performance but who cares this runs only every key type anyway
                try{
                    Color.valueOf(text);
                    return true;
                }catch(Exception e){
                    return false;
                }
            }).get();
        });

        buttons.clear();
        addCloseButton();
        buttons.button("@ok", Icon.ok, () -> {
            cons.get(current);
            hide();
        });
    }

    void updateColor(){
        updateColor(true);
    }

    void updateColor(boolean updateField){
        current.fromHsv(h, s, v);
        current.a = a;

        if(hexField != null && updateField){
            String val = current.toString();
            if(current.a >= 0.9999f){
                val = val.substring(0, 6);
            }
            hexField.setText(val);
        }
    }
}
