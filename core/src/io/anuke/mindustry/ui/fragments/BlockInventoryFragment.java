package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
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

import static io.anuke.mindustry.Vars.*;

public class BlockInventoryFragment extends Fragment{
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

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void requestItem(Player player, Tile tile, Item item, int amount){
        if(player == null || tile == null) return;

        int removed = tile.block().removeStack(tile, item, amount);

        player.inventory.addItem(item, removed);
        for(int j = 0; j < Mathf.clamp(removed / 3, 1, 8); j++){
            Timers.run(j * 3f, () -> Call.transferItemEffect(item, tile.drawx(), tile.drawy(), player));
        }
    }

    @Override
    public void build(Group parent){
        table = new Table();
        table.visible(() -> !state.is(State.menu));
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void showFor(Tile t){
        this.tile = t.target();
        if(tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.total() == 0)
            return;
        rebuild(true);
    }

    public void hide(){
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false), Actions.run(() -> {
            table.clear();
            table.update(null);
        }));
        table.setTouchable(Touchable.disabled);
        tile = null;
    }

    private void rebuild(boolean actions){

        Player player = input.player;

        IntSet container = new IntSet();

        table.clearChildren();
        table.background("inventory");
        table.setTouchable(Touchable.enabled);
        table.update(() -> {
            if(state.is(State.menu) || tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.total() == 0){
                hide();
            }else{
                if(holding && lastItem != null){
                    holdTime += Timers.delta();

                    if(holdTime >= holdWithdraw){
                        int amount = Math.min(tile.entity.items.get(lastItem), player.inventory.itemCapacityUsed(lastItem));
                        Call.requestItem(player, tile, lastItem, amount);
                        holding = false;
                        holdTime = 0f;
                    }
                }

                updateTablePosition();
                if(tile.block().hasItems){
                    for(int i = 0; i < content.items().size; i++){
                        boolean has = tile.entity.items.has(content.item(i));
                        if(has != container.contains(i)){
                            rebuild(false);
                        }
                    }
                }
            }
        });

        int cols = 3;
        int row = 0;

        table.margin(6f);
        table.defaults().size(mobile ? 16 * 3 : 16 * 2).space(6f);

        if(tile.block().hasItems){

            for(int i = 0; i < content.items().size; i++){
                Item item = content.item(i);
                if(!tile.entity.items.has(item)) continue;

                container.add(i);

                BooleanProvider canPick = () -> player.inventory.canAcceptItem(item);

                HandCursorListener l = new HandCursorListener();
                l.setEnabled(canPick);

                ItemImage image = new ItemImage(item.region, () -> {
                    if(tile == null || tile.entity == null){
                        return "";
                    }
                    return round(tile.entity.items.get(item));
                });
                image.addListener(l);

                image.addListener(new InputListener(){
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                        if(!canPick.get() || !tile.entity.items.has(item)) return false;
                        int amount = Math.min(1, player.inventory.itemCapacityUsed(item));
                        Call.requestItem(player, tile, item, amount);
                        lastItem = item;
                        holding = true;
                        holdTime = 0f;
                        return true;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, int button){
                        holding = false;
                        lastItem = null;
                    }
                });
                table.add(image);

                if(row++ % cols == cols - 1) table.row();
            }
        }

        if(row == 0){
            table.setSize(0f, 0f);
        }

        updateTablePosition();

        if(actions){
            table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                    Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));
        }
    }

    private String round(float f){
        f = (int) f;
        if(f >= 1000000){
            return Strings.toFixed(f / 1000000f, 1) + "[gray]mil[]";
        }else if(f >= 1000){
            return Strings.toFixed(f / 1000, 1) + "k";
        }else{
            return (int) f + "";
        }
    }

    private void updateTablePosition(){
        Vector2 v = Graphics.screen(tile.drawx() + tile.block().size * tilesize / 2f, tile.drawy() + tile.block().size * tilesize / 2f);
        table.pack();
        table.setPosition(v.x, v.y, Align.topLeft);
    }
}
