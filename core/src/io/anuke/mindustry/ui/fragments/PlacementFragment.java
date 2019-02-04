package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.ButtonGroup;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.Binding;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.OreBlock;

import static io.anuke.mindustry.Vars.*;

public class PlacementFragment extends Fragment{
    final int rowWidth = 4;

    Array<Block> returnArray = new Array<>();
    Category currentCategory = Category.distribution;
    Block hovered, lastDisplay;
    Tile lastHover;
    Tile hoverTile;
    Table blockTable, toggler, topTable;
    boolean shown = true;
    boolean lastGround;

    //TODO make this configurable
    final KeyCode[] inputGrid = {
        KeyCode.NUM_1, KeyCode.NUM_2, KeyCode.NUM_3, KeyCode.NUM_4,
        KeyCode.Q, KeyCode.W, KeyCode.E, KeyCode.R,
        KeyCode.A, KeyCode.S, KeyCode.D, KeyCode.F,
        KeyCode.Z, KeyCode.X, KeyCode.C, KeyCode.V
    }, inputCatGrid = {
        KeyCode.NUM_1, KeyCode.NUM_2,
        KeyCode.Q, KeyCode.W,
        KeyCode.A, KeyCode.S,
        KeyCode.Z, KeyCode.X, KeyCode.C, KeyCode.V
    };

    public PlacementFragment(){
        Events.on(WorldLoadEvent.class, event -> {
            currentCategory = Category.turret;
            Group group = toggler.getParent();
            toggler.remove();
            build(group);
        });
    }

    boolean gridUpdate(InputHandler input){
        if(Core.input.keyDown(Binding.pick)){ //mouse eyedropper select
            Tile tile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(tile != null){
                tile = tile.target();
                Block tryRecipe = tile.block();
                if(tryRecipe.isVisible() && data.isUnlocked(tryRecipe)){
                    input.block = tryRecipe;
                    currentCategory = input.block.buildCategory;
                    return true;
                }
            }
        }

        if(!Core.input.keyDown(Binding.gridMode) || ui.chatfrag.chatOpen()) return false;
        if(Core.input.keyDown(Binding.gridModeShift)){ //select category
            int i = 0;
            for(KeyCode key : inputCatGrid){
                if(Core.input.keyDown(key)){
                    input.block = getByCategory(Category.values()[i]).first();
                    currentCategory = input.block.buildCategory;
                }
                i++;
            }
            return true;
        }else{ //select block
            int i = 0;
            Array<Block> recipes = getByCategory(currentCategory);
            for(KeyCode key : inputGrid){
                if(Core.input.keyDown(key))
                    input.block = (i < recipes.size && data.isUnlocked(recipes.get(i))) ? recipes.get(i) : null;
                i++;
            }
        }
        return false;
    }

    @Override
    public void build(Group parent){
        parent.fill(full -> {
            toggler = full;
            full.bottom().right().visible(() -> !state.is(State.menu));

            full.table(frame -> {
                InputHandler input = control.input(0);

                //rebuilds the category table with the correct recipes
                Runnable rebuildCategory = () -> {
                    blockTable.clear();
                    blockTable.top().margin(5);

                    int index = 0;

                    ButtonGroup<ImageButton> group = new ButtonGroup<>();
                    group.setMinCheckCount(0);

                    for(Block block : getByCategory(currentCategory)){

                        if(index++ % rowWidth == 0){
                            blockTable.row();
                        }

                        boolean[] unlocked = {false};

                        ImageButton button = blockTable.addImageButton("icon-locked", "select", 8 * 4, () -> {
                            if(data.isUnlocked(block)){
                                input.block = input.block == block ? null : block;
                            }
                        }).size(46f).group(group).get();

                        button.update(() -> { //color unplacable things gray
                            boolean ulock = data.isUnlocked(block);
                            TileEntity core = players[0].getClosestCore();
                            Color color = core != null && (core.items.has(block.buildRequirements) || state.rules.infiniteResources) ? Color.WHITE : ulock ? Color.GRAY : Color.WHITE;
                            button.forEach(elem -> elem.setColor(color));
                            button.setChecked(input.block == block);

                            if(ulock == unlocked[0]) return;
                            unlocked[0] = ulock;

                            if(!ulock){
                                button.replaceImage(new Image("icon-locked"));
                            }else{
                                button.replaceImage(new Image(block.icon(Icon.medium)));
                            }
                        });

                        button.hovered(() -> hovered = block);
                        button.exited(() -> {
                            if(hovered == block){
                                hovered = null;
                            }
                        });
                    }
                    blockTable.act(0f);
                };

                //top table with hover info
                frame.table("button-edge-2", top -> {
                    topTable = top;
                    top.add(new Table()).growX().update(topTable -> {
                        //don't refresh unnecessarily
                        if((tileDisplayBlock() == null && lastDisplay == getSelected() && !lastGround)
                        || (tileDisplayBlock() != null && lastHover == hoverTile && lastGround))
                            return;

                        topTable.clear();
                        topTable.top().left().margin(5);

                        lastHover = hoverTile;
                        lastDisplay = getSelected();
                        lastGround = tileDisplayBlock() != null;

                        if(lastDisplay != null){ //show selected recipe
                            lastGround = false;

                            topTable.table(header -> {
                                header.left();
                                header.add(new Image(lastDisplay.icon(Icon.medium))).size(8 * 4);
                                header.labelWrap(() -> !data.isUnlocked(lastDisplay) ? Core.bundle.get("blocks.unknown") : lastDisplay.formalName)
                                .left().width(190f).padLeft(5);
                                header.add().growX();
                                if(data.isUnlocked(lastDisplay)){
                                    header.addButton("?", "clear-partial", () -> ui.content.show(lastDisplay))
                                    .size(8 * 5).padTop(-5).padRight(-5).right().grow();
                                }
                            }).growX().left();
                            topTable.row();
                            //add requirement table
                            topTable.table(req -> {
                                req.top().left();

                                for(ItemStack stack : lastDisplay.buildRequirements){
                                    req.table(line -> {
                                        line.left();
                                        line.addImage(stack.item.region).size(8 * 2);
                                        line.add(stack.item.localizedName()).color(Color.LIGHT_GRAY).padLeft(2).left();
                                        line.labelWrap(() -> {
                                            TileEntity core = players[0].getClosestCore();
                                            if(core == null || state.rules.infiniteResources) return "*/*";

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
                            topTable.table(t -> {
                                t.left();
                                t.add(new Image(lastDisplay.getDisplayIcon(hoverTile))).size(8 * 4);
                                t.labelWrap(lastDisplay.getDisplayName(hoverTile)).left().width(190f).padLeft(5);
                            }).growX().left();
                            if(hoverTile.getTeam() == players[0].getTeam()){
                                topTable.row();
                                topTable.table(t -> {
                                    t.left().defaults().left();
                                    lastDisplay.display(hoverTile, t);
                                }).left().growX();
                            }
                        }
                    });
                }).colspan(3).fillX().visible(() -> getSelected() != null || tileDisplayBlock() != null).touchable(Touchable.enabled);
                frame.row();
                frame.addImage("blank").color(Palette.accent).colspan(3).height(3).growX();
                frame.row();
                frame.table("pane-2", blocksSelect -> {
                    blocksSelect.margin(4).marginTop(0);
                    blocksSelect.table(blocks -> blockTable = blocks).grow();
                    blocksSelect.row();
                    blocksSelect.table(input::buildUI).growX();
                }).fillY().bottom().touchable(Touchable.enabled);
                frame.table(categories -> {
                    categories.defaults().size(50f);

                    ButtonGroup<ImageButton> group = new ButtonGroup<>();

                    for(Category cat : Category.values()){
                        if(getByCategory(cat).isEmpty()) continue;

                        categories.addImageButton("icon-" + cat.name(), "clear-toggle", 16 * 2, () -> {
                            currentCategory = cat;
                            rebuildCategory.run();
                        }).group(group).update(i -> i.setChecked(currentCategory == cat));

                        if(cat.ordinal() % 2 == 1) categories.row();
                    }
                }).touchable(Touchable.enabled);

                rebuildCategory.run();
                frame.update(() -> {
                    if(gridUpdate(input)) rebuildCategory.run();
                });
            });
        });
    }
    
    Array<Block> getByCategory(Category cat){
        returnArray.clear();
        for(Block block : content.blocks()){
            if(block.buildCategory == cat && block.isVisible()){
                returnArray.add(block);
            }
        }
        return returnArray;
    }

    /** Returns the currently displayed block in the top box. */
    Block getSelected(){
        Block toDisplay = null;

        Vector2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //setup hovering tile
        if(!Core.scene.hasMouse() && topTable.hit(v.x, v.y, false) == null){
            Tile tile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            if(tile != null){
                hoverTile = tile.target();
            }else{
                hoverTile = null;
            }
        }else{
            hoverTile = null;
        }

        //block currently selected
        if(control.input(0).block != null){
            toDisplay = control.input(0).block;
        }

        //block hovered on in build menu
        if(hovered != null){
            toDisplay = hovered;
        }

        return toDisplay;
    }

    /** Returns the block currently being hovered over in the world. */
    Block tileDisplayBlock(){
        return hoverTile == null ? null : hoverTile.block().synthetic() ? hoverTile.block() : hoverTile.floor() instanceof OreBlock ? hoverTile.floor() : null;
    }

    /** Show or hide the placement menu. */
    void toggle(float t, Interpolation ip){
        toggler.clearActions();
        if(shown){
            shown = false;
            toggler.actions(Actions.translateBy(toggler.getTranslation().x + toggler.getWidth(), 0, t, ip));
        }else{
            shown = true;
            toggler.actions(Actions.translateBy(-toggler.getTranslation().x, 0, t, ip));
        }
    }
}