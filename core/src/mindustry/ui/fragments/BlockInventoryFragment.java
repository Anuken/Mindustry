package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.ui.layout.Stack;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.util.*;

import static mindustry.Vars.*;

public class BlockInventoryFragment extends Fragment{
    private static final float holdWithdraw = 20f;
    private static final float holdShrink = 120f;

    Table table = new Table();
    Building tile;
    float holdTime = 0f, emptyTime;
    boolean holding;
    float[] shrinkHoldTimes = new float[content.items().size];
    Item lastItem;

    {
        Events.on(WorldLoadEvent.class, e -> hide());
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void requestItem(Player player, Building tile, Item item, int amount){
        if(player == null || tile == null || !tile.interactable(player.team()) || !player.within(tile, buildingRange)) return;
        amount = Math.min(player.unit().maxAccepted(item), amount);
        int fa = amount;

        if(amount == 0) return;

        if(net.server() && (!Units.canInteract(player, tile) ||
            !netServer.admins.allowAction(player, ActionType.withdrawItem, tile.tile(), action -> {
                action.item = item;
                action.itemAmount = fa;
            }))) throw new ValidateException(player, "Player cannot request items.");

        int removed = tile.removeStack(item, amount);

        player.unit().addItem(item, removed);
        Events.fire(new WithdrawEvent(tile, player, item, amount));
        for(int j = 0; j < Mathf.clamp(removed / 3, 1, 8); j++){
            Time.run(j * 3f, () -> Call.transferItemEffect(item, tile.x, tile.y, player.unit()));
        }
    }

    @Override
    public void build(Group parent){
        table.name = "inventory";
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void showFor(Building t){
        if(this.tile == t){
            hide();
            return;
        }
        this.tile = t;
        if(tile == null || !tile.block.isAccessible() || tile.items.total() == 0)
            return;
        rebuild(true);
    }

    public void hide(){
        if(table == null) return;

        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interp.pow3Out), Actions.run(() -> {
            table.clearChildren();
            table.clearListeners();
            table.update(null);
        }), Actions.visible(false));
        table.touchable = Touchable.disabled;
        tile = null;
    }

    private void rebuild(boolean actions){
        IntSet container = new IntSet();

        Arrays.fill(shrinkHoldTimes, 0);
        holdTime = emptyTime = 0f;

        table.clearChildren();
        table.clearActions();
        table.background(Tex.inventory);
        table.touchable = Touchable.enabled;
        table.update(() -> {

            if(state.isMenu() || tile == null || !tile.isValid() || !tile.block.isAccessible() || emptyTime >= holdShrink){
                hide();
            }else{
                if(tile.items.total() == 0){
                    emptyTime += Time.delta;
                }else{
                    emptyTime = 0f;
                }

                if(holding && lastItem != null){
                    holdTime += Time.delta;

                    if(holdTime >= holdWithdraw){
                        int amount = Math.min(tile.items.get(lastItem), player.unit().maxAccepted(lastItem));
                        Call.requestItem(player, tile, lastItem, amount);
                        holding = false;
                        holdTime = 0f;

                        if(net.client()) Events.fire(new WithdrawEvent(tile, player, lastItem, amount));
                    }
                }

                updateTablePosition();
                if(tile.block.hasItems){
                    boolean dirty = false;
                    if(shrinkHoldTimes.length != content.items().size) shrinkHoldTimes = new float[content.items().size];

                    for(int i = 0; i < content.items().size; i++){
                        boolean has = tile.items.has(content.item(i));
                        boolean had = container.contains(i);
                        if(has){
                            shrinkHoldTimes[i] = 0f;
                            dirty |= !had;
                        }else if(had){
                            shrinkHoldTimes[i] += Time.delta;
                            dirty |= shrinkHoldTimes[i] >= holdShrink;
                        }
                    }
                    if(dirty) rebuild(false);
                }
            }
        });

        int cols = 3;
        int row = 0;

        table.margin(4f);
        table.defaults().size(8 * 5).pad(4f);

        if(tile.block.hasItems){

            for(int i = 0; i < content.items().size; i++){
                Item item = content.item(i);
                if(!tile.items.has(item)) continue;

                container.add(i);

                Boolp canPick = () -> player.unit().acceptsItem(item) && !state.isPaused() && player.within(tile, itemTransferRange);

                HandCursorListener l = new HandCursorListener();
                l.setEnabled(canPick);

                Element image = itemImage(item.icon(Cicon.xlarge), () -> {
                    if(tile == null || !tile.isValid()){
                        return "";
                    }
                    return round(tile.items.get(item));
                });
                image.addListener(l);

                image.addListener(new InputListener(){
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        if(!canPick.get() || tile == null || !tile.isValid() || tile.items == null || !tile.items.has(item)) return false;
                        int amount = Math.min(1, player.unit().maxAccepted(item));
                        if(amount > 0){
                            Call.requestItem(player, tile, item, amount);
                            lastItem = item;
                            holding = true;
                            holdTime = 0f;
                            if(net.client()) Events.fire(new WithdrawEvent(tile, player, item, amount));
                        }
                        return true;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
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

        table.visible = true;

        if(actions){
            table.setScale(0f, 1f);
            table.actions(Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out));
        }else{
            table.setScale(1f, 1f);
        }
    }

    private String round(float f){
        f = (int)f;
        if(f >= 1000000){
            return (int)(f / 1000000f) + "[gray]" + Core.bundle.getOrNull("unit.millions") + "[]";
        }else if(f >= 1000){
            return (int)(f / 1000) + Core.bundle.getOrNull("unit.thousands");
        }else{
            return (int)f + "";
        }
    }

    private void updateTablePosition(){
        Vec2 v = Core.input.mouseScreen(tile.x + tile.block.size * tilesize / 2f, tile.y + tile.block.size * tilesize / 2f);
        table.pack();
        table.setPosition(v.x, v.y, Align.topLeft);
    }

    private Element itemImage(TextureRegion region, Prov<CharSequence> text){
        Stack stack = new Stack();

        Table t = new Table().left().bottom();
        t.label(text);

        stack.add(new Image(region));
        stack.add(t);
        return stack;
    }
}
