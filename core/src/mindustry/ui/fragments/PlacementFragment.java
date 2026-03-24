package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class PlacementFragment{
    final int rowWidth = 4;

    public Category currentCategory = Category.distribution;

    Seq<Block> returnArray = new Seq<>(), returnArray2 = new Seq<>();
    Seq<Category> returnCatArray = new Seq<>();
    boolean[] categoryEmpty = new boolean[Category.all.length];
    ObjectMap<Category,Block> selectedBlocks = new ObjectMap<>();
    ObjectFloatMap<Category> scrollPositions = new ObjectFloatMap<>();
    @Nullable Block menuHoverBlock;
    @Nullable Displayable hover;
    @Nullable Building lastFlowBuild, nextFlowBuild;
    @Nullable Object lastDisplayState;
    @Nullable Team lastTeam;
    boolean wasHovered;
    Table blockTable, toggler, topTable, blockCatTable, commandTable;
    Stack mainStack;
    ScrollPane blockPane;
    Runnable rebuildCommand;
    boolean blockSelectEnd, wasCommandMode;
    int blockSelectSeq;
    long blockSelectSeqMillis;
    KeyBind[] blockSelect = {
        Binding.blockSelect01,
        Binding.blockSelect02,
        Binding.blockSelect03,
        Binding.blockSelect04,
        Binding.blockSelect05,
        Binding.blockSelect06,
        Binding.blockSelect07,
        Binding.blockSelect08,
        Binding.blockSelect09,
        Binding.blockSelect10,
        Binding.blockSelectLeft,
        Binding.blockSelectRight,
        Binding.blockSelectUp,
        Binding.blockSelectDown
    };

    public PlacementFragment(){
        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> {
                currentCategory = Category.distribution;
                control.input.block = null;
                rebuild();
            });
        });

        Events.run(Trigger.unitCommandChange, () -> {
            if(rebuildCommand != null){
                rebuildCommand.run();
            }
        });

        Events.on(UnlockEvent.class, event -> {
            if(event.content instanceof Block){
                rebuild();
            }
        });

        Events.on(ResetEvent.class, event -> {
            selectedBlocks.clear();
        });

        Events.run(Trigger.update, () -> {
            //disable flow updating on previous building so it doesn't waste CPU
            if(lastFlowBuild != null && lastFlowBuild != nextFlowBuild){
                if(lastFlowBuild.flowItems() != null) lastFlowBuild.flowItems().stopFlow();
                if(lastFlowBuild.liquids != null) lastFlowBuild.liquids.stopFlow();
            }

            lastFlowBuild = nextFlowBuild;

            if(nextFlowBuild != null){
                if(nextFlowBuild.flowItems() != null) nextFlowBuild.flowItems().updateFlow();
                if(nextFlowBuild.liquids != null) nextFlowBuild.liquids.updateFlow();
            }
        });
    }

    public Displayable hover(){
        return hover;
    }

    public void rebuild(){
        //category does not change on rebuild anymore, only on new world load
        Group group = toggler.parent;
        int index = toggler.getZIndex();
        toggler.remove();
        build(group);
        toggler.setZIndex(index);
    }

    boolean updatePick(InputHandler input){
        Tile tile = world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
        if(tile != null && Core.input.keyTap(Binding.pick) && player.isBuilder() && !Core.scene.hasDialog()){ //mouse eyedropper select
            var build = tile.build;

            //can't middle click buildings in fog
            if(build != null && build.inFogTo(player.team())){
                build = null;
            }

            Block tryBlock = build == null ? null : build instanceof ConstructBuild c ? c.current : build.block;
            Object tryConfig = build == null || !build.block.copyConfig ? null : build.config();

            for(BuildPlan req : player.unit().plans()){
                if(!req.breaking && req.block.bounds(req.x, req.y, Tmp.r1).contains(Core.input.mouseWorld())){
                    tryBlock = req.block;
                    tryConfig = req.config;
                    break;
                }
            }

            if(tryBlock == null && state.rules.editor){
                tryBlock =
                    tile.block() != Blocks.air ? tile.block() :
                    tile.overlay() != Blocks.air ? tile.overlay() :
                    tile.floor() != Blocks.air ? tile.floor() : null;
            }

            if(tryBlock != null && build == null && tryConfig == null){
                tryConfig = tryBlock.getConfig(tile);
            }

            if(tryBlock != null && ((tryBlock.isVisible() && unlocked(tryBlock)) || state.rules.editor)){
                input.block = tryBlock;
                tryBlock.lastConfig = tryConfig;
                if(tryBlock.isVisible()){
                    currentCategory = input.block.category;
                }
                tryBlock.onPicked(tile);
                return true;
            }
        }
        return false;
    }

    boolean gridUpdate(InputHandler input){
        scrollPositions.put(currentCategory, blockPane.getScrollY());

        if(updatePick(input)){
            return true;
        }

        if(ui.chatfrag.shown() || ui.consolefrag.shown() || Core.scene.hasKeyboard()) return false;

        for(int i = 0; i < blockSelect.length; i++){
            if(Core.input.keyTap(blockSelect[i])){
                if(i > 9){ //select block directionally
                    Seq<Block> blocks = getUnlockedByCategory(currentCategory);
                    Block currentBlock = getSelectedBlock(currentCategory);
                    for(int j = 0; j < blocks.size; j++){
                        if(blocks.get(j) == currentBlock){
                            switch(i){
                                //left
                                case 10 -> j = (j - 1 + blocks.size) % blocks.size;
                                //right
                                case 11 -> j = (j + 1) % blocks.size;
                                //up
                                case 12 -> {
                                    j = (j > 3 ? j - 4 : blocks.size - blocks.size % 4 + j);
                                    j -= (j < blocks.size ? 0 : 4);
                                }
                                //down
                                case 13 -> j = (j < blocks.size - 4 ? j + 4 : j % 4);
                            }
                            input.block = blocks.get(j);
                            selectedBlocks.put(currentCategory, input.block);
                            break;
                        }
                    }
                }else if(blockSelectEnd || Time.timeSinceMillis(blockSelectSeqMillis) > 400){ //1st number of combo, select category
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
                    if(i >= blocks.size || !unlocked(blocks.get(i))) return true;
                    input.block = (i < blocks.size) ? blocks.get(i) : null;
                    selectedBlocks.put(currentCategory, input.block);
                    blockSelectSeqMillis = Time.millis();
                }
                return true;
            }
        }

        if(Core.input.keyTap(Binding.categoryPrev)){
            int i = 0;
            do{
                currentCategory = currentCategory.prev();
                i ++;
            }while(categoryEmpty[currentCategory.ordinal()] && i < categoryEmpty.length);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        if(Core.input.keyTap(Binding.categoryNext)){
            int i = 0;
            do{
                currentCategory = currentCategory.next();
                i ++;
            }while(categoryEmpty[currentCategory.ordinal()] && i < categoryEmpty.length);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        if(Core.input.keyTap(Binding.blockInfo)){
            if(hovered() instanceof Unit unit && unit.type.unlockedNow()){
                ui.content.show(unit.type());
            }else{
                var build = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
                Block hovering = build == null ? null : build instanceof ConstructBuild c ? c.current : build.block;
                Block displayBlock = menuHoverBlock != null ? menuHoverBlock : input.block != null ? input.block : hovering;
                if(displayBlock != null && displayBlock.unlockedNow()){
                    ui.content.show(displayBlock);
                    Events.fire(new BlockInfoEvent());
                }
            }
        }

        return false;
    }

    public void build(Group parent){
        parent.fill(full -> {
            toggler = full;
            full.bottom().right().visible(() -> ui.hudfrag.shown);

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

                        ImageButton button = blockTable.button(new TextureRegionDrawable(block.uiIcon), Styles.selecti, () -> {
                            if(unlocked(block)){
                                if((Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.controlLeft)) && Fonts.getUnicode(block.name) != 0){
                                    Core.app.setClipboardText((char)Fonts.getUnicode(block.name) + "");
                                    ui.showInfoFade("@copied");
                                }else{
                                    control.input.block = control.input.block == block ? null : block;
                                    selectedBlocks.put(currentCategory, control.input.block);
                                }
                            }
                        }).size(46f).group(group).name("block-" + block.name).get();
                        button.resizeImage(iconMed);

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
                frame.table(Tex.buttonEdge2, top -> {
                    topTable = top;
                    top.add(new Table()).growX().update(topTable -> {

                        //find current hovered thing
                        Displayable hovered = hover;
                        Block displayBlock = menuHoverBlock != null ? menuHoverBlock : control.input.block;
                        Object displayState = displayBlock != null ? displayBlock : hovered;
                        boolean isHovered = displayBlock == null; //use hovered thing if displayblock is null

                        //don't refresh unnecessarily
                        //refresh only when the hover state changes, or the displayed block changes
                        if(wasHovered == isHovered && lastDisplayState == displayState && lastTeam == player.team()) return;

                        topTable.clear();
                        topTable.top().left().margin(5);

                        lastDisplayState = displayState;
                        wasHovered = isHovered;
                        lastTeam = player.team();

                        //show details of selected block, with costs
                        if(displayBlock != null){

                            topTable.table(header -> {
                                String keyCombo = "";
                                if(!mobile){
                                    Seq<Block> blocks = getByCategory(currentCategory);
                                    for(int i = 0; i < blocks.size; i++){
                                        if(blocks.get(i) == displayBlock && (i + 1) / 10 - 1 < blockSelect.length){
                                            keyCombo = Core.bundle.format("placement.blockselectkeys", blockSelect[currentCategory.ordinal()].value.key.toString())
                                                + (i < 10 ? "" : blockSelect[(i + 1) / 10 - 1].value.key.toString() + ",")
                                                + blockSelect[i % 10].value.key.toString() + "]";
                                            break;
                                        }
                                    }
                                }
                                String keyComboFinal = keyCombo;
                                header.left();
                                header.add(new Image(displayBlock.uiIcon)).scaling(Scaling.fit).size(8 * 4);
                                header.labelWrap(() -> !unlocked(displayBlock) ? Core.bundle.get("block.unknown") : displayBlock.localizedName + keyComboFinal)
                                .left().width(190f).padLeft(5);
                                header.add().growX();
                                if(unlocked(displayBlock)){
                                    header.button("?", Styles.flatBordert, () -> {
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
                                        line.image(stack.item.uiIcon).size(8 * 2);
                                        line.add(stack.item.localizedName).maxWidth(140f).fillX().color(Color.lightGray).padLeft(2).left().get().setEllipsis(true);
                                        line.labelWrap(() -> {
                                            Building core = player.core();
                                            int stackamount = Math.round(stack.amount * state.rules.buildCostMultiplier);
                                            if(core == null || state.rules.infiniteResources) return "*/" + stackamount;

                                            int amount = core.items.get(stack.item);
                                            String color = (amount < stackamount / 2f ? "[scarlet]" : amount < stackamount ? "[accent]" : "[white]");

                                            return color + UI.formatAmount(amount) + "[white]/" + stackamount;
                                        }).padLeft(5);
                                    }).left();
                                    req.row();
                                }
                            }).growX().left().margin(3);

                            if((!displayBlock.isPlaceable() || !player.isBuilder()) && !state.rules.editor){
                                topTable.row();
                                topTable.table(b -> {
                                    b.image(Icon.cancel).padRight(2).color(Color.scarlet);
                                    b.add(!player.isBuilder() ? "@unit.nobuild" : !displayBlock.supportsEnv(state.rules.env) ? "@unsupported.environment" : "@banned").width(190f).wrap();
                                    b.left();
                                }).padTop(2).left();
                            }

                        }else if(hovered != null){
                            //show hovered item, whatever that may be
                            hovered.display(topTable);
                        }
                    });
                }).colspan(3).fillX().visible(this::hasInfoBox).touchable(Touchable.enabled).row();

                frame.image().color(Pal.gray).colspan(3).height(4).growX().row();

                blockCatTable = new Table();
                commandTable = new Table(Tex.pane2);
                mainStack = new Stack();

                mainStack.update(() -> {
                    if(control.input.commandMode != wasCommandMode){
                        mainStack.clearChildren();
                        mainStack.addChild(control.input.commandMode ? commandTable : blockCatTable);

                        //hacky, but forces command table to be same width as blocks
                        if(control.input.commandMode){
                            commandTable.getCells().peek().width(blockCatTable.getWidth() / Scl.scl(1f));
                        }

                        wasCommandMode = control.input.commandMode;
                    }
                });

                frame.add(mainStack).colspan(3).fill();

                frame.row();

                //for better inset visuals at the bottom
                frame.rect((x, y, w, h) -> {
                    if(Core.scene.marginBottom > 0){
                        Tex.paneLeft.draw(x, 0, w, y);
                    }
                }).colspan(3).fillX().row();

                //commandTable: commanded units
                {
                    commandTable.touchable = Touchable.enabled;
                    commandTable.add(Core.bundle.get("commandmode.name")).fill().center().labelAlign(Align.center).row();
                    commandTable.image().color(Pal.accent).growX().pad(20f).padTop(0f).padBottom(4f).row();
                    commandTable.table(u -> {

                        Bits activeCommands = new Bits(content.unitCommands().size);
                        Bits activeStances = new Bits(content.unitStances().size);

                        Bits availableCommands = new Bits(content.unitCommands().size);
                        Bits availableStances = new Bits(content.unitStances().size);
                        Bits activeTypes = new Bits(content.units().size), prevActiveTypes = new Bits(content.units().size);

                        u.left();
                        Bits usedCommands = new Bits(content.unitCommands().size);
                        var commands = new Seq<UnitCommand>();

                        Bits usedStances = new Bits(content.unitStances().size);
                        var stances = new Seq<UnitStance>();
                        var stancesOut = new Seq<UnitStance>();

                        int[] counts = new int[content.units().size];

                        rebuildCommand = () -> {
                            u.clearChildren();
                            var units = control.input.selectedUnits;
                            if(units.size > 0){
                                usedCommands.clear();
                                usedStances.clear();
                                commands.clear();
                                stances.clear();
                                Arrays.fill(counts, 0);

                                for(var unit : units){
                                    counts[unit.type.id] ++;

                                    stancesOut.clear();
                                    unit.type.getUnitStances(unit, stancesOut);

                                    for(var stance : stancesOut){
                                        if(!usedStances.get(stance.id)){
                                            stances.add(stance);
                                            usedStances.set(stance.id);
                                        }
                                    }
                                }

                                Table unitlist = u.table().growX().left().get();
                                unitlist.left();

                                int col = 0;
                                for(int i = 0; i < counts.length; i++){
                                    int fi = i;
                                    if(counts[i] > 0){
                                        var type = content.unit(i);
                                        unitlist.add(StatValues.stack(type, counts[i])).pad(4).with(b -> {
                                            b.clearListeners();
                                            b.addListener(Tooltips.getInstance().create(type.localizedName, false));

                                            Label amountLabel = b.find("stack amount");
                                            if(amountLabel != null){
                                                amountLabel.setText(() -> counts[fi] + "");
                                            }

                                            var listener = new ClickListener();

                                            //left click -> select
                                            b.clicked(KeyCode.mouseLeft, () -> {
                                                control.input.selectedUnits.removeAll(unit -> unit.type != type);
                                                Events.fire(Trigger.unitCommandChange);
                                            });
                                            //right click -> remove
                                            b.clicked(KeyCode.mouseRight, () -> {
                                                control.input.selectedUnits.removeAll(unit -> unit.type == type);
                                                Events.fire(Trigger.unitCommandChange);
                                            });

                                            b.addListener(listener);
                                            b.addListener(new HandCursorListener());
                                            //gray on hover
                                            b.update(() -> ((Group)b.getChildren().first()).getChildren().first().setColor(listener.isOver() ? Color.lightGray : Color.white));
                                        });

                                        if(++col % 7 == 0){
                                            unitlist.row();
                                        }

                                        for(var command : type.commands){
                                            if(!usedCommands.get(command.id)){
                                                commands.add(command);
                                                usedCommands.set(command.id);
                                            }
                                        }
                                    }
                                }

                                //list commands
                                if(commands.size > 1){
                                    u.row();

                                    u.table(coms -> {
                                        coms.left();
                                        int scol = 0;
                                        for(var command : commands){
                                            coms.button(Icon.icons.get(command.icon, Icon.cancel), Styles.clearNoneTogglei, () -> {
                                                Call.setUnitCommand(player, units.mapInt(un -> un.id, un -> un.type.allowCommand(un, command)).toArray(), command);
                                            }).checked(i -> activeCommands.get(command.id)).size(50f).tooltip(command.localized(), true);

                                            if(++scol % 6 == 0) coms.row();
                                        }

                                    }).fillX().padTop(4f).left();
                                }

                                //list stances
                                if(stances.size > 1){
                                    u.row();

                                    if(commands.size > 1){
                                        u.add(new Image(Tex.whiteui)).height(3f).color(Pal.gray).pad(7f).growX().row();
                                    }

                                    u.table(coms -> {
                                        coms.left();
                                        int scol = 0;
                                        for(var stance : stances){

                                            coms.button(stance.getIcon(), Styles.clearNoneTogglei, () -> {
                                                Call.setUnitStance(player, units.mapInt(un -> un.id, un -> un.type.allowStance(un, stance)).toArray(), stance, !activeStances.get(stance.id));
                                            }).checked(i -> activeStances.get(stance.id)).size(50f).tooltip(stance.localized(), true);

                                            if(++scol % 6 == 0) coms.row();
                                        }
                                    }).fillX().padTop(4f).left();
                                }
                            }else{
                                u.add(Core.bundle.get("commandmode.nounits")).color(Color.lightGray).growX().center().labelAlign(Align.center).pad(6);
                            }
                        };

                        u.update(() -> {
                            {
                                activeCommands.clear();
                                activeStances.clear();
                                availableCommands.clear();
                                availableStances.clear();
                                activeTypes.clear();

                                Arrays.fill(counts, 0);

                                //find the command that all units have, or null if they do not share one
                                for(var unit : control.input.selectedUnits){
                                    if(unit.controller() instanceof CommandAI cmd){
                                        activeCommands.set(cmd.command.id);
                                        activeStances.set(cmd.stances);
                                    }

                                    counts[unit.type.id] ++;

                                    activeTypes.set(unit.type.id);

                                    stancesOut.clear();
                                    unit.type.getUnitStances(unit, stancesOut);

                                    for(var stance : stancesOut){
                                        availableStances.set(stance.id);
                                    }

                                    for(var command : unit.type.commands){
                                        availableCommands.set(command.id);
                                    }
                                }

                                if(!usedCommands.equals(availableCommands) || !usedStances.equals(availableStances) || !prevActiveTypes.equals(activeTypes)){
                                    rebuildCommand.run();
                                    prevActiveTypes.set(activeTypes);
                                }

                                //not a huge fan of running input logic here, but it's convenient as the stance arrays are all here...
                                for(UnitStance stance : stances){
                                    //first stance must always be the stop stance
                                    if(stance.keybind != null && Core.input.keyTap(stance.keybind)){
                                        Call.setUnitStance(player, control.input.selectedUnits.mapInt(un -> un.id, un -> un.type.allowStance(un, stance)).toArray(), stance, !activeStances.get(stance.id));
                                    }
                                }

                                for(UnitCommand command : commands){
                                    //first stance must always be the stop stance
                                    if(command.keybind != null && Core.input.keyTap(command.keybind)){
                                        Call.setUnitCommand(player, control.input.selectedUnits.mapInt(un -> un.id, un -> un.type.allowCommand(un, command)).toArray(), command);
                                    }
                                }
                            }
                        });
                        rebuildCommand.run();
                    }).grow();
                }

                //blockCatTable: all blocks | all categories
                {
                    blockCatTable.table(Tex.pane2, blocksSelect -> {
                        blocksSelect.margin(4).marginTop(0);
                        blockPane = blocksSelect.pane(blocks -> blockTable = blocks).height(194f).update(pane -> {
                            if(pane.hasScroll()){
                                Element result = Core.scene.getHoverElement();
                                if(result == null || !result.isDescendantOf(pane)){
                                    Core.scene.setScrollFocus(null);
                                }
                            }
                        }).grow().get();
                        blockPane.setStyle(Styles.smallPane);
                        blocksSelect.row();
                        blocksSelect.table(t -> {
                            t.image().color(Pal.gray).height(4f).colspan(4).growX();
                            t.row();
                            control.input.buildPlacementUI(t);
                        }).name("inputTable").growX();
                    }).fillY().bottom().touchable(Touchable.enabled);
                    blockCatTable.table(categories -> {
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

                        boolean needsAssign = categoryEmpty[currentCategory.ordinal()];

                        int f = 0;
                        for(Category cat : getCategories()){
                            if(f++ % 2 == 0) categories.row();

                            if(categoryEmpty[cat.ordinal()]){
                                categories.image(Styles.black6);
                                continue;
                            }

                            if(needsAssign){
                                currentCategory = cat;
                                needsAssign = false;
                            }

                            categories.button(ui.getIcon(cat.name()), Styles.clearTogglei, () -> {
                                currentCategory = cat;
                                if(control.input.block != null){
                                    control.input.block = getSelectedBlock(currentCategory);
                                }
                                rebuildCategory.run();
                            }).group(group).update(i -> i.setChecked(currentCategory == cat)).name("category-" + cat.name());
                        }
                    }).fillY().bottom().touchable(Touchable.enabled);
                }

                mainStack.add(blockCatTable);

                rebuildCategory.run();
                frame.update(() -> {
                    if(!control.input.commandMode && gridUpdate(control.input)){
                        rebuildCategory.run();
                    }
                });
            });
        });
    }

    Seq<Category> getCategories(){
        return returnCatArray.clear().addAll(Category.all).sort((c1, c2) -> Boolean.compare(categoryEmpty[c1.ordinal()], categoryEmpty[c2.ordinal()]));
    }

    Seq<Block> getByCategory(Category cat){
        return returnArray.selectFrom(content.blocks(), block -> block.category == cat && block.isVisible() && block.environmentBuildable());
    }

    Seq<Block> getUnlockedByCategory(Category cat){
        return returnArray2.selectFrom(content.blocks(), block -> block.category == cat && block.isVisible() && unlocked(block)).sort((b1, b2) -> Boolean.compare(!b1.isPlaceable(), !b2.isPlaceable()));
    }

    Block getSelectedBlock(Category cat){
        return selectedBlocks.get(cat, () -> getByCategory(cat).find(this::unlocked));
    }

    boolean unlocked(Block block){
        return block.unlockedNowHost() && block.placeablePlayer && block.environmentBuildable() &&
            block.supportsEnv(state.rules.env);
    }

    boolean hasInfoBox(){
        hover = hovered();
        return control.input.block != null || menuHoverBlock != null || hover != null;
    }

    /** @return the thing being hovered over. */
    public @Nullable Displayable hovered(){
        Vec2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse(Core.input.mouseX(), Core.input.mouseY()) || topTable.hit(v.x, v.y, false) != null) return null;

        //check for a unit
        Unit unit = Units.closestOverlap(player.team(), Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> !u.isLocal() && u.displayable());
        //if cursor has a unit, display it
        if(unit != null) return unit;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            if(hoverTile.build != null && hoverTile.build.displayable() && !hoverTile.build.inFogTo(player.team())){
                return nextFlowBuild = hoverTile.build;
            }

            //if the tile has a drop, display the drop
            if((hoverTile.drop() != null && hoverTile.block() == Blocks.air) || hoverTile.wallDrop() != null || hoverTile.floor().liquidDrop != null){
                return hoverTile;
            }
        }

        return null;
    }
}
