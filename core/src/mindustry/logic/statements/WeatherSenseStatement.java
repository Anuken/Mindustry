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

@RegisterStatement("weathersense")
public class WeatherSenseStatement extends LogicStatement{
    public String to = "result";
    public String weather = "@rain";

    private transient TextField tfield;

    @Override
    public void build(Table table){
        field(table, to, str -> to = str);

        table.add(" = ");
        table.add(bundle("weather"));

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
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SenseWeatherI(builder.var(weather), builder.var(to));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
