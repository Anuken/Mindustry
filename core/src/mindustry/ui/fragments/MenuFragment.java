package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MenuFragment extends Fragment{
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;

    public MenuFragment(){
        Events.on(DisposeEvent.class, event -> {
            renderer.dispose();
        });
    }

    @Override
    public void build(Group parent){
        renderer = new MenuRenderer();

        Group group = new WidgetGroup();
        group.setFillParent(true);
        group.visible(() -> !ui.editor.isShown());
        parent.addChild(group);

        parent = group;

        parent.fill((x, y, w, h) -> renderer.render());

        parent.fill(c -> {
            container = c;

            if(!mobile){
                buildDesktop();
                Events.on(ResizeEvent.class, event -> buildDesktop());
            }else{
                buildMobile();
                Events.on(ResizeEvent.class, event -> buildMobile());
            }
        });

        //info icon
        if(mobile){
            parent.fill(c -> c.bottom().left().button("", Styles.infot, ui.about::show).size(84, 45));
            parent.fill(c -> c.bottom().right().button("", Styles.discordt, ui.discord::show).size(84, 45));
        }else if(becontrol.active()){
            parent.fill(c -> c.bottom().right().button("@be.check", Icon.refresh, () -> {
                ui.loadfrag.show();
                becontrol.checkUpdate(result -> {
                    ui.loadfrag.hide();
                    if(!result){
                        ui.showInfo("@be.noupdates");
                    }
                });
            }).size(200, 60).update(t -> {
                t.getLabel().setColor(becontrol.isUpdateAvailable() ? Tmp.c1.set(Color.white).lerp(Pal.accent, Mathf.absin(5f, 1f)) : Color.white);
            }));
        }

        String versionText = "[#ffffffba]" + ((Version.build == -1) ? "[#fc8140aa]custom build" : (Version.type.equals("official") ? Version.modifier : Version.type) + " build " + Version.build + (Version.revision == 0 ? "" : "." + Version.revision));

        parent.fill((x, y, w, h) -> {
            TextureRegion logo = Core.atlas.find("logo");
            float logoscl = Scl.scl(1);
            float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
            float logoh = logow * (float)logo.height / logo.width;

            float fx = (int)(Core.graphics.getWidth() / 2f);
            float fy = (int)(Core.graphics.getHeight() - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);

            Draw.color();
            Draw.rect(logo, fx, fy, logow, logoh);

            Fonts.def.setColor(Color.white);
            Fonts.def.draw(versionText, fx, fy - logoh/2f, Align.center);
        }).touchable = Touchable.disabled;
    }

    private void buildMobile(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        container.defaults().size(size).pad(5).padTop(4f);

        MobileButton
            play = new MobileButton(Icon.play, "@campaign", () -> checkPlay(ui.planet::show)),
            custom = new MobileButton(Icon.rightOpenOut, "@customgame", () -> checkPlay(ui.custom::show)),
            maps = new MobileButton(Icon.download, "@loadgame", () -> checkPlay(ui.load::show)),
            join = new MobileButton(Icon.add, "@joingame", () -> checkPlay(ui.join::show)),
            editor = new MobileButton(Icon.terrain, "@editor", () -> checkPlay(ui.maps::show)),
            tools = new MobileButton(Icon.settings, "@settings", ui.settings::show),
            mods = new MobileButton(Icon.book, "@mods", ui.mods::show),
            donate = new MobileButton(Icon.link, "@website", () -> Core.app.openURI("https://anuke.itch.io/mindustry")),
            exit = new MobileButton(Icon.exit, "@quit", () -> Core.app.exit());

        if(!Core.graphics.isPortrait()){
            container.marginTop(60f);
            container.add(play);
            container.add(join);
            container.add(custom);
            container.add(maps);
            container.row();

            container.table(table -> {
                table.defaults().set(container.defaults());

                table.add(editor);
                table.add(tools);

                table.add(mods);
                //if(platform.canDonate()) table.add(donate);
                if(!ios) table.add(exit);
            }).colspan(4);
        }else{
            container.marginTop(0f);
            container.add(play);
            container.add(maps);
            container.row();
            container.add(custom);
            container.add(join);
            container.row();
            container.add(editor);
            container.add(tools);
            container.row();

            container.table(table -> {
                table.defaults().set(container.defaults());

                table.add(mods);
                //if(platform.canDonate()) table.add(donate);
                if(!ios) table.add(exit);
            }).colspan(2);
        }
    }

    private void buildDesktop(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());


        float width = 230f;
        Drawable background = Styles.black6;

        container.left();
        container.add().width(Core.graphics.getWidth()/10f);
        container.table(background, t -> {
            t.defaults().width(width).height(70f);

            buttons(t,
                new Buttoni("@play", Icon.play,
                    new Buttoni("@campaign", Icon.play, () -> checkPlay(ui.planet::show)),
                    new Buttoni("@joingame", Icon.add, () -> checkPlay(ui.join::show)),
                    new Buttoni("@customgame", Icon.terrain, () -> checkPlay(ui.custom::show)),
                    new Buttoni("@loadgame", Icon.download, () -> checkPlay(ui.load::show)),
                    new Buttoni("@tutorial", Icon.info, () -> checkPlay(control::playTutorial))
                ),
                new Buttoni("@editor", Icon.terrain, () -> checkPlay(ui.maps::show)), steam ? new Buttoni("@workshop", Icon.book, platform::openWorkshop) : null,
                new Buttoni(Core.bundle.get("mods"), Icon.bookOpen, ui.mods::show),
                //not enough space for this button
                //new Buttoni("@schematics", Icon.paste, ui.schematics::show),
                new Buttoni("@settings", Icon.settings, ui.settings::show),
                new Buttoni("@about.button", Icon.info, ui.about::show),
                new Buttoni("@quit", Icon.exit, Core.app::exit)
            );

        }).width(width).growY();

        container.table(background, t -> {
            submenu = t;
            t.color.a = 0f;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());

        }).width(width).growY();
    }

    private void checkPlay(Runnable run){
        if(!mods.hasContentErrors()){
            run.run();
        }else{
            ui.showInfo("@mod.noerrorplay");
        }
    }

    private void fadeInMenu(){
        submenu.clearActions();
        submenu.actions(Actions.alpha(1f, 0.15f, Interp.fade));
    }

    private void fadeOutMenu(){
        //nothing to fade out
        if(submenu.getChildren().isEmpty()){
            return;
        }

        submenu.clearActions();
        submenu.actions(Actions.alpha(1f), Actions.alpha(0f, 0.2f, Interp.fade), Actions.run(() -> submenu.clearChildren()));
    }

    private void buttons(Table t, Buttoni... buttons){
        for(Buttoni b : buttons){
            if(b == null) continue;
            Button[] out = {null};
            out[0] = t.button(b.text, b.icon, Styles.clearToggleMenut, () -> {
                if(currentMenu == out[0]){
                    currentMenu = null;
                    fadeOutMenu();
                }else{
                    if(b.submenu != null){
                        currentMenu = out[0];
                        submenu.clearChildren();
                        fadeInMenu();
                        //correctly offset the button
                        submenu.add().height((Core.graphics.getHeight() - out[0].getY(Align.topLeft)) / Scl.scl(1f));
                        submenu.row();
                        buttons(submenu, b.submenu);
                    }else{
                        currentMenu = null;
                        fadeOutMenu();
                        b.runnable.run();
                    }
                }
            }).marginLeft(11f).get();
            out[0].update(() -> out[0].setChecked(currentMenu == out[0]));
            t.row();
        }
    }

    private static class Buttoni{
        final Drawable icon;
        final String text;
        final Runnable runnable;
        final Buttoni[] submenu;

        public Buttoni(String text, Drawable icon, Runnable runnable){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = null;
        }

        public Buttoni(String text, Drawable icon, Buttoni... buttons){
            this.icon = icon;
            this.text = text;
            this.runnable = () -> {};
            this.submenu = buttons;
        }
    }
}
