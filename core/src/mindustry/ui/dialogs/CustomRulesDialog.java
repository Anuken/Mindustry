package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.Rules.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.ui.*;
import mindustry.world.*;

import static arc.util.Time.*;
import static mindustry.Vars.*;

public class CustomRulesDialog extends BaseDialog{
    Rules rules;
    private Table main;
    private Prov<Rules> resetter;
    private LoadoutDialog loadoutDialog;

    public CustomRulesDialog(){
        super("@mode.custom");

        loadoutDialog = new LoadoutDialog();

        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    private <T extends UnlockableContent> void showBanned(String title, ContentType type, ObjectSet<T> set, Boolf<T> pred){
        BaseDialog bd = new BaseDialog(title);
        bd.addCloseButton();

        Runnable[] rebuild = {null};

        rebuild[0] = () -> {
            float previousScroll = bd.cont.getChildren().isEmpty() ? 0f : ((ScrollPane)bd.cont.getChildren().first()).getScrollY();
            bd.cont.clear();
            bd.cont.pane(t -> {
                t.margin(10f);

                if(set.isEmpty()){
                    t.add("@empty");
                }

                Seq<T> array = set.toSeq();
                array.sort();

                int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
                int i = 0;

                for(T con : array){
                    t.table(Tex.underline, b -> {
                        b.left().margin(4f);
                        b.image(con.uiIcon).size(iconMed).padRight(3);
                        b.add(con.localizedName).color(Color.lightGray).padLeft(3).growX().left().wrap();

                        b.button(Icon.cancel, Styles.clearNonei, () -> {
                            set.remove(con);
                            rebuild[0].run();
                        }).size(70f).pad(-4f).padLeft(0f);
                    }).size(300f, 70f).padRight(5);

                    if(++i % cols == 0){
                        t.row();
                    }
                }
            }).get().setScrollYForce(previousScroll);
            bd.cont.row();
            bd.cont.button("@add", Icon.add, () -> {
                BaseDialog dialog = new BaseDialog("@add");
                dialog.cont.pane(t -> {
                    t.left().margin(14f);
                    int[] i = {0};
                    content.<T>getBy(type).each(b -> !set.contains(b) && pred.get(b), b -> {
                        int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                        t.button(new TextureRegionDrawable(b.uiIcon), Styles.flati, iconMed, () -> {
                            set.add(b);
                            rebuild[0].run();
                            dialog.hide();
                        }).size(60f);

                        if(++i[0] % cols == 0){
                            t.row();
                        }
                    });
                });

                dialog.addCloseButton();
                dialog.show();
            }).size(300f, 64f);
        };

        bd.shown(rebuild[0]);
        bd.buttons.button("@addall", Icon.add, () -> {
            set.addAll(content.<T>getBy(type).select(pred));
            rebuild[0].run();
        }).size(180, 64f);

        bd.buttons.button("@clear", Icon.trash, () -> {
            set.clear();
            rebuild[0].run();
        }).size(180, 64f);

        bd.show();
    }

    public void show(Rules rules, Prov<Rules> resetter){
        this.rules = rules;
        this.resetter = resetter;
        show();
    }

    void setup(){
        cont.clear();
        cont.pane(m -> main = m).scrollX(false);
        main.margin(10f);
        main.button("@settings.reset", () -> {
            rules = resetter.get();
            setup();
            requestKeyboard();
            requestScroll();
        }).size(300f, 50f);
        main.left().defaults().fillX().left().pad(5);
        main.row();

        title("@rules.title.waves");
        check("@rules.waves", b -> rules.waves = b, () -> rules.waves);
        check("@rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer);
        check("@rules.waitForWaveToEnd", b -> rules.waitEnemies = b, () -> rules.waitEnemies);
        number("@rules.wavespacing", false, f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, () -> rules.waveTimer, 1, Float.MAX_VALUE);
        //this is experimental, because it's not clear that 0 makes it default.
        if(experimental){
            number("@rules.initialwavespacing", false, f -> rules.initialWaveSpacing = f * 60f, () -> rules.initialWaveSpacing / 60f, () -> true, 0, Float.MAX_VALUE);
        }
        number("@rules.dropzoneradius", false, f -> rules.dropZoneRadius = f * tilesize, () -> rules.dropZoneRadius / tilesize, () -> true);

        title("@rules.title.resourcesbuilding");
        check("@rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);
        check("@rules.onlydepositcore", b -> rules.onlyDepositCore = b, () -> rules.onlyDepositCore);
        check("@rules.reactorexplosions", b -> rules.reactorExplosions = b, () -> rules.reactorExplosions);
        check("@rules.schematic", b -> rules.schematicsAllowed = b, () -> rules.schematicsAllowed);
        check("@rules.coreincinerates", b -> rules.coreIncinerates = b, () -> rules.coreIncinerates);
        check("@rules.cleanupdeadteams", b -> rules.cleanupDeadTeams = b, () -> rules.cleanupDeadTeams, () -> rules.pvp);
        check("@rules.disableworldprocessors", b -> rules.disableWorldProcessors = b, () -> rules.disableWorldProcessors);
        check("@rules.accessibleworldprocessors", b -> {rules.accessibleWorldProcessors = b; rules.disableWorldProcessors &= !b;}, () -> rules.accessibleWorldProcessors);
        number("@rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("@rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier, 0.001f, 50f);
        number("@rules.deconstructrefundmultiplier", false, f -> rules.deconstructRefundMultiplier = f, () -> rules.deconstructRefundMultiplier, () -> !rules.infiniteResources);
        number("@rules.blockhealthmultiplier", f -> rules.blockHealthMultiplier = f, () -> rules.blockHealthMultiplier);
        number("@rules.blockdamagemultiplier", f -> rules.blockDamageMultiplier = f, () -> rules.blockDamageMultiplier);

        main.button("@configure",
            () -> loadoutDialog.show(999999, rules.loadout,
                i -> true,
                () -> rules.loadout.clear().add(new ItemStack(Items.copper, 100)),
                () -> {}, () -> {}
        )).left().width(300f).row();

        main.button("@bannedblocks", () -> showBanned("@bannedblocks", ContentType.block, rules.bannedBlocks, Block::canBeBuilt)).left().width(300f).row();

        //TODO objectives would be nice
        if(experimental && false){
            main.button("@objectives", () -> {

            }).left().width(300f).row();
        }

        title("@rules.title.unit");
        check("@rules.unitammo", b -> rules.unitAmmo = b, () -> rules.unitAmmo);
        check("@rules.unitcapvariable", b -> rules.unitCapVariable = b, () -> rules.unitCapVariable);
        numberi("@rules.unitcap", f -> rules.unitCap = f, () -> rules.unitCap, -999, 999);
        number("@rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);
        number("@rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier, 0f, 50f);

        main.button("@bannedunits", () -> showBanned("@bannedunits", ContentType.unit, rules.bannedUnits, u -> !u.isHidden())).left().width(300f).row();

        title("@rules.title.enemy");
        check("@rules.attack", b -> rules.attackMode = b, () -> rules.attackMode);
        check("@rules.corecapture", b -> rules.coreCapture = b, () -> rules.coreCapture);
        check("@rules.placerangecheck", b -> rules.placeRangeCheck = b, () -> rules.placeRangeCheck);
        check("@rules.polygoncoreprotection", b -> rules.polygonCoreProtection = b, () -> rules.polygonCoreProtection);
        number("@rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200), () -> !rules.polygonCoreProtection);

        title("@rules.title.environment");
        check("@rules.explosions", b -> rules.damageExplosions = b, () -> rules.damageExplosions);
        check("@rules.fire", b -> rules.fire = b, () -> rules.fire);
        check("@rules.fog", b -> rules.fog = b, () -> rules.fog);
        check("@rules.lighting", b -> rules.lighting = b, () -> rules.lighting);

        if(experimental){
            check("@rules.limitarea", b -> rules.limitMapArea = b, () -> rules.limitMapArea);
            numberi("x", x -> state.rules.limitX = x, () -> state.rules.limitX, () -> state.rules.limitMapArea, 0, 10000);
            numberi("y", y -> state.rules.limitY = y, () -> state.rules.limitY, () -> state.rules.limitMapArea, 0, 10000);
            numberi("w", w -> state.rules.limitWidth = w, () -> state.rules.limitWidth, () -> state.rules.limitMapArea, 0, 10000);
            numberi("h", h -> state.rules.limitHeight = h, () -> state.rules.limitHeight, () -> state.rules.limitMapArea, 0, 10000);
        }

        main.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(rules.ambientLight));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.ambientlight");
        }, () -> ui.picker.show(rules.ambientLight, rules.ambientLight::set)).left().width(250f).row();

        main.button("@rules.weather", this::weatherDialog).width(250f).left().row();

        title("@rules.title.planet");

        main.table(Tex.button, t -> {
            t.margin(10f);
            var group = new ButtonGroup<>();
            var style = Styles.flatTogglet;

            t.defaults().size(140f, 50f);

            //TODO dynamic selection of planets
            for(Planet planet : new Planet[]{Planets.serpulo, Planets.erekir}){
                t.button(planet.localizedName, style, () -> {
                    rules.env = planet.defaultEnv;
                    rules.hiddenBuildItems.clear();
                    rules.hiddenBuildItems.addAll(planet.hiddenItems);
                }).group(group).checked(rules.env == planet.defaultEnv);
            }

            t.button("@rules.anyenv", style, () -> {
                rules.env = Vars.defaultEnv;
                rules.hiddenBuildItems.clear();
            }).group(group).checked(rules.hiddenBuildItems.size == 0);
        }).left().fill(false).expand(false, false).row();

        title("@rules.title.teams");

        team("@rules.playerteam", t -> rules.defaultTeam = t, () -> rules.defaultTeam);
        team("@rules.enemyteam", t -> rules.waveTeam = t, () -> rules.waveTeam);

        for(Team team : Team.baseTeams){
            boolean[] shown = {false};
            Table wasMain = main;

            main.button("[#" + team.color +  "]" + team.localized() + (team.emoji.isEmpty() ? "" : "[] " + team.emoji), Icon.downOpen, Styles.togglet, () -> {
                shown[0] = !shown[0];
            }).marginLeft(14f).width(260f).height(55f).checked(a -> shown[0]).row();

            main.collapser(t -> {
                t.left().defaults().fillX().left().pad(5);
                main = t;
                TeamRule teams = rules.teams.get(team);

                number("@rules.blockhealthmultiplier", f -> teams.blockHealthMultiplier = f, () -> teams.blockHealthMultiplier);
                number("@rules.blockdamagemultiplier", f -> teams.blockDamageMultiplier = f, () -> teams.blockDamageMultiplier);

                check("@rules.rtsai", b -> teams.rtsAi = b, () -> teams.rtsAi, () -> team != rules.defaultTeam);
                numberi("@rules.rtsminsquadsize", f -> teams.rtsMinSquad = f, () -> teams.rtsMinSquad, () -> teams.rtsAi, 0, 100);
                number("@rules.rtsminattackweight", f -> teams.rtsMinWeight = f, () -> teams.rtsMinWeight, () -> teams.rtsAi);

                check("@rules.infiniteresources", b -> teams.infiniteResources = b, () -> teams.infiniteResources);
                number("@rules.buildspeedmultiplier", f -> teams.buildSpeedMultiplier = f, () -> teams.buildSpeedMultiplier, 0.001f, 50f);

                number("@rules.unitdamagemultiplier", f -> teams.unitDamageMultiplier = f, () -> teams.unitDamageMultiplier);
                number("@rules.unitbuildspeedmultiplier", f -> teams.unitBuildSpeedMultiplier = f, () -> teams.unitBuildSpeedMultiplier, 0.001f, 50f);

                main = wasMain;
            }, () -> shown[0]).growX().row();
        }
    }

    void team(String text, Cons<Team> cons, Prov<Team> prov){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5);

            for(Team team : Team.baseTeams){
                t.button(Tex.whiteui, Styles.squareTogglei, 38f, () -> {
                    cons.get(team);
                }).pad(1f).checked(b -> prov.get() == team).size(60f).tooltip(team.localized()).with(i -> i.getStyle().imageUpColor = team.color);
            }
        }).padTop(0).row();
    }

    void number(String text, Floatc cons, Floatp prov){
        number(text, false, cons, prov, () -> true, 0, Float.MAX_VALUE);
    }

    void number(String text, Floatc cons, Floatp prov, float min, float max){
        number(text, false, cons, prov, () -> true, min, max);
    }

    void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition){
        number(text, integer, cons, prov, condition, 0, Float.MAX_VALUE);
    }

    void number(String text, Floatc cons, Floatp prov, Boolp condition){
        number(text, false, cons, prov, condition, 0, Float.MAX_VALUE);
    }

    void numberi(String text, Intc cons, Intp prov, int min, int max){
        numberi(text, cons, prov, () -> true, min, max);
    }

    void numberi(String text, Intc cons, Intp prov, Boolp condition, int min, int max){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
                .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((prov.get()) + "", s -> cons.get(Strings.parseInt(s)))
                .update(a -> a.setDisabled(!condition.get()))
                .padRight(100f)
                .valid(f -> Strings.parseInt(f) >= min && Strings.parseInt(f) <= max).width(120f).left();
        }).padTop(0).row();
    }

    void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition, float min, float max){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
            .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((integer ? (int)prov.get() : prov.get()) + "", s -> cons.get(Strings.parseFloat(s)))
            .padRight(100f)
            .update(a -> a.setDisabled(!condition.get()))
            .valid(f -> Strings.canParsePositiveFloat(f) && Strings.parseFloat(f) >= min && Strings.parseFloat(f) <= max).width(120f).left();
        }).padTop(0);
        main.row();
    }

    void check(String text, Boolc cons, Boolp prov){
        check(text, cons, prov, () -> true);
    }

    void check(String text, Boolc cons, Boolp prov, Boolp condition){
        main.check(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f).get().left();
        main.row();
    }

    void title(String text){
        main.add(text).color(Pal.accent).padTop(20).padRight(100f).padBottom(-3);
        main.row();
        main.image().color(Pal.accent).height(3f).padRight(100f).padBottom(20);
        main.row();
    }

    Cell<TextField> field(Table table, float value, Floatc setter){
        return table.field(Strings.autoFixed(value, 2), v -> setter.get(Strings.parseFloat(v)))
            .valid(Strings::canParsePositiveFloat)
            .size(90f, 40f).pad(2f);
    }

    void weatherDialog(){
        BaseDialog dialog = new BaseDialog("@rules.weather");
        Runnable[] rebuild = {null};

        dialog.cont.pane(base -> {

            rebuild[0] = () -> {
                base.clearChildren();
                int cols = Math.max(1, (int)(Core.graphics.getWidth() / Scl.scl(450)));
                int idx = 0;

                for(WeatherEntry entry : rules.weather){
                    base.top();
                    //main container
                    base.table(Tex.pane, c -> {
                        c.margin(0);

                        //icons to perform actions
                        c.table(Tex.whiteui, t -> {
                            t.setColor(Pal.gray);

                            t.top().left();
                            t.add(entry.weather.localizedName).left().padLeft(6);

                            t.add().growX();

                            ImageButtonStyle style = Styles.geni;
                            t.defaults().size(42f);

                            t.button(Icon.cancel, style, () -> {
                                rules.weather.remove(entry);
                                rebuild[0].run();
                            });
                        }).growX();

                        c.row();

                        //all the options
                        c.table(f -> {
                            f.marginLeft(4);
                            f.left().top();

                            f.defaults().padRight(4).left();

                            f.add("@rules.weather.duration");
                            field(f, entry.minDuration / toMinutes, v -> entry.minDuration = v * toMinutes).disabled(v -> entry.always);
                            f.add("@waves.to");
                            field(f, entry.maxDuration / toMinutes, v -> entry.maxDuration = v * toMinutes).disabled(v -> entry.always);
                            f.add("@unit.minutes");

                            f.row();

                            f.add("@rules.weather.frequency");
                            field(f, entry.minFrequency / toMinutes, v -> entry.minFrequency = v * toMinutes).disabled(v -> entry.always);
                            f.add("@waves.to");
                            field(f, entry.maxFrequency / toMinutes, v -> entry.maxFrequency = v * toMinutes).disabled(v -> entry.always);
                            f.add("@unit.minutes");

                            f.row();

                            f.check("@rules.weather.always", val -> entry.always = val).checked(cc -> entry.always).padBottom(4);

                            //intensity can't currently be customized

                        }).grow().left().pad(6).top();
                    }).width(410f).pad(3).top().left().fillY();

                    if(++idx % cols == 0){
                        base.row();
                    }
                }
            };

            rebuild[0].run();
        }).grow();

        dialog.addCloseButton();

        dialog.buttons.button("@add", Icon.add, () -> {
            BaseDialog add = new BaseDialog("@add");
            add.cont.pane(t -> {
                t.background(Tex.button);
                int i = 0;
                for(Weather weather : content.<Weather>getBy(ContentType.weather)){
                    if(weather.hidden) continue;

                    t.button(weather.localizedName, Styles.flatt, () -> {
                        rules.weather.add(new WeatherEntry(weather));
                        rebuild[0].run();

                        add.hide();
                    }).size(140f, 50f);
                    if(++i % 2 == 0) t.row();
                }
            });
            add.addCloseButton();
            add.show();
        }).width(170f);

        dialog.show();
    }
}
