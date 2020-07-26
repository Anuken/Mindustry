package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class PlacementFragment extends Fragment{
    final int rowWidth = 4;

    public Category currentCategory = Category.distribution;
    Seq<Block> returnArray = new Seq<>();
    Seq<Category> returnCatArray = new Seq<>();
    boolean[] categoryEmpty = new boolean[Category.all.length];
    ObjectMap<Category,Block> selectedBlocks = new ObjectMap<>();
    ObjectFloatMap<Category> scrollPositions = new ObjectFloatMap<>();
    Block menuHoverBlock;
    Displayable hover;
    Object lastDisplayState;
    boolean wasHovered;
    Table blockTable, toggler, topTable;
    ScrollPane blockPane;
    boolean blockSelectEnd;
    int blockSelectSeq;
    long blockSelectSeqMillis;
    Binding[] blockSelect = {
        Binding.block_select_01,
        Binding.block_select_02,
        Binding.block_select_03,
        Binding.block_select_04,
        Binding.block_select_05,
        Binding.block_select_06,
        Binding.block_select_07,
        Binding.block_select_08,
        Binding.block_select_09,
        Binding.block_select_10,
        Binding.block_select_left,
        Binding.block_select_right,
        Binding.block_select_up,
        Binding.block_select_down
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

        Events.on(ResetEvent.class, event -> {
            selectedBlocks.clear();
        });
    }

    void rebuild(){
        currentCategory = Category.turret;
        Group group = toggler.parent;
        int index = toggler.getZIndex();
        toggler.remove();
        build(group);
        toggler.setZIndex(index);
    }

    boolean gridUpdate(InputHandler input){
        scrollPositions.put(currentCategory, blockPane.getScrollY());

        if(Core.input.keyDown(Binding.pick) && player.isBuilder()){ //mouse eyedropper select
            Building tile = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            Block tryRecipe = tile == null ? null : tile.block();
            Object tryConfig = tile == null ? null : tile.config();

            for(BuildPlan req : player.builder().plans()){
                if(!req.breaking && req.block.bounds(req.x, req.y, Tmp.r1).contains(Core.input.mouseWorld())){
                    tryRecipe = req.block;
                    tryConfig = req.config;
                    break;
                }
            }

            if(tryRecipe != null && tryRecipe.isVisible() && unlocked(tryRecipe)){
                input.block = tryRecipe;
                tryRecipe.lastConfig = tryConfig;
                currentCategory = input.block.category;
                return true;
            }
        }

        if(ui.chatfrag.shown() || Core.scene.hasKeyboard()) return false;

        for(int i = 0; i < blockSelect.length; i++){
            if(Core.input.keyTap(blockSelect[i])){
                if(i > 9) { //select block directionally
                    Seq<Block> blocks = getUnlockedByCategory(currentCategory);
                    Block currentBlock = getSelectedBlock(currentCategory);
                    for(int j = 0; j < blocks.size; j++){
                        if(blocks.get(j) == currentBlock){
                            switch(i){
                                case 10: //left
                                    j = (j - 1 + blocks.size) % blocks.size;
                                    break;
                                case 11: //right
                                    j = (j + 1) % blocks.size;
                                    break;
                                case 12: //up
                                    j = (j > 3 ? j - 4 : blocks.size - blocks.size % 4 + j);
                                    j -= (j < blocks.size ? 0 : 4);
                                    break;
                                case 13: //down
                                    j = (j < blocks.size - 4 ? j + 4 : j % 4);
                            }
                            input.block = blocks.get(j);
                            selectedBlocks.put(currentCategory, input.block);
                            break;
                        }
                    }
                }else if(blockSelectEnd || Time.timeSinceMillis(blockSelectSeqMillis) > Core.settings.getInt("blockselecttimeout")){ //1st number of combo, select category
                    //select only visible categories
                    if(!getUnlockedByCategory(Category.all[i]).isEmpty()){
                        currentCategory = Category.all[i];
                        if(input.block != null){
                            input.block = getSelectedBlock(currentCategory);
                        }
                        blockSelectEnd = false;
                        blockSelectSeq = 0;
                        blockSelectSeqMillis = Time.millis();
                    }
                }else{ //select block
                    if(blockSelectSeq == 0){ //2nd number of combo
                        blockSelectSeq = i + 1;
                    }else{ //3rd number of combo
                        //entering "X,1,0" selects the same block as "X,0"
                        i += (blockSelectSeq - (i != 9 ? 0 : 1)) * 10;
                        blockSelectEnd = true;
                    }
                    Seq<Block> blocks = getByCategory(currentCategory);
                    if(!unlocked(blocks.get(i))) return true;
                    input.block = (i < blocks.size) ? blocks.get(i) : null;
                    selectedBlocks.put(currentCategory, input.block);
                    blockSelectSeqMillis = Time.millis();
                }
                return true;
            }
        }

        if(Core.input.keyTap(Binding.category_prev)){
            do{
                currentCategory = currentCategory.prev();
            }while(categoryEmpty[currentCategory.ordinal()]);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        if(Core.input.keyTap(Binding.category_next)){
            do{
                currentCategory = currentCategory.next();
            }while(categoryEmpty[currentCategory.ordinal()]);
            input.block = getSelectedBlock(currentCategory);
            return true;
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

                    for(Block block : getUnlockedByCategory(currentCategory)){
                        if(!unlocked(block)) continue;
                        if(index++ % rowWidth == 0){
                            blockTable.row();
                        }

                        ImageButton button = blockTable.button(new TextureRegionDrawable(block.icon(Cicon.medium)), Styles.selecti, () -> {
                            if(unlocked(block)){
                                if(Core.input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(block.name) != 0){
                                    Core.app.setClipboardText((char)Fonts.getUnicode(block.name) + "");
                                    ui.showInfoFade("$copied");
                                }else{
                                    control.input.block = control.input.block == block ? null : block;
                                    selectedBlocks.put(currentCategory, control.input.block);
                                }
                            }
                        }).size(46f).group(group).name("block-" + block.name).get();
                        button.resizeImage(Cicon.medium.size);

                        button.update(() -> { //color unplacable things gray
                            Building core = player.core();
                            Color color = (state.rules.infiniteResources || (core != null && (core.items.has(block.requirements, state.rules.buildCostMultiplier) || state.rules.infiniteResources))) && player.isBuilder() ? Color.white : Color.gray;
                            button.forEach(elem -> elem.setColor(color));
                            button.setChecked(control.input.block == block);

                            if(!block.isPlaceable()){
                                button.forEach(elem -> elem.setColor(Color.darkGray));
                            }
                        });

                        button.hovered(() -> menuHoverBlock = block);
                        button.exited(() -> {
                            if(menuHoverBlock == block){
                                menuHoverBlock = null;
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
                    blockPane.setScrollYForce(scrollPositions.get(currentCategory, 0));
                    Core.app.post(() -> {
                        blockPane.setScrollYForce(scrollPositions.get(currentCategory, 0));
                        blockPane.act(0f);
                        blockPane.layout();
                    });
                };

                //top table with hover info
                frame.table(Tex.buttonEdge2,top -> {
                    topTable = top;
                    top.add(new Table()).growX().update(topTable -> {

                        //find current hovered thing
                        Displayable hovered = hover;
                        Block displayBlock = menuHoverBlock != null ? menuHoverBlock : control.input.block;
                        Object displayState = displayBlock != null ? displayBlock : hovered;
                        boolean isHovered = displayBlock == null; //use hovered thing if displayblock is null

                        //don't refresh unnecessarily
                        //refresh only when the hover state changes, or the displayed block changes
                        if(wasHovered == isHovered && lastDisplayState == displayState) return;

                        topTable.clear();
                        topTable.top().left().margin(5);

                        lastDisplayState = displayState;
                        wasHovered = isHovered;

                        //show details of selected block, with costs
                        if(displayBlock != null){

                            topTable.table(header -> {
                                String keyCombo = "";
                                if(!mobile && Core.settings.getBool("blockselectkeys")){
                                    Seq<Block> blocks = getByCategory(currentCategory);
                                    for(int i = 0; i < blocks.size; i++){
                                        if(blocks.get(i) == displayBlock && (i + 1) / 10 - 1 < blockSelect.length){
                                            keyCombo = Core.bundle.format("placement.blockselectkeys", Core.keybinds.get(blockSelect[currentCategory.ordinal()]).key.toString())
                                                + (i < 10 ? "" : Core.keybinds.get(blockSelect[(i + 1) / 10 - 1]).key.toString() + ",")
                                                + Core.keybinds.get(blockSelect[i % 10]).key.toString() + "]";
                                            break;
                                        }
                                    }
                                }
                                final String keyComboFinal = keyCombo;
                                header.left();
                                header.add(new Image(displayBlock.icon(Cicon.medium))).size(8 * 4);
                                header.labelWrap(() -> !unlocked(displayBlock) ? Core.bundle.get("block.unknown") : displayBlock.localizedName + keyComboFinal)
                                .left().width(190f).padLeft(5);
                                header.add().growX();
                                if(unlocked(displayBlock)){
                                    header.button("?", Styles.clearPartialt, () -> {
                                        ui.content.show(displayBlock);
                                        Events.fire(new BlockInfoEvent());
                                    }).size(8 * 5).padTop(-5).padRight(-5).right().grow().name("blockinfo");
                                }
                            }).growX().left();
                            topTable.row();
                            //add requirement table
                            topTable.table(req -> {
                                req.top().left();

                                for(ItemStack stack : displayBlock.requirements){
                                    req.table(line -> {
                                        line.left();
                                        line.image(stack.item.icon(Cicon.small)).size(8 * 2);
                                        line.add(stack.item.localizedName).maxWidth(140f).fillX().color(Color.lightGray).padLeft(2).left().get().setEllipsis(true);
                                        line.labelWrap(() -> {
                                            Building core = player.core();
                                            if(core == null || state.rules.infiniteResources) return "*/*";

                                            int amount = core.items.get(stack.item);
                                            int stackamount = Math.round(stack.amount * state.rules.buildCostMultiplier);
                                            String color = (amount < stackamount / 2f ? "[red]" : amount < stackamount ? "[accent]" : "[white]");

                                            return color + UI.formatAmount(amount) + "[white]/" + stackamount;
                                        }).padLeft(5);
                                    }).left();
                                    req.row();
                                }
                            }).growX().left().margin(3);

                            if(!displayBlock.isPlaceable() || !player.isBuilder()){
                                topTable.row();
                                topTable.table(b -> {
                                    b.image(Icon.cancel).padRight(2).color(Color.scarlet);
                                    b.add(!player.isBuilder() ? "$unit.nobuild" : displayBlock.unplaceableMessage()).width(190f).wrap();
                                    b.left();
                                }).padTop(2).left();
                            }

                        }else if(hovered != null){
                            //show hovered item, whatever that may be
                            hovered.display(topTable);
                        }
                    });
                }).colspan(3).fillX().visible(this::hasInfoBox).touchable(Touchable.enabled);
                frame.row();
                frame.image().color(Pal.gray).colspan(3).height(4).growX();
                frame.row();
                frame.table(Tex.pane2, blocksSelect -> {
                    blocksSelect.margin(4).marginTop(0);
                    blockPane = blocksSelect.pane(blocks -> blockTable = blocks).height(194f).update(pane -> {
                        if(pane.hasScroll()){
                            Element result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                            if(result == null || !result.isDescendantOf(pane)){
                                Core.scene.setScrollFocus(null);
                            }
                        }
                    }).grow().get();
                    blockPane.setStyle(Styles.smallPane);
                    blocksSelect.row();
                    blocksSelect.table(control.input::buildPlacementUI).name("inputTable").growX();
                }).fillY().bottom().touchable(Touchable.enabled);
                frame.table(categories -> {
                    categories.bottom();
                    categories.add(new Image(Styles.black6){
                        @Override
                        public void draw(){
                            if(height <= Scl.scl(3f)) return;
                            getDrawable().draw(x, y, width, height - Scl.scl(3f));
                        }
                    }).colspan(2).growX().growY().padTop(-3f).row();
                    categories.defaults().size(50f);

                    ButtonGroup<ImageButton> group = new ButtonGroup<>();

                    //update category empty values
                    for(Category cat : Category.all){
                        Seq<Block> blocks = getUnlockedByCategory(cat);
                        categoryEmpty[cat.ordinal()] = blocks.isEmpty();
                    }

                    int f = 0;
                    for(Category cat : getCategories()){
                        if(f++ % 2 == 0) categories.row();

                        if(categoryEmpty[cat.ordinal()]){
                            categories.image(Styles.black6);
                            continue;
                        }

                        categories.button(ui.getIcon(cat.name()), Styles.clearToggleTransi, () -> {
                            currentCategory = cat;
                            if(control.input.block != null){
                                control.input.block = getSelectedBlock(currentCategory);
                            }
                            rebuildCategory.run();
                        }).group(group).update(i -> i.setChecked(currentCategory == cat)).name("category-" + cat.name());
                    }
                }).fillY().bottom().touchable(Touchable.enabled);

                rebuildCategory.run();
                frame.update(() -> {
                    if(gridUpdate(control.input)) rebuildCategory.run();
                });
            });
        });
    }

    Seq<Category> getCategories(){
        return returnCatArray.clear().addAll(Category.all).sort((c1, c2) -> Boolean.compare(categoryEmpty[c1.ordinal()], categoryEmpty[c2.ordinal()]));
    }

    Seq<Block> getByCategory(Category cat){
        return returnArray.selectFrom(content.blocks(), block -> block.category == cat && block.isVisible());
    }

    Seq<Block> getUnlockedByCategory(Category cat){
        return returnArray.selectFrom(content.blocks(), block -> block.category == cat && block.isVisible() && unlocked(block)).sort((b1, b2) -> Boolean.compare(!b1.isPlaceable(), !b2.isPlaceable()));
    }

    Block getSelectedBlock(Category cat){
        return selectedBlocks.get(cat, () -> getByCategory(cat).find(this::unlocked));
    }

    boolean unlocked(Block block){
        return block.unlockedNow();
    }

    boolean hasInfoBox(){
        hover = hovered();
        return control.input.block != null || menuHoverBlock != null || hover != null;
    }

    /** Returns the thing being hovered over. */
    @Nullable
    Displayable hovered(){
        Vec2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse() || topTable.hit(v.x, v.y, false) != null) return null;

        //check for a unit
        Unit unit = Units.closestOverlap(player.team(), Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> !u.isLocal());
        //if cursor has a unit, display it
        if(unit != null) return unit;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            if(hoverTile.build != null){
                hoverTile.build.updateFlow = true;
                return hoverTile.build;
            }

            //if the tile has a drop, display the drop
            if(hoverTile.drop() != null){
                return hoverTile;
            }
        }

        return null;
    }
}
