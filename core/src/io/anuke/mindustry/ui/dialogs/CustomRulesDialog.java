package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.function.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.ItemType;

import static io.anuke.mindustry.Vars.tilesize;

public class CustomRulesDialog extends FloatingDialog{
    private Table main;
    private Rules rules;
    private Supplier<Rules> resetter;
    private LoadoutDialog loadoutDialog;

    public CustomRulesDialog(){
        super("$mode.custom");

        loadoutDialog = new LoadoutDialog();
        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    public void show(Rules rules, Supplier<Rules> resetter){
        this.rules = rules;
        this.resetter = resetter;
        show();
    }

    void setup(){
        cont.clear();
        cont.pane(m -> main = m);
        main.margin(10f);
        main.addButton("$settings.reset", () -> {
            rules = resetter.get();
            setup();
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
        check("$rules.limitedRespawns", b -> rules.limitedRespawns = b, () -> rules.limitedRespawns, ()->!rules.pvp || rules.resourcesWar);
        number("$rules.respawns", true, f -> rules.respawns = (int)f, () -> rules.respawns, () -> rules.limitedRespawns);
        number("$rules.respawntime", f -> rules.respawnTime = f * 60f, () -> rules.respawnTime / 60f);

        title("$rules.title.resourcesbuilding");
        check("$rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);
        number("$rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("$rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier);

        main.addButton("$configure",
                () -> loadoutDialog.show(
                    Blocks.coreShard.itemCapacity,
                    () -> rules.loadout,
                    () -> {
                        rules.loadout.clear();
                        rules.loadout.add(new ItemStack(Items.copper, 200));
                    },
                    () -> {}, () -> {},
                    item -> item.type == ItemType.material
        )).left().width(300f);
        main.row();

        title("$rules.title.player");
        number("$rules.playerdamagemultiplier", f -> rules.playerDamageMultiplier = f, () -> rules.playerDamageMultiplier);
        number("$rules.playerhealthmultiplier", f -> rules.playerHealthMultiplier = f, () -> rules.playerHealthMultiplier);

        title("$rules.title.unit");
        check("$rules.unitdrops", b -> rules.unitDrops = b, () -> rules.unitDrops, () -> true);
        number("$rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier);
        number("$rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);
        number("$rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);

        title("$rules.title.enemy");
        check("$rules.attack", b -> rules.attackMode = b, () -> rules.attackMode);
        check("$rules.enemyCheat", b -> rules.enemyCheat = b, () -> rules.enemyCheat);
        number("$rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200));

        title("$rules.title.resourceswar");

        main.table(t->{
            t.left();
            t.add("$rules.regularmode").update(tt->tt.setColor(rules.resourcesWar ? Color.WHITE : Pal.darkishGray)).left();
            t.row();
            t.add("$rules.rushmode").update(tt->tt.setColor(rules.resourcesWar ? Color.WHITE : Pal.darkishGray)).left();
            t.row();
        }).padTop(20).padBottom(30).padRight(100f).left();
        main.row();
        check("$rules.rushmode.check", b -> rules.rushGame = b, () -> rules.rushGame, () -> rules.resourcesWar);
        main.table(t->{}).padTop(20);
        main.row();

        number("$rules.eliminationspacing", false, f -> rules.eliminationTime = f * 60f, () -> rules.eliminationTime / 60f, () -> rules.resourcesWar && !rules.rushGame);
        number("$rules.pointsthreshold", true, i -> rules.firstThreshold = (int) i, () -> rules.firstThreshold, () -> rules.resourcesWar && rules.rushGame);
        number("$rules.pointsthresholdbump", true, i -> rules.bumpThreshold = (int) i, () -> rules.bumpThreshold, () -> rules.resourcesWar && rules.rushGame);
        check("$rules.buffing", b -> rules.buffing = b, () -> rules.buffing, () -> rules.resourcesWar);
        number("$rules.bufftime", false, f -> rules.buffTime = f * 60f, () -> rules.buffTime / 60f, () -> rules.buffing);
        number("$rules.buffspacing", false, f -> rules.buffSpacing = f * 60f, () -> rules.buffSpacing / 60f, () -> rules.buffing);
        number("$rules.buffmultiplier", false, f -> rules.buffMultiplier = f, () -> rules.buffMultiplier, () -> rules.buffing);
        check("$rules.lifes", b -> rules.enableLifes = b, () -> rules.enableLifes, () -> rules.resourcesWar);
        number("$rules.lifesCount", true, f -> rules.lifesCount = (int)f, () -> rules.lifesCount, () -> rules.enableLifes);
    }

    void number(String text, FloatConsumer cons, FloatProvider prov){
        number(text, false, cons, prov, () -> true);
    }

    void number(String text, boolean integer, FloatConsumer cons, FloatProvider prov, BooleanProvider condition){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
            .update(a -> a.setColor(condition.get() ? Color.WHITE : Color.GRAY));
            Platform.instance.addDialog(t.addField((integer ? (int)prov.get() : prov.get()) + "", s -> cons.accept(Strings.parseFloat(s)))
            .padRight(100f)
            .update(a -> a.setDisabled(!condition.get()))
            .valid(Strings::canParsePositiveFloat).width(120f).left().get());
        }).padTop(0);
        main.row();
    }

    void check(String text, BooleanConsumer cons, BooleanProvider prov){
        check(text, cons, prov, () -> true);
    }

    void check(String text, BooleanConsumer cons, BooleanProvider prov, BooleanProvider condition){
        main.addCheck(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f).get().left();
        main.row();
    }

    void title(String text){
        main.add(text).color(Pal.accent).padTop(20).padBottom(20).padRight(100f);
        main.row();
    }
}
