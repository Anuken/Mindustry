package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.Styles;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class PlacementFragment extends Fragment{
    final int rowWidth = 4;

    Array<Block> returnArray = new Array<>();
    Array<Category> returnCatArray = new Array<>();
    boolean[] categoryEmpty = new boolean[Category.all.length];
    Category currentCategory = Category.distribution;
    Block hovered, lastDisplay;
    Tile lastHover;
    Tile hoverTile;
    Table blockTable, toggler, topTable;
    boolean lastGround;

    //not configurable, no plans to make it configurable
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
            Core.app.post(() -> {
                control.input.block = null;
                rebuild();
            });
        });

        Events.on(UnlockEvent.class, event -> {
            if(event.content instanceof Block){
                rebuild();
            }
        });
    }

    void rebuild(){
        currentCategory = Category.turret;
        Group group = toggler.getParent();
        int index = toggler.getZIndex();
        toggler.remove();
        build(group);
        toggler.setZIndex(index);
    }

    boolean gridUpdate(InputHandler input){
        if(Core.input.keyDown(Binding.pick)){ //mouse eyedropper select
            Tile tile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(tile != null){
                tile = tile.link();
                Block tryRecipe = tile.block();
                if(tryRecipe.isVisible() && unlocked(tryRecipe)){
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
                    input.block = getByCategory(Category.all[i]).first();
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
                    input.block = (i < recipes.size && unlocked(recipes.get(i))) ? recipes.get(i) : null;
                i++;
            }
        }
        return false;
    }

    @Override
    public void build(Group parent){
        parent.fill(full -> {
            toggler = full;
            full.bottom().right().visible(() -> ui.hudfrag.shown());

            full.table(frame -> {

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

                        if(!unlocked(block)){
                            blockTable.add().size(46);
                            continue;
                        }

                        ImageButton button = blockTable.addImageButton(Icon.lockedSmall, Styles.selecti, () -> {
                            if(unlocked(block)){
                                control.input.block = control.input.block == block ? null : block;
                            }
                        }).size(46f).group(group).name("block-" + block.name).get();

                        button.getStyle().imageUp = new TextureRegionDrawable(block.icon(Block.Icon.medium));

                        button.update(() -> { //color unplacable things gray
                            TileEntity core = player.getClosestCore();
                            Color color = state.rules.infiniteResources || (core != null && (core.items.has(block.buildRequirements, state.rules.buildCostMultiplier) || state.rules.infiniteResources)) ? Color.white : Color.gray;
                            button.forEach(elem -> elem.setColor(color));
                            button.setChecked(control.input.block == block);
                        });

                        button.hovered(() -> hovered = block);
                        button.exited(() -> {
                            if(hovered == block){
                                hovered = null;
                            }
                        });
                    }
                    //add missing elements to even out table size
                    if(index < 4){
                        for(int i = 0; i < 4-index; i++){
                            blockTable.add().size(46f);
                        }
                    }
                    blockTable.act(0f);
                };

                //top table with hover info
                frame.table(Tex.buttonEdge2,top -> {
                    topTable = top;
                    top.add(new Table()).growX().update(topTable -> {
                        //don't refresh unnecessarily
                        if((tileDisplayBlock() == null && lastDisplay == getSelected() && !lastGround)
                        || (tileDisplayBlock() != null && lastHover == hoverTile && lastDisplay == tileDisplayBlock() && lastGround))
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
                                header.add(new Image(lastDisplay.icon(Block.Icon.medium))).size(8 * 4);
                                header.labelWrap(() -> !unlocked(lastDisplay) ? Core.bundle.get("block.unknown") : lastDisplay.localizedName)
                                .left().width(190f).padLeft(5);
                                header.add().growX();
                                if(unlocked(lastDisplay)){
                                    header.addButton("?", Styles.clearPartialt, () -> {
                                        ui.content.show(lastDisplay);
                                        Events.fire(new BlockInfoEvent());
                                    }).size(8 * 5).padTop(-5).padRight(-5).right().grow().name("blockinfo");
                                }
                            }).growX().left();
                            topTable.row();
                            //add requirement table
                            topTable.table(req -> {
                                req.top().left();

                                for(ItemStack stack : lastDisplay.buildRequirements){
                                    req.table(line -> {
                                        line.left();
                                        line.addImage(stack.item.icon(Item.Icon.small)).size(8 * 2);
                                        line.add(stack.item.localizedName()).color(Color.lightGray).padLeft(2).left();
                                        line.labelWrap(() -> {
                                            TileEntity core = player.getClosestCore();
                                            if(core == null || state.rules.infiniteResources) return "*/*";

                                            int amount = core.items.get(stack.item);
                                            int stackamount = Math.round(stack.amount * state.rules.buildCostMultiplier);
                                            String color = (amount < stackamount / 2f ? "[red]" : amount < stackamount ? "[accent]" : "[white]");

                                            return color + ui.formatAmount(amount) + "[white]/" + stackamount;
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
                            if(hoverTile.getTeam() == player.getTeam()){
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
                frame.addImage().color(Pal.gray).colspan(3).height(4).growX();
                frame.row();
                frame.table(Tex.pane2, blocksSelect -> {
                    blocksSelect.margin(4).marginTop(0);
                    blocksSelect.table(blocks -> blockTable = blocks).grow();
                    blocksSelect.row();
                    blocksSelect.table(control.input::buildUI).name("inputTable").growX();
                }).fillY().bottom().touchable(Touchable.enabled);
                frame.table(categories -> {
                    categories.defaults().size(50f);

                    ButtonGroup<ImageButton> group = new ButtonGroup<>();

                    //update category empty values
                    for(Category cat : Category.all){
                        Array<Block> blocks = getByCategory(cat);
                        categoryEmpty[cat.ordinal()] = blocks.isEmpty() || !unlocked(blocks.first());
                    }

                    int f = 0;
                    for(Category cat : getCategories()){
                        if(f++ % 2 == 0) categories.row();

                        if(categoryEmpty[cat.ordinal()]){
                            categories.addImage(Styles.black6);
                            continue;
                        }

                        categories.addImageButton(Core.atlas.drawable("icon-" + cat.name() + "-smaller"), Styles.clearToggleTransi, () -> {
                            currentCategory = cat;
                            rebuildCategory.run();
                        }).group(group).update(i -> i.setChecked(currentCategory == cat)).name("category-" + cat.name());
                    }
                }).touchable(Touchable.enabled);

                rebuildCategory.run();
                frame.update(() -> {
                    if(gridUpdate(control.input)) rebuildCategory.run();
                });
            });
        });
    }

    Array<Category> getCategories(){
        returnCatArray.clear();
        returnCatArray.addAll(Category.all);
        returnCatArray.sort((c1, c2) -> Boolean.compare(categoryEmpty[c1.ordinal()], categoryEmpty[c2.ordinal()]));
        return returnCatArray;
    }

    Array<Block> getByCategory(Category cat){
        returnArray.clear();
        for(Block block : content.blocks()){
            if(block.buildCategory == cat && block.isVisible()){
                returnArray.add(block);
            }
        }
        returnArray.sort((b1, b2) -> -Boolean.compare(unlocked(b1), unlocked(b2)));
        return returnArray;
    }

    boolean unlocked(Block block){
        return !world.isZone() || data.isUnlocked(block);
    }

    /** Returns the currently displayed block in the top box. */
    Block getSelected(){
        Block toDisplay = null;

        Vector2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //setup hovering tile
        if(!Core.scene.hasMouse() && topTable.hit(v.x, v.y, false) == null){
            Tile tile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            if(tile != null){
                hoverTile = tile.link();
            }else{
                hoverTile = null;
            }
        }else{
            hoverTile = null;
        }

        //block currently selected
        if(control.input.block != null){
            toDisplay = control.input.block;
        }

        //block hovered on in build menu
        if(hovered != null){
            toDisplay = hovered;
        }

        return toDisplay;
    }

    /** Returns the block currently being hovered over in the world. */
    Block tileDisplayBlock(){
        return hoverTile == null ? null : hoverTile.block().synthetic() ? hoverTile.block() : hoverTile.drop() != null ? hoverTile.overlay().itemDrop != null ? hoverTile.overlay() : hoverTile.floor() : null;
    }
}