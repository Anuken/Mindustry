package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.mindustry.type.*;

import java.util.function.Supplier;

import static io.anuke.mindustry.Vars.content;

public class LoadoutDialog extends FloatingDialog{
    private Runnable hider;
    private Supplier<Array<ItemStack>> supplier;
    private Runnable resetter;
    private Runnable updater;
    private Predicate<Item> filter;
    private int capacity;

    public LoadoutDialog(){
        super("$configure");
        setFillParent(false);
        addCloseButton();
        shown(this::setup);
        hidden(() -> {
            if(hider != null){
                hider.run();
            }
        });
        buttons.row();
        buttons.addButton("$settings.reset", () -> {
            resetter.run();
            updater.run();
            setup();
        }).size(210f, 64f);
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
        cont.clear();
        float bsize = 40f;
        int step = 50;

        for(ItemStack stack : supplier.get()){
            cont.addButton("x", "clear-partial", () -> {
                supplier.get().remove(stack);
                updater.run();
                setup();
            }).size(bsize);

            cont.addButton("-", "clear-partial", () -> {
                stack.amount = Math.max(stack.amount - step, 0);
                updater.run();
            }).size(bsize);
            cont.addButton("+", "clear-partial", () -> {
                stack.amount = Math.min(stack.amount + step, capacity);
                updater.run();
            }).size(bsize);

            cont.addImage(stack.item.icon(Item.Icon.medium)).size(8 * 3).padRight(4);
            cont.label(() -> stack.amount + "").left();

            cont.row();
        }

        cont.addButton("$add", () -> {
            FloatingDialog dialog = new FloatingDialog("");
            dialog.setFillParent(false);
            for(Item item : content.items().select(item -> filter.test(item) && item.type == ItemType.material && supplier.get().find(stack -> stack.item == item) == null)){
                TextButton button = dialog.cont.addButton("", "clear", () -> {
                    supplier.get().add(new ItemStack(item, 0));
                    updater.run();
                    setup();
                    dialog.hide();
                }).size(300f, 36f).get();
                button.clearChildren();
                button.left();
                button.addImage(item.icon(Item.Icon.medium)).size(8 * 3).pad(4);
                button.add(item.localizedName);
                dialog.cont.row();
            }
            dialog.show();
        }).colspan(4).size(100f, bsize).left().disabled(b -> !content.items().contains(item -> filter.test(item) && !supplier.get().contains(stack -> stack.item == item)));
        pack();
    }
}
