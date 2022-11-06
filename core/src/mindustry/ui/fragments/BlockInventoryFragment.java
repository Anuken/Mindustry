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
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.*;

public class BlockInventoryFragment{
    private static final float holdWithdraw = 20f;
    private static final float holdShrink = 120f;

    Table table = new Table();
    Building build;
    float holdTime = 0f, emptyTime;
    boolean holding, held;
    float[] shrinkHoldTimes = new float[content.items().size];
    Item lastItem;

    {
        Events.on(WorldLoadEvent.class, e -> hide());
    }

    public void build(Group parent){
        table.name = "inventory";
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void showFor(Building t){
        if(this.build == t){
            hide();
            return;
        }
        this.build = t;
        if(build == null || !build.block.isAccessible() || build.items == null || build.items.total() == 0){
            return;
        }
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
        build = null;
    }

    private void takeItem(int requested){
        if(!build.canWithdraw()) return;

        //take everything
        int amount = Math.min(requested, player.unit().maxAccepted(lastItem));

        if(amount > 0){
            Call.requestItem(player, build, lastItem, amount);
            holding = false;
            holdTime = 0f;
            held = true;

            if(net.client()) Events.fire(new WithdrawEvent(build, player, lastItem, amount));
        }
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

            if(state.isMenu() || build == null || !build.isValid() || !build.block.isAccessible() || emptyTime >= holdShrink){
                hide();
            }else{
                if(build.items.total() == 0){
                    emptyTime += Time.delta;
                }else{
                    emptyTime = 0f;
                }

                if(holding && lastItem != null && (holdTime += Time.delta) >= holdWithdraw){
                    holdTime = 0f;

                    //take one when held
                    takeItem(1);
                }

                updateTablePosition();
                if(build.block.hasItems){
                    boolean dirty = false;
                    if(shrinkHoldTimes.length != content.items().size) shrinkHoldTimes = new float[content.items().size];

                    for(int i = 0; i < content.items().size; i++){
                        boolean has = build.items.has(content.item(i));
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

                if(table.getChildren().isEmpty()){
                    hide();
                }
            }
        });

        int cols = 3;
        int row = 0;

        table.margin(4f);
        table.defaults().size(8 * 5).pad(4f);

        if(build.block.hasItems){

            for(int i = 0; i < content.items().size; i++){
                Item item = content.item(i);
                if(!build.items.has(item)) continue;

                container.add(i);

                Boolp canPick = () -> player.unit().acceptsItem(item) && !state.isPaused() && player.within(build, itemTransferRange);

                HandCursorListener l = new HandCursorListener();
                l.enabled = canPick;

                Element image = itemImage(item.uiIcon, () -> {
                    if(build == null || !build.isValid()){
                        return "";
                    }
                    return round(build.items.get(item));
                });
                image.addListener(l);

                Boolp validClick = () -> !(!canPick.get() || build == null || !build.isValid() || build.items == null || !build.items.has(item));

                image.addListener(new ClickListener(){

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        held = false;
                        if(validClick.get()){
                            lastItem = item;
                            holding = true;
                        }

                        return super.touchDown(event, x, y, pointer, button);
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y){
                        if(!validClick.get() || held) return;

                        //take all
                        takeItem(build.items.get(lastItem = item));
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                        super.touchUp(event, x, y, pointer, button);

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
            return (int)(f / 1000000f) + "[gray]" + UI.millions;
        }else if(f >= 1000){
            return (int)(f / 1000) + UI.thousands;
        }else{
            return (int)f + "";
        }
    }

    private void updateTablePosition(){
        Vec2 v = Core.input.mouseScreen(build.x + build.block.size * tilesize / 2f, build.y + build.block.size * tilesize / 2f);
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
