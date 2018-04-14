package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.event.HandCursorListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.player;
import static io.anuke.mindustry.Vars.tilesize;

public class BlockInventoryFragment implements Fragment {
    private Table table;
    private Tile tile;

    @Override
    public void build() {
        table = new Table();
    }

    public void showFor(Tile t){
        this.tile = t.target();
        if(tile == null || tile.entity == null || !tile.block().isAccessible()) return;
        rebuild();
    }

    public void hide(){
        table.clear();
        table.remove();
        table.setTouchable(Touchable.disabled);
        table.update(() -> {});
        tile = null;
    }

    private void rebuild(){
        IntSet container = new IntSet();

        table.clear();
        if(table.getParent() == null) Core.scene.add(table);
        table.background("clear");
        table.setTouchable(Touchable.enabled);
        table.update(() -> {
            if(tile == null || tile.entity == null || !tile.block().isAccessible()){
                hide();
            }else {
                updateTablePosition();
                if(tile.block().hasInventory) {
                    int[] items = tile.entity.inventory.items;
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

        if(tile.block().hasPower){
            table.add(new ItemImage(Draw.region("icon-power"), () -> round(tile.entity.power.amount), Colors.get("power")));

            if (row++ % cols == cols - 1) table.row();
        }

        if(tile.block().hasLiquids){
            ItemImage image = new ItemImage(Draw.region("icon-liquid"),
                    () -> round(tile.entity.liquid.amount),
                    tile.entity.liquid.liquid == Liquids.none ? Color.GRAY : tile.entity.liquid.liquid.color);

            image.addListener(new HandCursorListener());
            image.tapped(() -> {
                if (tile.entity.liquid.amount > 0) {
                    /*
                    int amount = Inputs.keyDown("item_withdraw") ? items[f] : 1;
                    items[f] -= amount;

                    move(item.region, image, () -> player.inventory.addItem(item, amount), Color.WHITE);*/
                }
            });

            table.add(image);
            if (row++ % cols == cols - 1) table.row();
        }

        if(tile.block().hasInventory) {
            int[] items = tile.entity.inventory.items;

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

                ItemImage image = new ItemImage(item.region, () -> round(items[f]), Color.WHITE);
                image.addListener(new HandCursorListener());
                image.tapped(() -> {
                    if (items[f] > 0) {
                        int amount = Inputs.keyDown("item_withdraw") ? items[f] : 1;
                        items[f] -= amount;

                        move(item.region, image, () -> player.inventory.addItem(item, amount), Color.WHITE);
                    }
                });
                table.add(image);

                if (row++ % cols == cols - 1) table.row();
            }
        }

        if(row == 0){
            table.add("[LIGHT_GRAY]<empty>");
        }

        updateTablePosition();
    }

    private void move(TextureRegion region, ItemImage image, Callable c, Color color){
        Vector2 v = image.localToStageCoordinates(new Vector2(image.getWidth() / 2f, image.getHeight() / 2f));
        Vector2 tv = Graphics.screen(player.x + Angles.trnsx(player.rotation + 180f, 5f), player.y + Angles.trnsy(player.rotation + 180f, 5f));
        float tx = tv.x, ty = tv.y;
        float dur = 40f / 60f;

        Image mover = new Image(region);
        mover.setColor(color);
        mover.setSize(32f, 32f);
        mover.setOrigin(Align.center);
        mover.setPosition(v.x, v.y, Align.center);
        mover.setTouchable(Touchable.disabled);
        mover.actions(
            Actions.parallel(
                Actions.moveToAligned(tx, ty, Align.center, dur, Interpolation.fade),
                Actions.scaleTo(0.7f, 0.7f, dur, Interpolation.fade),
                Actions.rotateTo(player.rotation, dur, Interpolation.fade)
            ),
            Actions.call(c),
            Actions.removeActor()
        );

        Core.scene.add(mover);
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
        Vector2 v =  Graphics.screen(tile.drawx() + tile.block().size*tilesize/2f, tile.drawy() + tile.block().size*tilesize/2f);
        table.pack();
        table.setPosition(v.x, v.y, Align.topLeft);
    }

}
