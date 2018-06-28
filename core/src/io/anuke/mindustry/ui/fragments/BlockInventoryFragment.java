package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.net.In;
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
import static io.anuke.mindustry.Vars.tilesize;

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
        if(tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.totalItems() == 0) return;
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
            if(tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.totalItems() == 0){
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
        table.defaults().size(16*2).space(6f);

        if(tile.block().hasItems) {
            int[] items = tile.entity.items.items;

            for (int i = 0; i < items.length; i++) {
                final int f = i;
                if (items[i] == 0) continue;
                Item item = Item.getByID(i);

                container.add(i);

                BooleanProvider canPick = () -> player.inventory.canAcceptItem(item);

                HandCursorListener l = new HandCursorListener();
                l.setEnabled(canPick);

                ItemImage image = new ItemImage(item.region, () -> round(items[f]));
                image.addListener(l);
                image.tapped(() -> {
                    if(!canPick.get() || items[f] == 0) return;
                    int amount = Math.min(Inputs.keyDown("item_withdraw") ? items[f] : 1, player.inventory.itemCapacityUsed(item));
                    CallBlocks.requestItem(player, tile, item, amount);
                });
                table.add(image);

                if (row++ % cols == cols - 1) table.row();
            }
        }

        if(row == 0){
            table.setSize(0f, 0f);
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
        Vector2 v =  Graphics.screen(tile.drawx() + tile.block().size * tilesize/2f, tile.drawy() + tile.block().size * tilesize/2f);
        table.pack();
        table.setPosition(v.x, v.y, Align.topLeft);
    }

    @Remote(called = Loc.server, targets = Loc.both, in = In.blocks, forward = true)
    public static void requestItem(Player player, Tile tile, Item item, int amount){
        int removed = tile.block().removeStack(tile, item, amount);

        player.inventory.addItem(item, removed);
        for(int j = 0; j < Mathf.clamp(removed/3, 1, 8); j ++){
            Timers.run(j*3f, () -> CallEntity.transferItemEffect(item, tile.drawx(), tile.drawy(), player));
        }
    }
}
