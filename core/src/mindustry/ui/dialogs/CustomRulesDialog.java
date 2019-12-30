package mindustry.ui.dialogs;

import arc.*;
import arc.struct.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.Cicon;
import mindustry.world.*;

import static mindustry.Vars.*;

public class CustomRulesDialog extends FloatingDialog{
    private Table main;
    private Rules rules;
    private Prov<Rules> resetter;
    private LoadoutDialog loadoutDialog;
    private FloatingDialog banDialog;

    public CustomRulesDialog(){
        super("$mode.custom");

        loadoutDialog = new LoadoutDialog();
        banDialog = new FloatingDialog("$bannedblocks");
        banDialog.addCloseButton();

        banDialog.shown(this::rebuildBanned);
        banDialog.buttons.addImageTextButton("$addall", Icon.arrow16Small, () -> {
            rules.bannedBlocks.addAll(content.blocks().select(Block::isBuildable));
            rebuildBanned();
        }).size(180, 64f);

        banDialog.buttons.addImageTextButton("$clear", Icon.trash16Small, () -> {
            rules.bannedBlocks.clear();
            rebuildBanned();
        }).size(180, 64f);

        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    private void rebuildBanned(){
        float previousScroll = banDialog.cont.getChildren().isEmpty() ? 0f : ((ScrollPane)banDialog.cont.getChildren().first()).getScrollY();
        banDialog.cont.clear();
        banDialog.cont.pane(t -> {
            t.margin(10f);

            if(rules.bannedBlocks.isEmpty()){
                t.add("$empty");
            }

            Array<Block> array = Array.with(rules.bannedBlocks);
            array.sort();

            int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
            int i = 0;

            for(Block block : array){
                t.table(Tex.underline, b -> {
                    b.left().margin(4f);
                    b.addImage(block.icon(Cicon.medium)).size(Cicon.medium.size).padRight(3);
                    b.add(block.localizedName).color(Color.lightGray).padLeft(3).growX().left().wrap();

                    b.addImageButton(Icon.cancelSmall, Styles.clearPartiali, () -> {
                       rules.bannedBlocks.remove(block);
                       rebuildBanned();
                    }).size(70f).pad(-4f).padLeft(0f);
                }).size(300f, 70f).padRight(5);

                if(++i % cols == 0){
                    t.row();
                }
            }
        }).get().setScrollYForce(previousScroll);
        banDialog.cont.row();
        banDialog.cont.addImageTextButton("$add", Icon.addSmall, () -> {
            FloatingDialog dialog = new FloatingDialog("$add");
            dialog.cont.pane(t -> {
                t.left().margin(14f);
                int[] i = {0};
                content.blocks().each(b -> !rules.bannedBlocks.contains(b) && b.isBuildable(), b -> {
                    int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                    t.addImageButton(new TextureRegionDrawable(b.icon(Cicon.medium)), Styles.cleari, () -> {
                        rules.bannedBlocks.add(b);
                        rebuildBanned();
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
        number("$rules.blockhealthmultiplier", f -> rules.blockHealthMultiplier = f, () -> rules.blockHealthMultiplier);

        main.addButton("$configure",
            () -> loadoutDialog.show(Blocks.coreShard.itemCapacity, rules.loadout,
                () -> {
                    rules.loadout.clear();
                    rules.loadout.add(new ItemStack(Items.copper, 100));
                }, () -> {}, () -> {}
        )).left().width(300f);
        main.row();

        main.addButton("$bannedblocks", banDialog::show).left().width(300f);
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
}
