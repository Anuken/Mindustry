package mindustry.logic.statements;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("printchar")
public class PrintCharStatement extends LStatement{
    public String value = "65";

    @Override
    public void build(Table table){
        table.add(bundle("char")).padLeft(3f);
        TextField field = field(table, value, str -> value = str).get();
        table.button(b -> {
            b.image(Icon.pencilSmall);
            b.clicked(() -> showSelectTable(b, (t, hide) -> {
                t.row();
                t.table(i -> {
                    i.left();
                    int c = 0;
                    for(char j = 32; j < 127; j++){
                        final int chr = j;
                        i.button(String.valueOf(j), Styles.flatt, () -> {
                            value = Integer.toString(chr);
                            field.setText(value);
                            hide.run();
                        }).size(32f);
                        if(++c % 8 == 0) i.row();
                    }
                });
            }));
        }, Styles.logict, () -> {
        }).size(40f).padLeft(-2f).color(table.color);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new PrintCharI(builder.var(value));
    }

    @Override
    public LCategory category(){
        return LCategory.io;
    }
}
