package io.anuke.mindustry.maps.filters;


import io.anuke.arc.*;
import io.anuke.arc.func.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import static io.anuke.mindustry.Vars.updateEditorOnChange;

public abstract class FilterOption{
    public static final Boolf<Block> floorsOnly = b -> (b instanceof Floor && !(b instanceof OverlayFloor)) && Core.atlas.isFound(b.icon(io.anuke.mindustry.ui.Cicon.full));
    public static final Boolf<Block> wallsOnly = b -> (!b.synthetic() && !(b instanceof Floor)) && Core.atlas.isFound(b.icon(io.anuke.mindustry.ui.Cicon.full));
    public static final Boolf<Block> floorsOptional = b -> b == Blocks.air || ((b instanceof Floor && !(b instanceof OverlayFloor)) && Core.atlas.isFound(b.icon(io.anuke.mindustry.ui.Cicon.full)));
    public static final Boolf<Block> wallsOptional = b -> b == Blocks.air || ((!b.synthetic() && !(b instanceof Floor)) && Core.atlas.isFound(b.icon(io.anuke.mindustry.ui.Cicon.full)));
    public static final Boolf<Block> wallsOresOptional = b -> b == Blocks.air || (((!b.synthetic() && !(b instanceof Floor)) || (b instanceof OverlayFloor)) && Core.atlas.isFound(b.icon(io.anuke.mindustry.ui.Cicon.full)));
    public static final Boolf<Block> oresOnly = b -> b instanceof OverlayFloor && Core.atlas.isFound(b.icon(io.anuke.mindustry.ui.Cicon.full));
    public static final Boolf<Block> anyOptional = b -> floorsOnly.get(b) || wallsOnly.get(b) || oresOnly.get(b) || b == Blocks.air;

    public abstract void build(Table table);

    public Runnable changed = () -> {};

    static class SliderOption extends FilterOption{
        final String name;
        final Floatp getter;
        final Floatc setter;
        final float min, max, step;

        SliderOption(String name, Floatp getter, Floatc setter, float min, float max){
            this(name, getter, setter, min, max, (max - min) / 200);
        }

        SliderOption(String name, Floatp getter, Floatc setter, float min, float max, float step){
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        @Override
        public void build(Table table){
            table.add("$filter.option." + name);
            table.row();
            Slider slider = table.addSlider(min, max, step, setter).growX().get();
            slider.setValue(getter.get());
            if(updateEditorOnChange){
                slider.changed(changed);
            }else{
                slider.released(changed);
            }
        }
    }

    static class BlockOption extends FilterOption{
        final String name;
        final Prov<Block> supplier;
        final Cons<Block> consumer;
        final Boolf<Block> filter;

        BlockOption(String name, Prov<Block> supplier, Cons<Block> consumer, Boolf<Block> filter){
            this.name = name;
            this.supplier = supplier;
            this.consumer = consumer;
            this.filter = filter;
        }

        @Override
        public void build(Table table){
            table.addButton(b -> b.addImage(supplier.get().icon(io.anuke.mindustry.ui.Cicon.small)).update(i -> ((TextureRegionDrawable)i.getDrawable())
                .setRegion(supplier.get() == Blocks.air ? Core.atlas.find("icon-none") : supplier.get().icon(io.anuke.mindustry.ui.Cicon.small))).size(8 * 3), () -> {
                FloatingDialog dialog = new FloatingDialog("");
                dialog.setFillParent(false);
                int i = 0;
                for(Block block : Vars.content.blocks()){
                    if(!filter.get(block)) continue;

                    dialog.cont.addImage(block == Blocks.air ? Core.atlas.find("icon-none-small") : block.icon(Cicon.medium)).size(8 * 4).pad(3).get().clicked(() -> {
                        consumer.get(block);
                        dialog.hide();
                        changed.run();
                    });
                    if(++i % 10 == 0) dialog.cont.row();
                }

                dialog.show();
            }).pad(4).margin(12f);

            table.add("$filter.option." + name);
        }
    }
}
