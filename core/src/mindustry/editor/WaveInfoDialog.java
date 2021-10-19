package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.game.SpawnGroup.*;

public class WaveInfoDialog extends BaseDialog{
    private int start = 0, displayed = 20, graphSpeed = 1;
    private final int maxVisible = 30, maxGraphSpeed = 16;
    Seq<SpawnGroup> groups = new Seq<>();
    private SpawnGroup expandedGroup;

    private Table table, iTable, uTable;
    private int search = -1, filterHealth, filterHealthMode, filterBegin = -1, filterEnd = -1, filterAmount, filterAmountWave;
    private boolean expandPane = false, filterStrict = false;
    private UnitType lastType = UnitTypes.dagger;
    private StatusEffect filterEffect = StatusEffects.none;
    private Sort sort = Sort.begin;
    private boolean reverseSort = false;
    private float updateTimer, payLeft, updatePeriod = 1f;
    private TextField amountField = new TextField();
    private WaveGraph graph = new WaveGraph();

    public WaveInfoDialog(){
        super("@waves.title");

        shown(this::setup);
        hidden(() -> state.rules.spawns = groups);

        addCloseListener();

        onResize(this::setup);
        addCloseButton();

        buttons.button("@waves.edit", Icon.pencil, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.table(Tex.button, t -> {
                var style = Styles.cleart;
                t.defaults().size(230f, 64f).pad(2f);

                t.button("@waves.copy", Icon.copy, style, () -> {
                    ui.showInfoFade("@waves.copied");
                    Core.app.setClipboardText(maps.writeWaves(groups));
                    dialog.hide();
                }).disabled(b -> groups == null).marginLeft(12f).row();

                t.button("@waves.load", Icon.download, style, () -> {
                    try{
                        groups = maps.readWaves(Core.app.getClipboardText());
                        buildGroups();
                    }catch(Exception e){
                        e.printStackTrace();
                        ui.showErrorMessage("@waves.invalid");
                    }
                    dialog.hide();
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith("[{")).row();

                t.button("@settings.reset", Icon.upload, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups = JsonIO.copy(waves.get());
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f).row();

                t.button("@clear", Icon.cancel, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups.clear();
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f);
            });

            dialog.show();
        }).size(250f, 64f);

        buttons.defaults().width(60f);

        buttons.button("<", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                shift(-graphSpeed);
            }
        });
        buttons.button(">", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                shift(graphSpeed);
            }
        });

        buttons.button("-", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                view(-graphSpeed);
            }
        });
        buttons.button("+", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                view(graphSpeed);
            }
        });

        if(experimental){
            buttons.button("x" + graphSpeed, () -> {
                graphSpeed *= 2;
                if(graphSpeed > maxGraphSpeed) graphSpeed = 1;
            }).update(b -> b.setText("x" + graphSpeed)).width(100f);

            buttons.button("Random", Icon.refresh, () -> {
                groups.clear();
                groups = Waves.generate(1f / 10f);
                buildGroups();
            }).width(200f);
        }
    }

    void view(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            displayed += amount;
            if(displayed < 5) displayed = 5;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void shift(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            start += amount;
            if(start < 0) start = 0;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? waves.get() : state.rules.spawns);

        cont.clear();
        cont.stack(new Table(Tex.clear, main -> {
            main.table(s -> {
                s.image(Icon.zoom).padRight(8);
                s.field(search < 0 ? "" : search + "", TextFieldFilter.digitsOnly, text -> {
                    search = !text.isEmpty() ? Math.max(Strings.parseInt(text) - 1, -1) : -1;
                    start = Math.max(search - (displayed / 2) - (displayed % 2), 0);
                    buildGroups();
                }).growX().maxTextLength(8).get().setMessageText("@waves.search");
                s.button(Icon.filter, Styles.emptyi, this::showFilters).size(46f).tooltip("@waves.filter");
            }).fillX().pad(6f).row();
            main.pane(t -> table = t).grow().padRight(8f).scrollX(false).row();
            main.table(f -> {
                f.button("@add", () -> {
                    if(groups == null) groups = new Seq<>();
                    SpawnGroup newGroup = new SpawnGroup(lastType);
                    groups.add(newGroup);
                    expandedGroup = newGroup;
                    showContentPane(newGroup, c -> newGroup.type = lastType = c, () -> newGroup.type);
                    buildGroups();
                    clearFilters();
                }).growX().height(70f);
                f.button(Icon.filter, () -> {
                    BaseDialog dialog = new BaseDialog("@waves.sort");
                    dialog.setFillParent(false);
                    dialog.cont.table(Tex.button, t -> {
                        for(Sort s : Sort.all){
                            if(s == Sort.tertiary) continue;
                            t.button("@waves.sort." + s, Styles.clearTogglet, () -> {
                                sort = s;
                                dialog.hide();
                                buildGroups();
                            }).size(150f, 60f).checked(s == sort);
                        }
                    }).row();
                    dialog.cont.check("@waves.sort.reverse", b -> {
                        reverseSort = b;
                        buildGroups();
                    }).padTop(4).checked(reverseSort).padBottom(8f);
                    dialog.addCloseButton();
                    dialog.show();
                    buildGroups();
                }).size(64f, 70f).padLeft(4f);
            }).fillX();
        }), new Label("@waves.none"){{
            visible(() -> groups.isEmpty());
            this.touchable = Touchable.disabled;
            setWrap(true);
            setAlignment(Align.center, Align.center);
        }}).width(390f).growY();

        cont.add(graph = new WaveGraph()).grow();

        buildGroups();
    }

    void buildGroups(){
        table.clear();
        table.top();
        table.margin(10f);

        var comps = Structs.comps(Structs.comps(Structs.comparingFloat(sort.sort), Structs.comparingFloat(sort.secondary)), Structs.comps(Structs.comparingFloat(Sort.tertiary.sort), Structs.comparingFloat(Sort.tertiary.secondary)));

        if(groups != null){
            groups.sort(comps);
            if(reverseSort) groups.reverse();

            int range = filterStrict ? 0 : 20;

            for(SpawnGroup group : groups){
                if((search >= 0 && group.getSpawned(search) <= 0)
                || (filterHealth != 0 && !(filterHealthMode == 0 ? group.type.health > filterHealth : filterHealthMode == 1 ? group.type.health < filterHealth : filterHealth - range*2 <= group.type.health && filterHealth + range*2 >= group.type.health))
                || (filterBegin >= 0 && !(filterStrict ? group.begin == filterBegin : group.begin - range/10 <= filterBegin && group.begin + range/10 >= filterBegin))
                || (filterEnd >= 0 && !(group.end - range/10 <= filterEnd && group.end + range/10 >= filterEnd))
                || (filterAmount != 0 && !(filterAmount - range/4 <= group.getSpawned(filterAmountWave) && filterAmount + range/4 >= group.getSpawned(filterAmountWave)))
                || (filterEffect != null && filterEffect != StatusEffects.none && group.effect != filterEffect)) continue;

                table.table(Tex.button, t -> {
                    t.margin(0).defaults().pad(3).padLeft(5f).growX().left();
                    t.button(b -> {
                        b.left();
                        b.image(group.type.uiIcon).size(32f).padRight(3).scaling(Scaling.fit);
                        if(group.effect != null && group.effect != StatusEffects.none) b.image(group.effect.uiIcon).size(20f).padRight(3).scaling(Scaling.fit);
                        b.add(group.type.localizedName).color(Pal.accent);

                        b.add().growX();

                        b.label(() -> (group.begin + 1) + "").color(Color.lightGray).minWidth(45f).labelAlign(Align.left).left();

                        b.button(Icon.settingsSmall, Styles.emptyi, () -> {
                            BaseDialog dialog = new BaseDialog("@waves.group");
                            dialog.setFillParent(false);
                            dialog.cont.table(Tex.button, a -> iTable = a).row();
                            dialog.cont.table(c -> {
                                c.defaults().size(210f, 64f).pad(2f);
                                c.button("@waves.duplicate", Icon.copy, () -> {
                                    SpawnGroup copy = group.copy();
                                    groups.add(copy);
                                    expandedGroup = copy;
                                    buildGroups();
                                    dialog.hide();
                                });
                                c.button("@settings.resetKey", Icon.refresh, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                                    group.effect = StatusEffects.none;
                                    group.payloads = Seq.with();
                                    group.items = null;
                                    buildGroups();
                                    dialog.hide();
                                }));
                            });
                            buildGroups();
                            updateIcons(group);
                            dialog.addCloseButton();
                            dialog.show();
                        }).pad(-6).size(46f);
                        b.button(Icon.unitsSmall, Styles.emptyi, () -> showContentPane(group, c -> group.type = lastType = c, () -> group.type)).pad(-6).size(46f);
                        b.button(Icon.cancel, Styles.emptyi, () -> {
                            if(expandedGroup == group) expandedGroup = null;
                            groups.remove(group);
                            table.getCell(t).pad(0f);
                            t.remove();
                            buildGroups();
                        }).pad(-6).size(46f).padRight(-12f);
                    }, () -> {
                        expandedGroup = expandedGroup == group ? null : group;
                        buildGroups();
                    }).height(46f).pad(-6f).padBottom(0f).row();

                    if(expandedGroup == group){
                        t.table(spawns -> {
                            numField("", spawns, f -> group.begin = f - 1, () -> group.begin + 1, 100f);
                            spawns.add("@waves.to").padLeft(4).padRight(4);
                            spawns.field(group.end == never ? "" : (group.end + 1) + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.end = Strings.parseInt(text) - 1;
                                    updateWaves();
                                }else if(text.isEmpty()){
                                    group.end = never;
                                    updateWaves();
                                }
                            }).width(100f).get().setMessageText("∞");
                        }).row();

                        t.table(p -> {
                            p.add("@waves.every").padRight(4);
                            p.field(group.spacing + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0){
                                    group.spacing = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(100f);
                            p.add("@waves.waves").padLeft(4);
                        }).row();

                        t.table(a -> {
                            numField("", a, f -> group.unitAmount = f, () -> group.unitAmount, 80f);

                            a.add(" + ");
                            a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextFieldFilter.floatsOnly, text -> {
                                if(Strings.canParsePositiveFloat(text)){
                                    group.unitScaling = 1f / Strings.parseFloat(text);
                                    updateWaves();
                                }
                            }).width(80f);
                            a.add("@waves.perspawn").padLeft(4);
                        }).row();

                        t.table(a -> {
                            numField("", a, f -> group.max = f, () -> group.max, 80f);
                            a.add("@waves.max").padLeft(5);
                        }).row();

                        t.table(a -> {
                            numField("", a, f -> group.shields = f, () -> (int)group.shields, 80f);
                            numField(" + ", a, f -> group.shieldScaling = f, () -> (int)group.shieldScaling, 80f);
                            a.add("@waves.shields").padLeft(4);
                        }).row();

                        t.check("@waves.guardian", b -> {
                            group.effect = (b ? StatusEffects.boss : null);
                            buildGroups();
                        }).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss)).padBottom(8f).row();

                        //spawn positions are clunky and thus experimental for now
                        if(experimental){
                            t.table(a -> {
                                a.add("spawn at ");

                                a.field(group.spawn == -1 ? "" : Point2.x(group.spawn) + "", TextFieldFilter.digitsOnly, text -> {
                                    if(Strings.canParsePositiveInt(text)){
                                        group.spawn = Point2.pack(Strings.parseInt(text), Point2.y(group.spawn));
                                        Log.info(group.spawn);
                                    }
                                }).width(70f);

                                a.add(",");

                                a.field(group.spawn == -1 ? "" : Point2.y(group.spawn) + "", TextFieldFilter.digitsOnly, text -> {
                                    if(Strings.canParsePositiveInt(text)){
                                        group.spawn = Point2.pack(Point2.x(group.spawn), Strings.parseInt(text));
                                        Log.info(group.spawn);
                                    }
                                }).width(70f);
                            }).padBottom(8f).padTop(-8f).row();
                        }
                    }
                }).width(340f).pad(8);

                table.row();
            }

            if(table.getChildren().isEmpty() && groups.any()){
                table.add("@none");
            }
        }else{
            table.add("@editor.default");
        }

        updateWaves();
    }

    <T extends UnlockableContent> void showContentPane(SpawnGroup group, Cons<T> cons, Prov<T> prov){
        showContentPane(group, cons, prov, i -> true, false);
    }

    <T extends UnlockableContent> void showContentPane(SpawnGroup group, Cons<T> cons, Prov<T> prov, Boolf<T> pred, boolean payloads){
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        ContentType ctype = prov.get().getContentType();

        if(payloads && ctype == ContentType.unit){
            if(group.payloads == null) group.payloads = new Seq<>();
            dialog.cont.table(e -> {
                uTable = e;
                updateIcons(group);
            }).padBottom(6f).row();
        }

        if(ctype == ContentType.item) dialog.cont.table(items -> {
            items.add(Core.bundle.get("filter.option.amount") + ":");
            amountField = items.field(group.items != null ? group.items.amount + "" : "", TextFieldFilter.digitsOnly, text -> {
                if(Strings.canParsePositiveInt(text) && group.items != null){
                    group.items.amount = Strings.parseInt(text) <= 0 ? group.type.itemCapacity : Mathf.clamp(Strings.parseInt(text), 0, group.type.itemCapacity);
                }
            }).width(120f).pad(2).margin(12f).maxTextLength((group.type.itemCapacity + "").length() + 1).get();
            amountField.setMessageText(group.type.itemCapacity + "");
        }).padBottom(6f).row();

        dialog.cont.pane(p -> {
            int[] i = {ctype != ContentType.unit ? 1 : 0};
            p.defaults().pad(2).margin(12f).minWidth(ctype == ContentType.item ? 200f : Float.MIN_VALUE).fillX();
            if(ctype != ContentType.unit) p.button(icon -> {
                icon.left();
                icon.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                icon.add("@settings.resetKey");
            }, () -> {
                cons.get(null);
                updateIcons(group);
                dialog.hide();
                buildGroups();
            });
            content.<T>getBy(ctype).each(type -> !type.isHidden() && pred.get(type), type -> {
                p.button(t -> {
                    t.left();
                    t.image(type.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    if(ctype == ContentType.item){
                        int amount = Strings.parseInt(amountField.getText()) <= 0 ? group.type.itemCapacity : Mathf.clamp(Strings.parseInt(amountField.getText()), 0, group.type.itemCapacity);
                        group.items = new ItemStack((Item)type, amount);
                    }else{
                        cons.get(type);
                    }

                    if(ctype == ContentType.unit){
                        if(group.payloads != null && !(group.type.constructor.get() instanceof Payloadc)) group.payloads.clear();
                        if(group.items != null) group.items.amount = Mathf.clamp(group.items.amount, 0, group.type.itemCapacity);
                    }
                    if(!payloads) dialog.hide();
                    updateIcons(group);
                    buildGroups();
                }).update(b -> b.setDisabled(payloads && type instanceof UnitType u && payLeft < u.hitSize * u.hitSize));
                if(++i[0] % 3 == 0) p.row();
            });
        });
        dialog.addCloseButton();
        dialog.show();
    }

    void showFilters(){
        BaseDialog dialog = new BaseDialog("@waves.filter");
        dialog.setFillParent(false);

        Runnable[] rebuild = {null};

        rebuild[0] = () -> {
            dialog.cont.clearChildren();
            dialog.cont.defaults().size(210f, 64f);

            dialog.cont.add(Core.bundle.get("waves.sort.health") + ":");
            dialog.cont.table(filter -> {
                filter.button(">", Styles.cleart, () -> {
                    filterHealthMode ++;
                    if(filterHealthMode == 3) filterHealthMode = 0;
                    buildGroups();
                }).update(b -> b.setText(filterHealthMode == 0 ? ">" : filterHealthMode == 1 ? "<" : "~")).size(40f).padRight(4f);
                numField("", filter, f -> filterHealth = f, () -> filterHealth, 170f, 15);
            }).row();

            dialog.cont.add("@waves.filter.begin");
            dialog.cont.table(filter -> {
                numField("", filter, f -> filterBegin = f - 1, () -> filterBegin + 1, 120f, 8);
                numField("@waves.to", filter, f -> filterEnd = f - 1, () -> filterEnd + 1, 120f, 8);
            }).row();

            dialog.cont.add(Core.bundle.get("waves.filter.amount") + ":");
            dialog.cont.table(filter -> {
                numField("", filter, f -> filterAmount = f, () -> filterAmount, 120f, 12);
                numField("@waves.filter.onwave", filter, f -> filterAmountWave = f, () -> filterAmountWave, 120f, 8);
            }).row();

            dialog.cont.table(filter -> {
                filter.add(Core.bundle.get("waves.filter.effect") + ":");
                filter.button(filterEffect != null && filterEffect != StatusEffects.none ? new TextureRegionDrawable(filterEffect.uiIcon) :
                Icon.logic, () -> {
                    showContentPane(null, c -> filterEffect = c, () -> StatusEffects.none, effect -> !(effect.isHidden()  || effect.reactive), false);
                    rebuild[0].run();
                }).update(b -> {
                    b.reset();
                    b.image(filterEffect != null && filterEffect != StatusEffects.none ? new TextureRegionDrawable(filterEffect.uiIcon) : Icon.logic);
                }).padLeft(30f).size(60f);
            });
        };
        rebuild[0].run();

        dialog.row();
        dialog.check("@waves.filter.strict", b -> {
            filterStrict = b;
            buildGroups();
        }).update(b -> b.setChecked(filterStrict)).padBottom(10f).row();

        dialog.table(p -> {
            p.defaults().size(210f, 64f).padLeft(4f).padRight(4f);
            p.button("@back", Icon.left, dialog::hide);
            p.button("@clear", Icon.refresh, () -> {
                clearFilters();
                buildGroups();
                rebuild[0].run();
            });
        });

        dialog.addCloseListener();
        dialog.show();
    }

    void updateIcons(SpawnGroup group){
        if(group == null) return;

        if(iTable != null){
            iTable.clear();
            iTable.defaults().size(200f, 60f).pad(2f);
            iTable.button(icon -> {
                if(group.effect != null && group.effect != StatusEffects.none){
                    icon.image(group.effect.uiIcon).padRight(6f);
                }else{
                    icon.image(Icon.logic).padRight(6f);
                }
                icon.add("@waves.group.effect");
            }, Styles.cleart, () -> showContentPane(group, c -> group.effect = c, () -> StatusEffects.none, effect -> !(effect.isHidden()  || effect.reactive), false));
            iTable.button("@waves.group.payloads", Icon.defense, Styles.cleart,
            () -> showContentPane(group, c -> group.payloads.add(c), () -> group.type, type -> !(type.isHidden() || type.hitSize * type.hitSize > group.type.payloadCapacity || type.flying), true))
            .disabled(!(group.type.constructor.get() instanceof Payloadc));
            iTable.button(icon -> {
                if(group.items != null){
                    icon.image(group.items.item.uiIcon).padRight(6f);
                }else{
                    icon.image(Icon.effect).padRight(6f);
                }
                icon.add("@waves.group.items");
            }, Styles.cleart, () -> showContentPane(group, c -> group.items = null, () -> Items.copper));
        }

        if(uTable != null && group.payloads != null){
            uTable.clear();
            uTable.left();
            uTable.defaults().pad(3);
            payLeft = group.type.payloadCapacity;
            uTable.table(units -> {
                int i = 0;
                for(UnitType payl : group.payloads){
                    if(i < maxVisible || expandPane){
                        units.table(Tex.button, s -> {
                            s.image(payl.uiIcon).size(45f);
                            s.button(Icon.cancelSmall, Styles.emptyi, () -> {
                                group.payloads.remove(payl);
                                updateIcons(group);
                                buildGroups();
                            }).size(20f).padRight(-9f).padLeft(-6f);
                        }).pad(2).margin(12f).fillX();
                    }
                    payLeft -= payl.hitSize * payl.hitSize;
                    if(++i % 10 == 0) units.row();
                }
            });
            uTable.table(b -> {
                b.defaults().pad(2);
                if(group.payloads.size > 1) b.button(Icon.cancel, () -> {
                    group.payloads.clear();
                    updateIcons(group);
                    buildGroups();
                }).tooltip("@clear").row();
                if(group.payloads.size > maxVisible) b.button(expandPane ? Icon.eyeSmall : Icon.eyeOffSmall, () -> {
                    expandPane = !expandPane;
                    updateIcons(group);
                }).size(45f).tooltip(expandPane ? "@server.shown" : "@server.hidden");
            }).padLeft(6f);
        }
    }

    void numField(String text, Table t, Intc cons, Intp prov, float width){
        numField(text, t, cons, prov, width, Integer.MAX_VALUE);
    }

    void numField(String text, Table t, Intc cons, Intp prov, float width, int maxLength){
        if(!text.isEmpty()) t.add(text);
        t.field(prov.get() + "", TextFieldFilter.digitsOnly, input -> {
            if(Strings.canParsePositiveInt(input)){
                cons.get(!input.isEmpty() ? Strings.parseInt(input) : 0);
                if(maxLength != Integer.MAX_VALUE) buildGroups();
                updateWaves();
            }
        }).width(width).maxTextLength(maxLength);
    }

    void clearFilters(){
        filterHealth = filterHealthMode = filterAmount = filterAmountWave = 0;
        filterStrict = false;
        filterBegin = filterEnd = -1;
        filterEffect = StatusEffects.none;
    }

    enum Sort{
        begin(g -> g.begin),
        health(g -> g.type.health),
        type(g -> g.type.id, g -> g.begin),
        //special tertiary sort value
        tertiary(g -> g.effect != null ? g.effect.id : 0, g -> g.payloads != null ? g.payloads.size : 0);

        static final Sort[] all = values();

        final Floatf<SpawnGroup> sort, secondary;

        Sort(Floatf<SpawnGroup> sort){
            this.sort = sort;
            this.secondary = g -> g.type.id;
        }

        Sort(Floatf<SpawnGroup> sort, Floatf<SpawnGroup> secondary){
            this.sort = sort;
            this.secondary = secondary;
        }
    }

    void updateWaves(){
        graph.groups = groups;
        graph.from = start;
        graph.to = start + displayed;
        graph.rebuild();
    }
}
