package mindustry.logic.statements;

import arc.func.*;
import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.logic.LogicCanvas.*;

@RegisterStatement("radar")
public class RadarStatement extends LogicStatement{
    public RadarTarget target1 = RadarTarget.enemy, target2 = RadarTarget.any, target3 = RadarTarget.any;
    public RadarSort sort = RadarSort.distance;
    public String radar = "turret1", sortOrder = "1", output = "result";

    @Override
    public void build(Table table){
        table.defaults().left();

        fields(table, output, v -> output = v);

        table.add(" = ");

        if(buildFrom()){
            fields(table, "from", radar, v -> radar = v);
        }

        Table inner = new Table();
        inner.setColor(table.color);
        table.add(inner).padLeft(2f);

        for(int i = 0; i < 3; i++){
            int fi = i;
            Prov<RadarTarget> get = () -> (fi == 0 ? target1 : fi == 1 ? target2 : target3);

            inner.button(b -> {
                b.label(() -> bundle(get.get()));
                b.clicked(() -> showSelect(b, RadarTarget.all, get.get(), t -> {
                    if(fi == 0) target1 = t;
                    else if(fi == 1) target2 = t;
                    else target3 = t;
                }, 2, cell -> cell.size(110, 50)));
            }, Styles.logict, () -> {
            }).size(110, 40).color(table.color).left();
        }

        fields(table, "order", sortOrder, v -> sortOrder = v);

        table.table(t -> {
            t.setColor(table.color);
            if(!isCompact()) t.add(bundle("sort")).padRight(4f).self(this::param);

            t.button(b -> {
                b.label(() -> bundle(sort));
                b.clicked(() -> showSelect(b, RadarSort.all, sort, sr -> sort = sr, 2, cell -> cell.size(100, 50)));
            }, Styles.logict, () -> {
            }).size(180, 40).color(table.color);

            if(isCompact()) t.add(bundle("sort")).padLeft(4f).self(this::param);
        });
    }

    public boolean buildFrom(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new RadarI(target1, target2, target3, sort, builder.var(radar), builder.var(sortOrder), builder.var(output));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.block;
    }
}
