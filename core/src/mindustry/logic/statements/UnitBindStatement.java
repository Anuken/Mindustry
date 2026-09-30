package mindustry.logic.statements;

import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

@RegisterStatement("ubind")
public class UnitBindStatement extends LStatement{
    public String type = "@poly";

    @Override
    public void build(Table table){
        table.add(bundle("type")).padLeft(3f);

        TextField field = field(table, type, str -> type = str).get();

        table.button(b -> {
            b.image(Icon.pencilSmall);
            b.clicked(() -> showSelectTable(b, (t, hide) -> {
                t.row();
                t.table(i -> {
                    i.left();
                    int c = 0;
                    for(UnitType item : Vars.content.units()){
                        if(!item.unlockedNow() || item.isHidden() || !item.logicControllable) continue;
                        i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
                            type = "@" + item.name;
                            field.setText(type);
                            hide.run();
                        }).size(40f);

                        if(++c % 6 == 0) i.row();
                    }
                }).colspan(3).width(240f).left();
            }));
        }, Styles.logict, () -> {
        }).size(40f).padLeft(-2).color(table.color);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new UnitBindI(builder.var(type));
    }

    @Override
    public LCategory category(){
        return LCategory.unit;
    }
}
