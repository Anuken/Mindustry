package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.ImageStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.OreBlock;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.*;

public class PlacementFragment extends Fragment{
    final int rowWidth = 4;

    Category currentCategory = Category.turret;
    Block hovered;
    Block lastDisplay;
    Tile hoverTile;
    Table blockTable;

    @Override
    public void build(Group parent){
        InputHandler input = control.input(0);

        parent.fill(frame -> {

            //rebuilds the category table with the correct recipes
            Runnable rebuildCategory = () -> {
                blockTable.clear();
                blockTable.top().margin(5);

                //blockTable.add(currentCategory.name()).colspan(rowWidth).growX(); //TODO localize

                int index = 0;

                ButtonGroup<ImageButton> group = new ButtonGroup<>();
                group.setMinCheckCount(0);

                for(Recipe recipe : content.recipes()){
                    if(recipe.category != currentCategory) continue;

                    if(index++ % rowWidth == 0){
                        blockTable.row();
                    }

                    ImageButton button = blockTable.addImageButton("blank", "select", 8*4,
                        () -> input.recipe = input.recipe == recipe ? null : recipe)
                    .size(50f).group(group).update(b -> b.setChecked(input.recipe == recipe)).get();

                    button.replaceImage(new ImageStack(recipe.result.getCompactIcon()));

                    if(!mobile){
                        button.hovered(() -> hovered = recipe.result);
                        button.exited(() -> {
                            if(hovered == recipe.result){
                                hovered = null;
                            }
                        });
                    }
                }
            };

            frame.bottom().left().visible(() -> !state.is(State.menu));

            frame.table("clear", top -> {
                top.add(new Table()).growX().update(topTable -> {
                    if((tileDisplayBlock() == null && lastDisplay == getSelected()) ||
                        (tileDisplayBlock() != null && lastDisplay == tileDisplayBlock())) return;

                    topTable.clear();
                    topTable.top().left().margin(5);

                    lastDisplay = getSelected();

                    if(lastDisplay != null){ //show selected recipe
                        topTable.table(header -> {
                            header.left();
                            header.add(new ImageStack(lastDisplay.getCompactIcon())).size(8*4);
                            header.labelWrap(lastDisplay.formalName).left().width(200f).padLeft(5);
                        }).growX().left();
                        topTable.row();
                        //add requirement table
                        topTable.table(req -> {
                            req.top().left();

                            for(ItemStack stack : Recipe.getByResult(lastDisplay).requirements){
                                req.table(line -> {
                                    line.left();
                                    line.addImage(stack.item.region).size(8*2);
                                    line.add(stack.item.localizedName()).color(Color.LIGHT_GRAY).padLeft(2).left();
                                    line.labelWrap(() -> {
                                        TileEntity core = players[0].getClosestCore();
                                        if(core == null || state.mode.infiniteResources) return "*/*";

                                        int amount = core.items.get(stack.item);
                                        String color = (amount < stack.amount / 2f ? "[red]" : amount < stack.amount ? "[accent]" : "[white]");

                                        return color + ui.formatAmount(amount) + "[white]/" + stack.amount;
                                    }).padLeft(5);
                                }).left();
                                req.row();
                            }
                        }).growX().left().margin(3);

                    }else if(tileDisplayBlock() != null){ //show selected tile
                        lastDisplay = tileDisplayBlock();
                        topTable.add(new ImageStack(lastDisplay.getDisplayIcon(hoverTile))).size(8*4);
                        topTable.labelWrap(lastDisplay.getDisplayName(hoverTile)).left().width(150f).padLeft(5);
                    }
                });
                top.row();
                top.addImage("blank").growX().color(Palette.accent).height(3f);
            }).colspan(3).fillX().visible(() -> getSelected() != null || tileDisplayBlock() != null).touchable(Touchable.enabled);
            frame.row();
            frame.table(categories -> {
                categories.defaults().size(48f);

                ButtonGroup<ImageButton> group = new ButtonGroup<>();

                for(Category cat : Category.values()){
                    categories.addImageButton("icon-" + cat.name(), "clear-toggle",  16*2, () -> {
                        currentCategory = cat;
                        rebuildCategory.run();
                    }).group(group);

                    if(cat.ordinal() %2 == 1) categories.row();
                }
            }).touchable(Touchable.enabled);
            frame.addImage("blank").width(3f).fillY().color(Palette.accent);

            frame.table("clear", blocks -> blockTable = blocks).fillY().bottom().touchable(Touchable.enabled);

            rebuildCategory.run();
        });
    }

    /**Returns the currently displayed block in the top box.*/
    Block getSelected(){
        Block toDisplay = null;

        //setup hovering tile
        if(!ui.hasMouse()){
            Tile tile = world.tileWorld(Graphics.mouseWorld().x, Graphics.mouseWorld().y);
            if(tile != null){
                hoverTile = tile.target();
            }else{
                hoverTile = null;
            }
        }else{
            hoverTile = null;
        }

        //block currently selected
        if(control.input(0).recipe != null){
            toDisplay = control.input(0).recipe.result;
        }

        //block hovered on in build menu
        if(hovered != null){
            toDisplay = hovered;
        }

        return toDisplay;
    }

    /**Returns the block currently being hovered over in the world.*/
    Block tileDisplayBlock(){
        return hoverTile == null ? null : hoverTile.block().synthetic() ? hoverTile.block() : hoverTile.floor() instanceof OreBlock ? hoverTile.floor() : null;
    }

    /**Rebuilds the whole placement menu, attempting to preserve previous state.*/
    void rebuild(){

    }

    void toggle(float t, Interpolation ip){

    }
}