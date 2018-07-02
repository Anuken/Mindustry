package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.math.Interpolation;
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
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.event.HandCursorListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.mobile;
import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.tilesize;

public class BlockInventoryFragment implements Fragment {
    private final static float holdWithdraw = 40f;

    private Table table;
    private Tile tile;
    private InputHandler input;
    private float holdTime = 0f;
    private boolean holding;
    private Item lastItem;

    public BlockInventoryFragment(InputHandler input){
        this.input = input;
    }

    @Override
    public void build(Group parent) {
        table = new Table();
        table.setVisible(() -> !state.is(State.menu));
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void showFor(Tile t){
        this.tile = t.target();
        if(tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.totalItems() == 0) return;
        rebuild();
    }

    public void hide(){
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false), Actions.run(() -> {
            table.clear();
            table.update(null);
        }));
        table.setTouchable(Touchable.disabled);
        tile = null;
    }

    private void rebuild(){
        Player player = input.player;

        IntSet container = new IntSet();

        table.clear();
        table.background("inventory");
        table.setTouchable(Touchable.enabled);
        table.update(() -> {
            if(tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.totalItems() == 0){
                hide();
            }else{
                if(holding && lastItem != null){
                    holdTime += Timers.delta();

                    if(holdTime >= holdWithdraw){
                        int amount = Math.min(tile.entity.items.getItem(lastItem), player.inventory.itemCapacityUsed(lastItem));
                        CallBlocks.requestItem(player, tile, lastItem, amount);
                        holding = false;
                        holdTime = 0f;
                    }
                }

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

        table.margin(6f);
        table.defaults().size(mobile ? 16*3 : 16*2).space(6f);

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

                image.addListener(new InputListener(){
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        if(!canPick.get() || items[f] == 0) return false;
                        int amount = Math.min(Inputs.keyDown("item_withdraw") ? items[f] : 1, player.inventory.itemCapacityUsed(item));
                        CallBlocks.requestItem(player, tile, item, amount);
                        lastItem = item;
                        holding = true;
                        holdTime = 0f;
                        return true;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                        holding = false;
                        lastItem = null;
                    }
                });
                table.add(image);

                if (row++ % cols == cols - 1) table.row();
            }
        }

        if(row == 0){
            table.setSize(0f, 0f);
        }

        updateTablePosition();

        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));
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
