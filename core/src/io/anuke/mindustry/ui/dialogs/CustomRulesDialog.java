package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class CustomRulesDialog extends FloatingDialog{
    private Table main;
    private Rules rules;
    private Prov<Rules> resetter;
    private LoadoutDialog loadoutDialog;
    private FloatingDialog allowanceDialog;

    public CustomRulesDialog(){
        super("$mode.custom");

        loadoutDialog = new LoadoutDialog();

        allowanceDialog = new FloatingDialog("$allowance");
        allowanceDialog.addCloseButton();

        allowanceDialog.shown(this::rebuildAllowance);
        allowanceDialog.buttons.addImageTextButton("$addall", Icon.arrow16Small, () -> {
            for(Block block : content.blocks().select(Block::isBuildable)){
                rules.allowance.put(block, 0);
            }
            rebuildAllowance();
        }).size(180, 64f);

        allowanceDialog.buttons.addImageTextButton("$clear", Icon.trash16Small, () -> {
            rules.allowance.clear();
            rebuildAllowance();
        }).size(180, 64f);

        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    private void rebuildAllowance(){

        // todo: someday remove backwards compatibility
        for(Block banned : rules.bannedBlocks){
            rules.allowance.put(banned, 0);
            rules.bannedBlocks.remove(banned);
        }

        float previousScroll = allowanceDialog.cont.getChildren().isEmpty() ? 0f : ((ScrollPane)allowanceDialog.cont.getChildren().first()).getScrollY();
        allowanceDialog.cont.clear();
        allowanceDialog.cont.pane(t -> {
            t.margin(10f);

            if(rules.allowance.isEmpty()){
                t.add("$empty");
            }

            Array<Block> array = Array.with(rules.allowance.keys());
            array.sort();

            int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
            int i = 0;
            float bsize = 40f;

            for(Block block : array){
                t.table(Tex.pane, b -> {
                    b.margin(4).marginRight(8).left();
                    b.addButton("-", Styles.cleart, () -> {
                        rules.allowance.put(block, Math.max(rules.allowance.get(block) - allowanceStep(rules.allowance.get(block)), 0));
                        rebuildAllowance();
                    }).size(bsize);

                    b.addButton("+", Styles.cleart, () -> {
                        rules.allowance.put(block, Math.min(rules.allowance.get(block) + allowanceStep(rules.allowance.get(block)), 10000));
                        rebuildAllowance();
                    }).size(bsize);

                    b.addImageButton(Icon.pencilSmaller, Styles.cleari, () -> ui.showTextInput("$allowance", block.localizedName, 10, rules.allowance.get(block) + "", true, str -> {
                        if(Strings.canParsePostiveInt(str)){
                            int amount = Strings.parseInt(str);
                            if(amount >= 0 && amount <= 10000){
                                rules.allowance.put(block, amount);
                                rebuildAllowance();
                                return;
                            }
                        }
                        ui.showInfo(Core.bundle.format("configure.invalid", 10000));
                    })).size(bsize);

                    b.addImage(block.icon(Cicon.small)).size(8 * 3).padRight(4).padLeft(4);
                    b.label(() -> rules.allowance.get(block) + "").left().width(90f);

                    b.addImageButton(Icon.cancelSmall, Styles.clearPartiali, () -> {
                        rules.allowance.remove(block);
                        rebuildAllowance();
                    }).size(70f).pad(-4f).padLeft(0f);
                }).pad(2).left().fillX();

                if(++i % cols == 0){
                    t.row();
                }
            }
        }).get().setScrollYForce(previousScroll);
        allowanceDialog.cont.row();
        allowanceDialog.cont.addImageTextButton("$add", Icon.addSmall, () -> {
            FloatingDialog dialog = new FloatingDialog("$add");
            dialog.cont.pane(t -> {
                t.left().margin(14f);
                int[] i = {0};
                content.blocks().each(b -> !rules.allowance.containsKey(b) && b.isBuildable(), b -> {
                    int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                    t.addImageButton(new TextureRegionDrawable(b.icon(Cicon.medium)), Styles.cleari, () -> {
                        Log.info(b.category);
                        rules.allowance.put(b, 0);
                        rebuildAllowance();
                        dialog.hide();
                    }).size(60f).get().resizeImage(Cicon.medium.size);

                    if(++i[0] % cols == 0){
                        t.row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).size(300f, 64f);
    }

    public void show(Rules rules, Prov<Rules> resetter){
        this.rules = rules;
        this.resetter = resetter;
        show();
    }

    void setup(){
        cont.clear();
        cont.pane(m -> main = m).get().setScrollingDisabled(true, false);
        main.margin(10f);
        main.addButton("$settings.reset", () -> {
            rules = resetter.get();
            setup();
            requestKeyboard();
            requestScroll();
        }).size(300f, 50f);
        main.left().defaults().fillX().left().pad(5);
        main.row();

        title("$rules.title.waves");
        check("$rules.waves", b -> rules.waves = b, () -> rules.waves);
        check("$rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer);
        check("$rules.waitForWaveToEnd", b -> rules.waitForWaveToEnd = b, () -> rules.waitForWaveToEnd);
        number("$rules.wavespacing", false, f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, () -> true);
        number("$rules.dropzoneradius", false, f -> rules.dropZoneRadius = f * tilesize, () -> rules.dropZoneRadius / tilesize, () -> true);

        title("$rules.title.respawns");
        number("$rules.respawntime", f -> rules.respawnTime = f * 60f, () -> rules.respawnTime / 60f);

        title("$rules.title.resourcesbuilding");
        check("$rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);
        check("$rules.reactorexplosions", b -> rules.reactorExplosions = b, () -> rules.reactorExplosions);
        number("$rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("$rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier);

        main.addButton("$configure",
            () -> loadoutDialog.show(Blocks.coreShard.itemCapacity, rules.loadout,
                () -> {
                    rules.loadout.clear();
                    rules.loadout.add(new ItemStack(Items.copper, 100));
                }, () -> {}, () -> {}
        )).left().width(300f);
        main.row();

        main.addButton("$allowance", allowanceDialog::show).left().width(300f);
        main.row();

        title("$rules.title.player");
        number("$rules.playerhealthmultiplier", f -> rules.playerHealthMultiplier = f, () -> rules.playerHealthMultiplier);
        number("$rules.playerdamagemultiplier", f -> rules.playerDamageMultiplier = f, () -> rules.playerDamageMultiplier);

        title("$rules.title.unit");
        check("$rules.unitdrops", b -> rules.unitDrops = b, () -> rules.unitDrops, () -> true);
        number("$rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);
        number("$rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);
        number("$rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier);

        title("$rules.title.enemy");
        check("$rules.attack", b -> rules.attackMode = b, () -> rules.attackMode);
        check("$rules.enemyCheat", b -> rules.enemyCheat = b, () -> rules.enemyCheat);
        number("$rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200));

        title("$rules.title.experimental");
        check("$rules.lighting", b -> rules.lighting = b, () -> rules.lighting);

        main.addButton(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(rules.ambientLight));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("$rules.ambientlight");
        }, () -> ui.picker.show(rules.ambientLight, rules.ambientLight::set)).left().width(250f);
        main.row();
    }

    void number(String text, Floatc cons, Floatp prov){
        number(text, false, cons, prov, () -> true);
    }

    void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
            .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            Vars.platform.addDialog(t.addField((integer ? (int)prov.get() : prov.get()) + "", s -> cons.get(Strings.parseFloat(s)))
            .padRight(100f)
            .update(a -> a.setDisabled(!condition.get()))
            .valid(Strings::canParsePositiveFloat).width(120f).left().get());
        }).padTop(0);
        main.row();
    }

    void check(String text, Boolc cons, Boolp prov){
        check(text, cons, prov, () -> true);
    }

    void check(String text, Boolc cons, Boolp prov, Boolp condition){
        main.addCheck(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f).get().left();
        main.row();
    }

    void title(String text){
        main.add(text).color(Pal.accent).padTop(20).padRight(100f).padBottom(-3);
        main.row();
        main.addImage().color(Pal.accent).height(3f).padRight(100f).padBottom(20);
        main.row();
    }

    private int allowanceStep(int amount){
        if(amount < 50){
            return 10;
        }else if(amount < 250){
            return 50;
        }else if(amount < 1000){
            return 250;
        }else{
            return 1000;
        }
    }
}
