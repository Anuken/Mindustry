package io.anuke.mindustry.maps.filters;


import io.anuke.arc.*;
import io.anuke.arc.function.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import static io.anuke.mindustry.Vars.updateEditorOnChange;

public abstract class FilterOption{
    public static final Predicate<Block> floorsOnly = b -> (b instanceof Floor && !(b instanceof OverlayFloor)) && Core.atlas.isFound(b.icon(Cicon.full));
    public static final Predicate<Block> wallsOnly = b -> (!b.synthetic() && !(b instanceof Floor)) && Core.atlas.isFound(b.icon(Cicon.full));
    public static final Predicate<Block> floorsOptional = b -> b == Blocks.air || ((b instanceof Floor && !(b instanceof OverlayFloor)) && Core.atlas.isFound(b.icon(Cicon.full)));
    public static final Predicate<Block> wallsOptional = b -> b == Blocks.air || ((!b.synthetic() && !(b instanceof Floor)) && Core.atlas.isFound(b.icon(Cicon.full)));
    public static final Predicate<Block> wallsOresOptional = b -> b == Blocks.air || (((!b.synthetic() && !(b instanceof Floor)) || (b instanceof OverlayFloor)) && Core.atlas.isFound(b.icon(Cicon.full)));
    public static final Predicate<Block> oresOnly = b -> b instanceof OverlayFloor && Core.atlas.isFound(b.icon(Cicon.full));
    public static final Predicate<Block> anyOptional = b -> floorsOnly.test(b) || wallsOnly.test(b) || oresOnly.test(b) || b == Blocks.air;

    public abstract void build(Table table);

    public Runnable changed = () -> {};

    static class SliderOption extends FilterOption{
        final String name;
        final FloatProvider getter;
        final FloatConsumer setter;
        final float min, max, step;

        SliderOption(String name, FloatProvider getter, FloatConsumer setter, float min, float max){
            this(name, getter, setter, min, max, (max - min) / 200);
        }

        SliderOption(String name, FloatProvider getter, FloatConsumer setter, float min, float max, float step){
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
        final Supplier<Block> supplier;
        final Consumer<Block> consumer;
        final Predicate<Block> filter;

        BlockOption(String name, Supplier<Block> supplier, Consumer<Block> consumer, Predicate<Block> filter){
            this.name = name;
            this.supplier = supplier;
            this.consumer = consumer;
            this.filter = filter;
        }

        @Override
        public void build(Table table){
            table.addButton(b -> b.addImage(supplier.get().icon(Cicon.small)).update(i -> ((TextureRegionDrawable)i.getDrawable())
                .setRegion(supplier.get() == Blocks.air ? Core.atlas.find("icon-none") : supplier.get().icon(Cicon.small))).size(8 * 3), () -> {
                FloatingDialog dialog = new FloatingDialog("");
                dialog.setFillParent(false);
                int i = 0;
                for(Block block : Vars.content.blocks()){
                    if(!filter.test(block)) continue;

                    dialog.cont.addImage(block == Blocks.air ? Core.atlas.find("icon-none-small") : block.icon(Cicon.medium)).size(8 * 4).pad(3).get().clicked(() -> {
                        consumer.accept(block);
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
