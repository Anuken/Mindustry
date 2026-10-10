package mindustry.logic.statements;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

@RegisterStatement("status")
public class ApplyStatusStatement extends LogicStatement{
    public boolean clear;
    public String effect = "@status-wet", unit = "unit", duration = "10";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(bundle(clear ? "clear" : "apply"), Styles.logict, () -> {
            clear = !clear;
            build(table);
        }).size(80f, 40f).pad(4f).color(table.color);

        TextField field = field(table, effect, str -> {
            effect = str;
            build(table);
        }).width(240f).wrap().get();
        table.button(b -> {
            b.image(Icon.pencilSmall);
            b.clicked(() -> showSelectTable(b, (t, hide) -> {
                t.left();
                for(StatusEffect status : content.statusEffects()){
                    if(status == StatusEffects.none) continue;
                    t.button(status.localizedName, status.uiIcon != Core.atlas.find("error") ? new TextureRegionDrawable(status.uiIcon) : Icon.effect, Styles.flatt, iconSmall, () -> {
                        effect = "@status-" + status.name;
                        build(table);
                        field.setText(effect);
                        hide.run();
                    }).size(240f, 40f).marginLeft(5f).padLeft(-1f).row();
                }
            }));
        }, Styles.logict, () -> {
        }).size(40f).padLeft(-2f).color(table.color);

        fields(table, "target", unit, str -> unit = str).left();

        if(!clear && !isPermanent()){
            fields(table, "seconds", duration, str -> duration = str).left();
        }
    }

    private boolean isPermanent(){
        if(!effect.startsWith("@status-")) return false;
        StatusEffect status = content.statusEffect(effect.substring(8));
        if(status == null) return false;
        return status.permanent;
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new ApplyEffectI(clear, builder.var(effect), builder.var(unit), builder.var(duration));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
