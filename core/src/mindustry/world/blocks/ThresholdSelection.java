package mindustry.world.blocks;

import arc.struct.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.Cicon;

import static mindustry.Vars.*;

public class ThresholdSelection{

    public static void buildThresholdTable(Table table, Prov<Threshold> holder, Cons<Threshold> consumer){

        Array<Threshold> thresholds = content.thresholds();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        Table cont = new Table();
        cont.defaults().size(38);

        int i = 0;

        for(Threshold item : thresholds){
            if(!data.isUnlocked(item) && world.isZone()) continue;

            ImageButton button = cont.addImageButton(Tex.whiteui, Styles.clearToggleTransi, 24, () -> control.input.frag.config.hideConfig()).group(group).get();
            button.changed(() -> consumer.get(button.isChecked() ? item : null));
            button.getStyle().imageUp = new TextureRegionDrawable(item.icon(Cicon.small));
            button.update(() -> button.setChecked(holder.get() == item));

            if(i++ % 4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }
}
