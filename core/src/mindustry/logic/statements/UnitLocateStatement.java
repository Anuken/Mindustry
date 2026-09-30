package mindustry.logic.statements;

import arc.scene.style.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

@RegisterStatement("ulocate")
public class UnitLocateStatement extends LStatement{
    public LLocate locate = LLocate.building;
    public BlockFlag flag = BlockFlag.core;
    public String enemy = "true", ore = "@copper";
    public String outX = "outx", outY = "outy", outFound = "found", outBuild = "building";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.add(bundle("find")).left().self(this::param);

        table.button(b -> {
            b.label(() -> bundle(locate));
            b.clicked(() -> showSelect(b, LLocate.all, locate, t -> {
                locate = t;
                build(table);
            }, 2, cell -> cell.size(110, 50)));
        }, Styles.logict, () -> {
        }).size(110, 40).color(table.color).left().padLeft(2);

        switch(locate){
            case building -> {
                table.add(bundle("group")).left().self(this::param);
                table.button(b -> {
                    b.label(() -> bundle(flag));
                    b.clicked(() -> showSelect(b, BlockFlag.allLogic, flag, t -> flag = t, 2, cell -> cell.size(120, 50)));
                }, Styles.logict, () -> {
                }).size(120, 40).color(table.color).left().padLeft(2);

                fields(table, "enemy", enemy, str -> enemy = str);
            }

            case ore -> {
                table.table(ts -> {
                    ts.color.set(table.color);

                    fields(ts, "ore", ore, str -> ore = str);

                    ts.button(b -> {
                        b.image(Icon.pencilSmall);
                        b.clicked(() -> showSelectTable(b, (t, hide) -> {
                            t.row();
                            t.table(i -> {
                                i.left();
                                int c = 0;
                                for(Item item : Vars.content.items()){
                                    if(!item.unlockedNow()) continue;
                                    i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
                                        ore = "@" + item.name;
                                        build(table);
                                        hide.run();
                                    }).size(40f);

                                    if(++c % 6 == 0) i.row();
                                }
                            }).colspan(3).width(240f).left();
                        }));
                    }, Styles.logict, () -> {
                    }).size(40f).padLeft(-2).color(table.color);
                });


            }

            case spawn, damaged -> {
            }
        }

        fields(table, "outX", outX, str -> outX = str);

        fields(table, "outY", outY, str -> outY = str);

        fields(table, "found", outFound, str -> outFound = str);

        if(locate != LLocate.ore){
            fields(table, "building", outBuild, str -> outBuild = str);
        }

    }

    @Override
    public LInstruction build(LAssembler builder){
        return new UnitLocateI(locate, flag, builder.var(enemy), builder.var(ore), builder.var(outX), builder.var(outY), builder.var(outFound), builder.var(outBuild));
    }

    @Override
    public LCategory category(){
        return LCategory.unit;
    }
}
