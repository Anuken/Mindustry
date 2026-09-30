package mindustry.logic.statements;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("setmarker")
public class SetMarkerStatement extends LStatement{
    public LMarkerControl type = LMarkerControl.pos;
    public String id = "0", p1 = "0", p2 = "0", p3 = "0";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        table.add(bundle("set")).padLeft(6f);

        table.button(b -> {
            b.label(() -> bundle(type));
            b.clicked(() -> showSelect(b, LMarkerControl.all, type, t -> {
                type = t;
                rebuild(table);
            }, 3, cell -> cell.size(140, 50)));
        }, Styles.logict, () -> {
        }).size(190, 40).color(table.color).left().padLeft(2);

        String desc = bundle("id");
        fields(table, desc, id, str -> id = str);

        //Q: why don't you just use arrays for this?
        //A: arrays aren't as easy to serialize so the code generator doesn't handle them
        for(int f = 0; f < type.params.length; f++){

            String value = f == 0 ? p1 : f == 1 ? p2 : p3;
            Cons<String> setter = f == 0 ? v -> p1 = v : f == 1 ? v -> p2 = v : v -> p3 = v;

            fields(table, type.params[f], value, setter);

            if(type == LMarkerControl.color || (type == LMarkerControl.colori && f == 1)){
                col(table, value, res -> {
                    setter.get("%" + res.toString().substring(0, res.a >= 1f ? 6 : 8));
                    build(table);
                });
            }else if(type == LMarkerControl.drawLayer){
                table.button(b -> {
                    b.image(Icon.pencilSmall);
                    b.clicked(() -> showSelectTable(b, (o, hide) -> {
                        o.row();
                        o.table(s -> {
                            s.left();
                            for(var field : Layer.class.getFields()){
                                float layer = Reflect.get(field);
                                s.button(field.getName() + " = " + layer, Styles.logicTogglet, () -> {
                                    p1 = Float.toString(layer);
                                    rebuild(table);
                                    hide.run();
                                }).size(240f, 40f).row();
                            }
                        }).width(240f).left();
                    }));
                }, Styles.logict, () -> {
                }).size(40f).padLeft(-11).color(table.color);
            }else if(type == LMarkerControl.textAlign || type == LMarkerControl.lineAlign){
                fieldAlignSelect(table, () -> p1, v -> {
                    p1 = v;
                    rebuild(table);
                }, true, type != LMarkerControl.lineAlign);
            }
        }
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SetMarkerI(type, builder.var(id), builder.var(p1), builder.var(p2), builder.var(p3));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
