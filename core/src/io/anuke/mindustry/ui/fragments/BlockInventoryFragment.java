package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.ItemTransfer;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BooleanProvider;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.event.HandCursorListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.state;

public class BlockInventoryFragment implements Fragment {
    private Table table;
    private boolean shown;
    private Tile tile;
    private InputHandler input;

    public BlockInventoryFragment(InputHandler input){
        this.input = input;
    }


    @Override
    public void build(Group parent) {
        table = new Table();
        table.setVisible(() -> !state.is(State.menu) && shown);
        parent.addChild(table);
    }

    public void showFor(Tile t){
        this.tile = t.target();
        if(tile == null || tile.entity == null || !tile.block().isAccessible()) return;
        rebuild();
    }

    public void hide(){
        shown = false;
        table.clear();
        table.setTouchable(Touchable.disabled);
        table.update(() -> {});
        tile = null;
    }

    private void rebuild(){
        Player player = input.player;

        shown = true;
        IntSet container = new IntSet();

        table.clear();
        table.background("clear");
        table.setTouchable(Touchable.enabled);
        table.update(() -> {
            if(tile == null || tile.entity == null || !tile.block().isAccessible()){
                hide();
            }else {
                updateTablePosition();
                if(tile.block().hasItems) {
                    int[] items = tile.entity.items.items;
                    for (int i = 0; i < items.length; i++) {
                        if ((items[i] == 0) == container.contains(i)) {
                            rebuild();
                        }
                    }
                }
            }
        });

        int cols = 3;
        int row = 0;

        table.margin(3f);
        table.defaults().size(16 * 2).space(6f);

        if(tile.block().hasItems) {
            int[] items = tile.entity.items.items;

            for (int i = 0; i < items.length; i++) {
                final int f = i;
                if (items[i] == 0) continue;
                Item item = Item.getByID(i);

                container.add(i);

                Table t = new Table();
                t.left().bottom();

                t.label(() -> round(items[f])).color(Color.DARK_GRAY);
                t.row();
                t.label(() -> round(items[f])).padTop(-22);

                BooleanProvider canPick = () -> player.inventory.canAcceptItem(item);

                HandCursorListener l = new HandCursorListener();
                l.setEnabled(canPick);

                ItemImage image = new ItemImage(item.region, () -> round(items[f]), Color.WHITE);
                image.addListener(l);
                image.tapped(() -> {
                    if(!canPick.get()) return;
                    if (items[f] > 0) {
                        int amount = Math.min(Inputs.keyDown("item_withdraw") ? items[f] : 1, player.inventory.itemCapacityUsed(item));
                        tile.block().removeStack(tile, item, amount);

                        int sent = Mathf.clamp(amount/3, 1, 8);
                        int per = Math.min(amount/sent, 5);
                        int[] soFar = {amount};
                        for(int j = 0; j < sent; j ++){
                            boolean all = j == sent-1;
                            Timers.run(j*5, () -> ItemTransfer.create(item, tile.drawx(), tile.drawy(), player, () -> {
                                player.inventory.addItem(item, all ? soFar[0] : per);
                                soFar[0] -= per;
                            }));
                        }
                    }
                });
                table.add(image);

                if (row++ % cols == cols - 1) table.row();
            }
        }

        if(row == 0){
            table.addImage("icon-items-none").color(Color.LIGHT_GRAY);
        }

        updateTablePosition();
    }

    private String round(float f){
        f = (int)f;
        if(f >= 1000){
            return Strings.toFixed(f/1000, 1) + "k";
        }else{
            return (int)f+"";
        }
    }

    private void updateTablePosition(){
        Vector2 v =  Graphics.screen(tile.drawx(), tile.drawy());
        table.pack();
        table.setPosition(v.x, v.y, Align.center);
    }

}
