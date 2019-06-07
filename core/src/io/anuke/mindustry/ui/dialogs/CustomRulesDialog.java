package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.function.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.graphics.Pal;

import static io.anuke.mindustry.Vars.tilesize;

public class CustomRulesDialog extends FloatingDialog{
    private Table main;
    private Rules rules;
    private Supplier<Rules> resetter;

    public CustomRulesDialog(){
        super("$mode.custom");

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
        check("$rules.limitedRespawns", b -> rules.limitedRespawns = b, () -> rules.limitedRespawns);
        number("$rules.respawns", true, f -> rules.respawns = (int)f, () -> rules.respawns, () -> rules.limitedRespawns);
        number("$rules.respawntime", f -> rules.respawnTime = f * 60f, () -> rules.respawnTime / 60f);

        title("$rules.title.resourcesbuilding");
        check("$rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);
        number("$rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("$rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier);

        title("$rules.title.player");
        number("$rules.playerdamagemultiplier", f -> rules.playerDamageMultiplier = f, () -> rules.playerDamageMultiplier);
        number("$rules.playerhealthmultiplier", f -> rules.playerHealthMultiplier = f, () -> rules.playerHealthMultiplier);

        title("$rules.title.unit");
        check("$rules.unitdrops", b -> rules.unitDrops = b, () -> rules.unitDrops, () -> true);
        number("$rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier);
        number("$rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);
        number("$rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);

        title("$rules.title.enemy");
        number("$rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200));
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
        main.addCheck(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f);
        main.row();
    }

    void title(String text){
        main.add(text).color(Pal.accent).padTop(20).padBottom(20).padRight(100f);
        main.row();
    }
}
