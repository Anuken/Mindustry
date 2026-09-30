package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.LogicDisplay.*;

import static mindustry.logic.LCanvas.*;

@RegisterStatement("draw")
public class DrawStatement extends LStatement{

    public GraphicsType type = GraphicsType.clear;
    public String x = "0", y = "0", p1 = "0", p2 = "0", p3 = "0", p4 = "0";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        table.left();

        table.button(b -> {
            b.label(() -> bundle(type));
            b.clicked(() -> showSelect(b, GraphicsType.all, type, t -> {
                type = t;
                if(type == GraphicsType.color){
                    p2 = "255";
                }

                if(type == GraphicsType.image){
                    p1 = "@copper";
                    p2 = "32";
                    p3 = "0";
                }

                if(type == GraphicsType.print){
                    p1 = "@bottomLeft";
                }

                rebuild(table);
            }, 2, cell -> cell.size(120, 50)));
        }, Styles.logict, () -> {
        }).size(120, 40).color(table.color).left().padLeft(2);

        if(isCompact()) table.add().width(200f);

        switch(type){
            case clear -> {
                fields(table, "r", x, v -> x = v);
                fields(table, "g", y, v -> y = v);
                fields(table, "b", p1, v -> p1 = v);
            }
            case color -> {
                fields(table, "r", x, v -> x = v);
                fields(table, "g", y, v -> y = v);
                fields(table, "b", p1, v -> p1 = v);
                fields(table, "a", p2, v -> p2 = v);
            }
            case col -> {
                fields(table, "color", x, v -> x = v).width(144f);
                col(table, x, res -> {
                    x = "%" + res.toString().substring(0, res.a >= 1f ? 6 : 8);
                    build(table);
                });
            }
            case stroke -> {
                table.add().width(4);
                fields(table, x, v -> x = v);
            }
            case line -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);
                fields(table, "x2", p1, v -> p1 = v);
                fields(table, "y2", p2, v -> p2 = v);
            }
            case rect, lineRect -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);
                fields(table, "width", p1, v -> p1 = v);
                fields(table, "height", p2, v -> p2 = v);
            }
            case poly, linePoly -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);
                fields(table, "sides", p1, v -> p1 = v);
                fields(table, "radius", p2, v -> p2 = v);
                fields(table, "rotation", p3, v -> p3 = v);
            }
            case triangle -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);
                fields(table, "x2", p1, v -> p1 = v);
                fields(table, "y2", p2, v -> p2 = v);
                fields(table, "x3", p3, v -> p3 = v);
                fields(table, "y3", p4, v -> p4 = v);
            }
            case image -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);
                fields(table, "image", p1, v -> p1 = v);
                fields(table, "size", p2, v -> p2 = v);
                fields(table, "rotation", p3, v -> p3 = v);
            }
            case print -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);

                fields(table, "align", p1, v -> p1 = v).width(170f);
                fieldAlignSelect(table, () -> p1, v -> {
                    p1 = v;
                    rebuild(table);
                }, true, true);
            }
            case translate, scale -> {
                fields(table, "x", x, v -> x = v);
                fields(table, "y", y, v -> y = v);
            }
            case rotate -> {
                fields(table, "angle", p1, v -> p1 = v);
            }
        }
    }

    @Override
    public void afterRead(){
        //0 constant alpha for colors is not allowed
        if(type == GraphicsType.color && p2.equals("0")){
            p2 = "255";
        }

        if(type == GraphicsType.print && nameToAlign.get(p1) != null){
            p1 = "@" + p1;
        }
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new DrawI((byte)type.ordinal(), builder.var(x), builder.var(y), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
    }

    @Override
    public LCategory category(){
        return LCategory.io;
    }
}
