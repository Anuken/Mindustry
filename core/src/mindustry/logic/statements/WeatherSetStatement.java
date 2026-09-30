package mindustry.logic.statements;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;

@RegisterStatement("weatherset")
public class WeatherSetStatement extends LogicStatement{
    public String weather = "@rain", state = "true";

    private transient TextField tfield;

    @Override
    public void build(Table table){
        tfield = field(table, weather, str -> weather = str).padRight(0f).get();

        table.button(b -> {
            b.image(Icon.pencilSmall);

            b.clicked(() -> showSelectTable(b, (t, hide) -> {
                t.row();
                t.table(i -> {
                    i.left();
                    int c = 0;
                    for(Weather w : Vars.content.weathers()){
                        i.button(bundle(w.name), Styles.flatt, () -> {
                            weather = "@" + w.name;
                            tfield.setText(weather);
                            hide.run();
                        }).height(40f).uniformX().wrapLabel(false).growX();

                        if(++c % 2 == 0) i.row();
                    }
                }).left();
            }));
        }, Styles.logict, () -> {
        }).size(40f).padLeft(-1).color(table.color);

        fields(table, "state", state, str -> state = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SetWeatherI(builder.var(weather), builder.var(state));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
