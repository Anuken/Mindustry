package mindustry.maps.filters;


import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public abstract class FilterOption{
    public static final Boolf<Block> floorsOnly = b -> (b instanceof Floor && !(b instanceof OverlayFloor)) && !headless && Core.atlas.isFound(b.icon(mindustry.ui.Cicon.full));
    public static final Boolf<Block> wallsOnly = b -> (!b.synthetic() && !(b instanceof Floor)) && !headless && Core.atlas.isFound(b.icon(mindustry.ui.Cicon.full));
    public static final Boolf<Block> floorsOptional = b -> b == Blocks.air || ((b instanceof Floor && !(b instanceof OverlayFloor)) && !headless && Core.atlas.isFound(b.icon(mindustry.ui.Cicon.full)));
    public static final Boolf<Block> wallsOptional = b -> b == Blocks.air || ((!b.synthetic() && !(b instanceof Floor)) && !headless && Core.atlas.isFound(b.icon(mindustry.ui.Cicon.full)));
    public static final Boolf<Block> wallsOresOptional = b -> b == Blocks.air || (((!b.synthetic() && !(b instanceof Floor)) || (b instanceof OverlayFloor)) && !headless && Core.atlas.isFound(b.icon(mindustry.ui.Cicon.full)));
    public static final Boolf<Block> oresOnly = b -> b instanceof OverlayFloor && !headless && Core.atlas.isFound(b.icon(mindustry.ui.Cicon.full));
    public static final Boolf<Block> anyOptional = b -> floorsOnly.get(b) || wallsOnly.get(b) || oresOnly.get(b) || b == Blocks.air;

    public abstract void build(Table table);

    public Runnable changed = () -> {};

    static class SliderOption extends FilterOption{
        final String name;
        final Floatp getter;
        final Floatc setter;
        final float min, max, step;

        boolean display;

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

        public SliderOption display(){
            display = true;
            return this;
        }

        @Override
        public void build(Table table){
            if(!display){
                table.add("$filter.option." + name);
            }else{
                table.label(() -> Core.bundle.get("filter.option." + name) + ": " + (int)getter.get());
            }
            table.row();
            Slider slider = table.slider(min, max, step, setter).growX().get();
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
            table.button(b -> b.image(supplier.get().icon(Cicon.small)).update(i -> ((TextureRegionDrawable)i.getDrawable())
                .setRegion(supplier.get() == Blocks.air ? Icon.block.getRegion() : supplier.get().icon(Cicon.small))).size(8 * 3), () -> {
                BaseDialog dialog = new BaseDialog("");
                dialog.setFillParent(false);
                int i = 0;
                for(Block block : Vars.content.blocks()){
                    if(!filter.get(block)) continue;

                    dialog.cont.image(block == Blocks.air ? Icon.block.getRegion() : block.icon(Cicon.medium)).size(8 * 4).pad(3).get().clicked(() -> {
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
