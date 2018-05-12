package io.anuke.mindustry.ui.fragments;

import io.anuke.ucore.scene.Group;
import io.anuke.ucore.util.Strings;

public class PlayerMenuFragment implements Fragment {

    @Override
    public void build(Group parent) {
        /*
        new table(){{
            new table("clear"){{
                ItemImage item = new ItemImage(player.inventory.item.item.region, () -> round(player.inventory.item.amount), Color.WHITE)
                        .updateRegion(() -> player.inventory.item.item.region);

                ItemImage liquid = new ItemImage(Draw.region("icon-liquid"), () -> round(player.inventory.liquid.amount), Color.WHITE)
                        .updateColor(() -> player.inventory.liquid.liquid == Liquids.none ? Color.GRAY : player.inventory.liquid.liquid.color);
                ItemImage power = new ItemImage(Draw.region("icon-power"), () -> round(player.inventory.power), Colors.get("power"));

                defaults().size(16 * 2).space(6f);
                add(item);
                add(liquid);
                add(power);

                visible(() -> Inputs.keyDown("player_list"));
                padTop(100);
                margin(5);
            }}.end();
        }}.end();*/
    }

    public String round(float f){
        f = (int)f;
        if(f >= 1000){
            return Strings.toFixed(f/1000, 1) + "k";
        }else{
            return (int)f+"";
        }
    }
}
