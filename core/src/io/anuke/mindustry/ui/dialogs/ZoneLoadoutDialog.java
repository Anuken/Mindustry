package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.scene.ui.TextButton;
import io.anuke.mindustry.type.*;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.data;

public class ZoneLoadoutDialog extends FloatingDialog{
    private Zone zone;
    private Runnable hider;

    public ZoneLoadoutDialog(){
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
            zone.resetStartingItems();
            zone.updateLaunchCost();
            setup();
        }).size(210f, 64f);
    }

    public void show(Zone zone, Runnable hider){
        this.hider = hider;
        this.zone = zone;
        show();
    }

    void setup(){
        cont.clear();
        float bsize = 40f;
        int step = 50;

        for(ItemStack stack : zone.getStartingItems()){
            cont.addButton("x", "clear-partial", () -> {
                zone.getStartingItems().remove(stack);
                zone.updateLaunchCost();
                setup();
            }).size(bsize);

            cont.addButton("-", "clear-partial", () -> {
                stack.amount = Math.max(stack.amount - step, 0);
                zone.updateLaunchCost();
            }).size(bsize);
            cont.addButton("+", "clear-partial", () -> {
                stack.amount = Math.min(stack.amount + step, zone.loadout.core().itemCapacity);
                zone.updateLaunchCost();
            }).size(bsize);

            cont.addImage(stack.item.icon(Item.Icon.medium)).size(8 * 3).padRight(4);
            cont.label(() -> stack.amount + "").left();

            cont.row();
        }

        cont.addButton("$add", () -> {
            FloatingDialog dialog = new FloatingDialog("");
            dialog.setFillParent(false);
            for(Item item : content.items().select(item -> data.getItem(item) > 0 && item.type == ItemType.material && zone.getStartingItems().find(stack -> stack.item == item) == null)){
                TextButton button = dialog.cont.addButton("", "clear", () -> {
                    zone.getStartingItems().add(new ItemStack(item, 0));
                    zone.updateLaunchCost();
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
        }).colspan(4).size(100f, bsize).left().disabled(b -> !content.items().contains(item -> data.getItem(item) > 0 && item.type == ItemType.material && !zone.getStartingItems().contains(stack -> stack.item == item)));
        pack();
    }
}
