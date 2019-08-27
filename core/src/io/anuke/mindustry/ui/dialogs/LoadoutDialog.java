package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.input.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.type.*;

import static io.anuke.mindustry.Vars.*;

public class LoadoutDialog extends FloatingDialog{
    private Runnable hider;
    private Supplier<Array<ItemStack>> supplier;
    private Runnable resetter;
    private Runnable updater;
    private Predicate<Item> filter;
    private Table items;
    private int capacity;

    public LoadoutDialog(){
        super("$configure");
        setFillParent(true);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                Core.app.post(this::hide);
            }
        });

        cont.add(items = new Table()).left();

        shown(this::setup);
        hidden(() -> {
            if(hider != null){
                hider.run();
            }
        });

        cont.row();

        cont.addButton("$add", () -> {
            FloatingDialog dialog = new FloatingDialog("");
            dialog.setFillParent(false);
            for(Item item : content.items().select(item -> filter.test(item) && item.type == ItemType.material && supplier.get().find(stack -> stack.item == item) == null)){
                TextButton button = dialog.cont.addButton("", "clear", () -> {
                    dialog.hide();
                    supplier.get().add(new ItemStack(item, 0));
                    updater.run();
                    setup();
                }).size(300f, 36f).get();
                button.clearChildren();
                button.left();
                button.addImage(item.icon(Item.Icon.medium)).size(8 * 3).pad(4);
                button.add(item.localizedName);
                dialog.cont.row();
            }
            dialog.show();
        }).size(100f, 40).left().disabled(b -> !content.items().contains(item -> filter.test(item) && !supplier.get().contains(stack -> stack.item == item)));

        cont.row();
        cont.addButton("$settings.reset", () -> {
            resetter.run();
            updater.run();
            setup();
        }).size(210f, 64f);

        cont.row();
        cont.addImageTextButton("$back", "icon-arrow-left", iconsize, this::hide).size(210f, 64f);
    }

    public void show(int capacity, Supplier<Array<ItemStack>> supplier, Runnable reseter, Runnable updater, Runnable hider, Predicate<Item> filter){
        this.resetter = reseter;
        this.supplier = supplier;
        this.updater = updater;
        this.capacity = capacity;
        this.hider = hider;
        this.filter = filter;
        show();
    }

    void setup(){
        items.clearChildren();
        items.left();
        float bsize = 40f;
        int step = 50;

        for(ItemStack stack : supplier.get()){
            items.addButton("x", "clear-partial", () -> {
                supplier.get().remove(stack);
                updater.run();
                setup();
            }).size(bsize);

            items.addButton("-", "clear-partial", () -> {
                stack.amount = Math.max(stack.amount - step, 0);
                updater.run();
            }).size(bsize);
            items.addButton("+", "clear-partial", () -> {
                stack.amount = Math.min(stack.amount + step, capacity);
                updater.run();
            }).size(bsize);

            items.addImage(stack.item.icon(Item.Icon.medium)).size(8 * 3).padRight(4).padLeft(4);
            items.label(() -> stack.amount + "").left();

            items.row();
        }
    }
}
