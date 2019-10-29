package io.anuke.mindustry.ui.fragments;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.actions.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.ImageButton.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.ctype.UnlockableContent;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.*;

public class HudFragment extends Fragment{
    public final PlacementFragment blockfrag = new PlacementFragment();

    private ImageButton flip;
    private Table lastUnlockTable;
    private Table lastUnlockLayout;
    private boolean shown = true;
    private float dsize = 59;

    private float coreAttackTime;
    private float lastCoreHP;
    private Team lastTeam;
    private float coreAttackOpacity = 0f;
    private long lastToast;

    public void build(Group parent){

        //menu at top left
        parent.fill(cont -> {
            cont.setName("overlaymarker");
            cont.top().left();

            if(mobile){

                {
                    Table select = new Table();

                    select.left();
                    select.defaults().size(dsize).left();

                    ImageButtonStyle style = Styles.clearTransi;

                    select.addImageButton(Icon.menuLarge, style, ui.paused::show);
                    flip = select.addImageButton(Icon.arrowUp, style, this::toggleMenus).get();

                    select.addImageButton(Icon.pause, style, () -> {
                        if(net.active()){
                            ui.listfrag.toggle();
                        }else{
                            state.set(state.is(State.paused) ? State.playing : State.paused);
                        }
                    }).name("pause").update(i -> {
                        if(net.active()){
                            i.getStyle().imageUp = Icon.players;
                        }else{
                            i.setDisabled(false);
                            i.getStyle().imageUp = state.is(State.paused) ? Icon.play : Icon.pause;
                        }
                    }).get();

                    select.addImageButton(Icon.settings, style,() -> {
                        if(net.active() && mobile){
                            if(ui.chatfrag.chatOpen()){
                                ui.chatfrag.hide();
                            }else{
                                ui.chatfrag.toggle();
                            }
                        }else if(world.isZone()){
                            ui.tech.show();
                        }else{
                            ui.database.show();
                        }
                    }).update(i -> {
                        if(net.active() && mobile){
                            i.getStyle().imageUp = Icon.chat;
                        }else{
                            i.getStyle().imageUp = Icon.database;
                        }
                    }).get();

                    select.addImage().color(Pal.gray).width(4f).fillY();

                    float size = Scl.scl(dsize);
                    Array<Element> children = new Array<>(select.getChildren());

                    //now, you may be wondering, why is this necessary? the answer is, I don't know, but it fixes layout issues somehow
                    int index = 0;
                    for(Element elem : children){
                        int fi = index++;
                        parent.addChild(elem);
                        elem.visible(() -> {
                            if(fi < 4){
                                elem.setSize(size);
                            }else{
                                elem.setSize(Scl.scl(4f), size);
                            }
                            elem.setPosition(fi * size, Core.graphics.getHeight(), Align.topLeft);
                            return true;
                        });
                    }

                    cont.add().size(dsize * 4 + 3, dsize).left();
                }

                cont.row();
                cont.addImage().height(4f).color(Pal.gray).fillX();
                cont.row();
            }

            cont.update(() -> {
                if(Core.input.keyTap(Binding.toggle_menus) && !ui.chatfrag.chatOpen() && !Core.scene.hasDialog() && !(Core.scene.getKeyboardFocus() instanceof TextField)){
                    toggleMenus();
                }
            });

            Table wavesMain, editorMain;

            cont.stack(wavesMain = new Table(), editorMain = new Table()).height(wavesMain.getPrefHeight());

            {
                wavesMain.visible(() -> shown && !state.isEditor());
                wavesMain.top().left();
                Stack stack = new Stack();
                Button waves = new Button(Styles.waveb);
                Table btable = new Table().margin(0);

                stack.add(waves);
                stack.add(btable);

                addWaveTable(waves);
                addPlayButton(btable);
                wavesMain.add(stack).width(dsize * 4 + 4f);
                wavesMain.row();
                wavesMain.table(Tex.button, t -> t.margin(10f).add(new Bar("boss.health", Pal.health, () -> state.boss() == null ? 0f : state.boss().healthf()).blink(Color.white))
                .grow()).fillX().visible(() -> state.rules.waves && state.boss() != null).height(60f).get();
                wavesMain.row();
            }

            {
                editorMain.table(Tex.buttonEdge4, t -> {
                    //t.margin(0f);
                    t.add("$editor.teams").growX().left();
                    t.row();
                    t.table(teams -> {
                        teams.left();
                        int i = 0;
                        for(Team team : Team.all){
                            ImageButton button = teams.addImageButton(Tex.whiteui, Styles.clearTogglePartiali, 40f, () -> Call.setPlayerTeamEditor(player, team))
                                .size(50f).margin(6f).get();
                            button.getImageCell().grow();
                            button.getStyle().imageUpColor = team.color;
                            button.update(() -> button.setChecked(player.getTeam() == team));

                            if(++i % 3 == 0){
                                teams.row();
                            }
                        }
                    }).left();

                    if(enableUnitEditing){

                        t.row();
                        t.addImageTextButton("$editor.spawn", Icon.add, () -> {
                            FloatingDialog dialog = new FloatingDialog("$editor.spawn");
                            int i = 0;
                            for(UnitType type : content.<UnitType>getBy(ContentType.unit)){
                                dialog.cont.addImageButton(Tex.whiteui, 8 * 6f, () -> {
                                    Call.spawnUnitEditor(player, type);
                                    dialog.hide();
                                }).get().getStyle().imageUp = new TextureRegionDrawable(type.icon(Cicon.xlarge));
                                if(++i % 4 == 0) dialog.cont.row();
                            }
                            dialog.addCloseButton();
                            dialog.setFillParent(false);
                            dialog.show();
                        }).fillX();

                        float[] size = {0};
                        float[] position = {0, 0};

                        t.row();
                        t.addImageTextButton("$editor.removeunit", Icon.quit, Styles.togglet, () -> {}).fillX().update(b -> {
                            boolean[] found = {false};
                            if(b.isChecked()){
                                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                                if(e == null){
                                    Vector2 world = Core.input.mouseWorld();
                                    Units.nearby(world.x, world.y, 1f, 1f, unit -> {
                                        if(!found[0] && unit instanceof BaseUnit){
                                            if(Core.input.keyTap(KeyCode.MOUSE_LEFT)){
                                                Call.removeUnitEditor(player, (BaseUnit)unit);
                                            }
                                            found[0] = true;
                                            unit.hitbox(Tmp.r1);
                                            size[0] = Mathf.lerpDelta(size[0], Tmp.r1.width * 2f + Mathf.absin(Time.time(), 10f, 5f), 0.1f);
                                            position[0] = unit.x;
                                            position[1] = unit.y;
                                        }
                                    });
                                }
                            }

                            Draw.color(Pal.accent, Color.white, Mathf.absin(Time.time(), 8f, 1f));
                            Lines.poly(position[0], position[1], 4, size[0] / 2f);
                            Draw.reset();

                            if(!found[0]){
                                size[0] = Mathf.lerpDelta(size[0], 0f, 0.2f);
                            }
                        });
                    }
                }).width(dsize * 4 + 4f);
                editorMain.visible(() -> shown && state.isEditor());
            }

            //fps display
            cont.table(info -> {
                info.top().left().margin(4).visible(() -> Core.settings.getBool("fps"));
                info.update(() -> info.setTranslation(state.rules.waves || state.isEditor() ? 0f : -Scl.scl(dsize * 4 + 3), 0));
                IntFormat fps = new IntFormat("fps");
                IntFormat ping = new IntFormat("ping");

                info.label(() -> fps.get(Core.graphics.getFramesPerSecond())).left().style(Styles.outlineLabel);
                info.row();
                info.label(() -> ping.get(netClient.getPing())).visible(net::client).left().style(Styles.outlineLabel);
            }).top().left();
        });
        
        parent.fill(t -> {
            t.visible(() -> Core.settings.getBool("minimap") && !state.rules.tutorial);
            //minimap
            t.add(new Minimap());
            t.row();
            //position
            t.label(() -> world.toTile(player.x) + "," + world.toTile(player.y))
                .visible(() -> Core.settings.getBool("position") && !state.rules.tutorial);
            t.top().right();
        });

        //spawner warning
        parent.fill(t -> {
            t.touchable(Touchable.disabled);
            t.table(Styles.black, c -> c.add("$nearpoint")
            .update(l -> l.setColor(Tmp.c1.set(Color.white).lerp(Color.scarlet, Mathf.absin(Time.time(), 10f, 1f))))
            .get().setAlignment(Align.center, Align.center))
            .margin(6).update(u -> u.color.a = Mathf.lerpDelta(u.color.a, Mathf.num(spawner.playerNear()), 0.1f)).get().color.a = 0f;
        });

        parent.fill(t -> {
            t.visible(() -> netServer.isWaitingForPlayers());
            t.table(Tex.button, c -> c.add("$waiting.players"));
        });

        //'core is under attack' table
        parent.fill(t -> {
            t.touchable(Touchable.disabled);
            float notifDuration = 240f;

            Events.on(StateChangeEvent.class, event -> {
                if(event.to == State.menu || event.from == State.menu){
                    coreAttackTime = 0f;
                    lastCoreHP = Float.NaN;
                }
            });

            t.top().visible(() -> {
                if(state.is(State.menu) || state.teams.get(player.getTeam()).cores.size == 0 || state.teams.get(player.getTeam()).cores.first().entity == null){
                    coreAttackTime = 0f;
                    return false;
                }

                float curr = state.teams.get(player.getTeam()).cores.first().entity.health;

                if(lastTeam != player.getTeam()){
                    lastCoreHP = curr;
                    lastTeam = player.getTeam();
                    return false;
                }

                if(!Float.isNaN(lastCoreHP) && curr < lastCoreHP){
                    coreAttackTime = notifDuration;
                }
                lastCoreHP = curr;

                t.getColor().a = coreAttackOpacity;
                if(coreAttackTime > 0){
                    coreAttackOpacity = Mathf.lerpDelta(coreAttackOpacity, 1f, 0.1f);
                }else{
                    coreAttackOpacity = Mathf.lerpDelta(coreAttackOpacity, 0f, 0.1f);
                }

                coreAttackTime -= Time.delta();
                lastTeam = player.getTeam();

                return coreAttackOpacity > 0;
            });
            t.table(Tex.button, top -> top.add("$coreattack").pad(2)
            .update(label -> label.getColor().set(Color.orange).lerp(Color.scarlet, Mathf.absin(Time.time(), 2f, 1f)))).touchable(Touchable.disabled);
        });

        //tutorial text
        parent.fill(t -> {
            Runnable resize = () -> {
                t.clearChildren();
                t.top().right().visible(() -> state.rules.tutorial);
                t.stack(new Button(){{
                    marginLeft(48f);
                    labelWrap(() -> control.tutorial.stage.text() + (control.tutorial.canNext() ? "\n\n" + Core.bundle.get("tutorial.next") : "")).width(!Core.graphics.isPortrait() ? 400f : 160f).pad(2f);
                    clicked(() -> control.tutorial.nextSentence());
                    setDisabled(() -> !control.tutorial.canNext());
                }},
                new Table(f -> {
                    f.left().addImageButton(Icon.arrowLeftSmall, Styles.emptyi, () -> {
                        control.tutorial.prevSentence();
                    }).width(44f).growY().visible(() -> control.tutorial.canPrev());
                }));
            };

            resize.run();
            Events.on(ResizeEvent.class, e -> resize.run());
        });

        //paused table
        parent.fill(t -> {
            t.top().visible(() -> state.isPaused()).touchable(Touchable.disabled);
            t.table(Tex.buttonTrans, top -> top.add("$paused").pad(5f));
        });

        //'saving' indicator
        parent.fill(t -> {
            t.bottom().visible(() -> control.saves.isSaving());
            t.add("$saveload").style(Styles.outlineLabel);
        });

        blockfrag.build(parent);
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.both)
    public static void setPlayerTeamEditor(Player player, Team team){
        if(state.isEditor()){
            player.setTeam(team);
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void spawnUnitEditor(Player player, UnitType type){
        if(state.isEditor()){
            BaseUnit unit = type.create(player.getTeam());
            unit.set(player.x, player.y);
            unit.rotation = player.rotation;
            unit.add();
        }
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void removeUnitEditor(Player player, BaseUnit unit){
        if(state.isEditor() && unit != null){
            unit.remove();
        }
    }

    private void scheduleToast(Runnable run){
        long duration = (int)(3.5 * 1000);
        long since = Time.timeSinceMillis(lastToast);
        if(since > duration){
            lastToast = Time.millis();
            run.run();
        }else{
            Time.runTask((duration - since) / 1000f * 60f, run);
            lastToast += duration;
        }
    }

    public void showToast(String text){
        if(state.is(State.menu)) return;

        scheduleToast(() -> {
            Sounds.message.play();

            Table table = new Table(Tex.button);
            table.update(() -> {
                if(state.is(State.menu)){
                    table.remove();
                }
            });
            table.margin(12);
            table.addImage(Icon.check).pad(3);
            table.add(text).wrap().width(280f).get().setAlignment(Align.center, Align.center);
            table.pack();

            //create container table which will align and move
            Table container = Core.scene.table();
            container.top().add(table);
            container.setTranslation(0, table.getPrefHeight());
            container.actions(Actions.translateBy(0, -table.getPrefHeight(), 1f, Interpolation.fade), Actions.delay(2.5f),
            //nesting actions() calls is necessary so the right prefHeight() is used
            Actions.run(() -> container.actions(Actions.translateBy(0, table.getPrefHeight(), 1f, Interpolation.fade), Actions.remove())));
        });
    }

    public boolean shown(){
        return shown;
    }

    /** Show unlock notification for a new recipe. */
    public void showUnlock(UnlockableContent content){
        //some content may not have icons... yet
        //also don't play in the tutorial to prevent confusion
        if(state.is(State.menu) || state.rules.tutorial) return;

        Sounds.message.play();

        //if there's currently no unlock notification...
        if(lastUnlockTable == null){
            scheduleToast(() -> {
                Table table = new Table(Tex.button);
                table.update(() -> {
                    if(state.is(State.menu)){
                        table.remove();
                        lastUnlockLayout = null;
                        lastUnlockTable = null;
                    }
                });
                table.margin(12);

                Table in = new Table();

                //create texture stack for displaying
                Image image = new Image(content.icon(Cicon.xlarge));
                image.setScaling(Scaling.fit);

                in.add(image).size(8 * 6).pad(2);

                //add to table
                table.add(in).padRight(8);
                table.add("$unlocked");
                table.pack();

                //create container table which will align and move
                Table container = Core.scene.table();
                container.top().add(table);
                container.setTranslation(0, table.getPrefHeight());
                container.actions(Actions.translateBy(0, -table.getPrefHeight(), 1f, Interpolation.fade), Actions.delay(2.5f),
                //nesting actions() calls is necessary so the right prefHeight() is used
                Actions.run(() -> container.actions(Actions.translateBy(0, table.getPrefHeight(), 1f, Interpolation.fade), Actions.run(() -> {
                    lastUnlockTable = null;
                    lastUnlockLayout = null;
                }), Actions.remove())));

                lastUnlockTable = container;
                lastUnlockLayout = in;
            });
        }else{
            //max column size
            int col = 3;
            //max amount of elements minus extra 'plus'
            int cap = col * col - 1;

            //get old elements
            Array<Element> elements = new Array<>(lastUnlockLayout.getChildren());
            int esize = elements.size;

            //...if it's already reached the cap, ignore everything
            if(esize > cap) return;

            //get size of each element
            float size = 48f / Math.min(elements.size + 1, col);

            lastUnlockLayout.clearChildren();
            lastUnlockLayout.defaults().size(size).pad(2);

            for(int i = 0; i < esize; i++){
                lastUnlockLayout.add(elements.get(i));

                if(i % col == col - 1){
                    lastUnlockLayout.row();
                }
            }

            //if there's space, add it
            if(esize < cap){

                Image image = new Image(content.icon(Cicon.medium));
                image.setScaling(Scaling.fit);

                lastUnlockLayout.add(image);
            }else{ //else, add a specific icon to denote no more space
                lastUnlockLayout.addImage(Icon.add);
            }

            lastUnlockLayout.pack();
        }
    }

    public void showLaunch(){
        Image image = new Image();
        image.getColor().a = 0f;
        image.setFillParent(true);
        image.actions(Actions.fadeIn(40f / 60f));
        image.update(() -> {
            if(state.is(State.menu)){
                image.remove();
            }
        });
        Core.scene.add(image);
    }

    public void showLand(){
        Image image = new Image();
        image.getColor().a = 1f;
        image.touchable(Touchable.disabled);
        image.setFillParent(true);
        image.actions(Actions.fadeOut(0.8f), Actions.remove());
        image.update(() -> {
            image.toFront();
            if(state.is(State.menu)){
                image.remove();
            }
        });
        Core.scene.add(image);
    }

    private void showLaunchConfirm(){
        FloatingDialog dialog = new FloatingDialog("$launch");
        dialog.update(() -> {
            if(!inLaunchWave()){
                dialog.hide();
            }
        });
        dialog.cont.add("$launch.confirm").width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.addButton("$cancel", dialog::hide);
        dialog.buttons.addButton("$ok", () -> {
            dialog.hide();
            Call.launchZone();
        });
        dialog.keyDown(KeyCode.ESCAPE, dialog::hide);
        dialog.keyDown(KeyCode.BACK, dialog::hide);
        dialog.show();
    }

    private boolean inLaunchWave(){
        return world.isZone() &&
            world.getZone().metCondition() &&
            !net.client() &&
            state.wave % world.getZone().launchPeriod == 0 && !spawner.isSpawning();
    }

    private boolean canLaunch(){
        return inLaunchWave() && state.enemies() <= 0;
    }

    private void toggleMenus(){
        if(flip != null){
            flip.getStyle().imageUp = shown ? Icon.arrowDown : Icon.arrowUp;
        }

        shown = !shown;
    }

    private void addWaveTable(Button table){
        StringBuilder ibuild = new StringBuilder();

        IntFormat wavef = new IntFormat("wave");
        IntFormat enemyf = new IntFormat("wave.enemy");
        IntFormat enemiesf = new IntFormat("wave.enemies");
        IntFormat waitingf = new IntFormat("wave.waiting", i -> {
            ibuild.setLength(0);
            int m = i/60;
            int s = i % 60;
            if(m <= 0){
                ibuild.append(s);
            }else{
                ibuild.append(m);
                ibuild.append(":");
                if(s < 10){
                    ibuild.append("0");
                }
                ibuild.append(s);
            }
            return ibuild.toString();
        });

        table.clearChildren();
        table.touchable(Touchable.enabled);

        StringBuilder builder = new StringBuilder();

        table.setName("waves");
        table.labelWrap(() -> {
            builder.setLength(0);
            builder.append(wavef.get(state.wave));
            builder.append("\n");

            if(inLaunchWave()){
                builder.append("[#");
                Tmp.c1.set(Color.white).lerp(state.enemies() > 0 ? Color.white : Color.scarlet, Mathf.absin(Time.time(), 2f, 1f)).toString(builder);
                builder.append("]");

                if(!canLaunch()){
                    builder.append(Core.bundle.get("launch.unable2"));
                }else{
                    builder.append(Core.bundle.get("launch"));
                    builder.append("\n");
                    builder.append(Core.bundle.format("launch.next", state.wave + world.getZone().launchPeriod));
                    builder.append("\n");
                }
                builder.append("[]\n");
            }

            if(state.enemies() > 0){
                if(state.enemies() == 1){
                    builder.append(enemyf.get(state.enemies()));
                }else{
                    builder.append(enemiesf.get(state.enemies()));
                }
                builder.append("\n");
            }

            if(state.rules.waveTimer){
                builder.append((state.rules.waitForWaveToEnd && unitGroups[waveTeam.ordinal()].size() > 0) ? Core.bundle.get("wave.waveInProgress") : ( waitingf.get((int)(state.wavetime/60))));
            }else if(state.enemies() == 0){
                builder.append(Core.bundle.get("waiting"));
            }

            return builder;
        }).growX().pad(8f);

        table.setDisabled(() -> !canLaunch());
        table.visible(() -> state.rules.waves);
        table.clicked(() -> {
            if(canLaunch()){
                showLaunchConfirm();
            }
        });
    }

    private boolean canSkipWave(){
        return state.rules.waves && ((net.server() || player.isAdmin) || !net.active()) && state.enemies() == 0 && !spawner.isSpawning() && !state.rules.tutorial;
    }

    private void addPlayButton(Table table){
        table.right().addImageButton(Icon.playSmaller, Styles.righti, 30f, () -> {
            if(net.client() && player.isAdmin){
                Call.onAdminRequest(player, AdminAction.wave);
            }else if(inLaunchWave()){
                ui.showConfirm("$confirm", "$launch.skip.confirm", () -> !canSkipWave(), () -> state.wavetime = 0f);
            }else{
                state.wavetime = 0f;
            }
        }).growY().fillX().right().width(40f)
        .visible(this::canSkipWave);
    }
}
