package mindustry.world.blocks;

import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class LiquidSelection{

    public static void buildLiquidTable(Table table, Prov<Liquid> holder, Cons<Liquid> consumer){

        Array<Liquid> liquids = content.liquids();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        Table cont = new Table();
        cont.defaults().size(38);

        int i = 0;

        for(Liquid liquid : liquids){
            if(!data.isUnlocked(liquid) && world.isZone()) continue;

            ImageButton button = cont.addImageButton(Tex.whiteui, Styles.clearToggleTransi, 24, () -> control.input.frag.config.hideConfig()).group(group).get();
            button.changed(() -> consumer.get(button.isChecked() ? liquid : null));
            button.getStyle().imageUp = new TextureRegionDrawable(liquid.icon(Cicon.small));
            button.update(() -> button.setChecked(holder.get() == liquid));

            if(i++ % 4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }
}
