package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.Button;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.EventType.DisposeEvent;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.graphics.MenuRenderer;
import io.anuke.mindustry.ui.MobileButton;

import static io.anuke.mindustry.Vars.*;

public class MenuFragment extends Fragment{
    private Texture logo = new Texture("sprites/logo.png");
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;

    public MenuFragment(){
        Events.on(DisposeEvent.class, event -> {
            renderer.dispose();
            logo.dispose();
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

        //discord icon in top right
        //parent.fill(c -> c.top().right().addButton("", "discord", ui.discord::show).size(84, 45)
        //.visible(() -> state.is(State.menu)));

        //info icon
        if(mobile){
            parent.fill(c -> c.bottom().left().addButton("", "info", ui.about::show).size(84, 45));
            parent.fill(c -> c.bottom().right().addButton("", "discord", ui.discord::show).size(84, 45));
        }

        String versionText = "[#ffffffba]" + ((Version.build == -1) ? "[#fc8140aa]custom build" : Version.modifier + " build " + Version.build);

        parent.fill((x, y, w, h) -> {
            float logoscl = UnitScl.dp.scl(1);
            float logow = Math.min(logo.getWidth() * logoscl, Core.graphics.getWidth() - UnitScl.dp.scl(20));
            float logoh = logow * (float)logo.getHeight() / logo.getWidth();

            float fx = (int)(Core.graphics.getWidth() / 2f);
            float fy = (int)(Core.graphics.getHeight() - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? UnitScl.dp.scl(30f) : 0f);

            Draw.color();
            Draw.rect(Draw.wrap(logo), fx, fy, logow, logoh);
            Core.scene.skin.font().setColor(Color.WHITE);
            Core.scene.skin.font().draw(versionText, fx, fy - logoh/2f, Align.center);
        }).touchable(Touchable.disabled);
    }

    private void buildMobile(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        float isize = iconsize;
        container.defaults().size(size).pad(5).padTop(4f);

        MobileButton
            play = new MobileButton("icon-play-2", isize, "$campaign", ui.deploy::show),
            custom = new MobileButton("icon-play-custom", isize, "$customgame", ui.custom::show),
            maps = new MobileButton("icon-load", isize, "$loadgame", ui.load::show),
            join = new MobileButton("icon-add", isize, "$joingame", ui.join::show),
            editor = new MobileButton("icon-editor", isize, "$editor", ui.maps::show),
            tools = new MobileButton("icon-tools", isize, "$settings", ui.settings::show),
            donate = new MobileButton("icon-link", isize, "$website", () -> Core.net.openURI("https://anuke.itch.io/mindustry")),
            exit = new MobileButton("icon-exit", isize, "$quit", () -> Core.app.exit());

        if(!Core.graphics.isPortrait()){
            container.marginTop(60f);
            container.add(play);
            container.add(join);
            container.add(custom);
            if(ios) container.row();
            container.add(maps);
            if(!ios) container.row();

            container.table(table -> {
                table.defaults().set(container.defaults());

                table.add(editor);
                table.add(tools);

                if(Platform.instance.canDonate()) table.add(donate);
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

                if(Platform.instance.canDonate()) table.add(donate);
                if(!ios) table.add(exit);
            }).colspan(2);
        }
    }

    private void buildDesktop(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());


        float width = 230f;
        String background = "flat-trans";

        container.left();
        container.add().width(Core.graphics.getWidth()/10f);
        container.table(background, t -> {
            t.defaults().width(width).height(70f);

            buttons(t,
                new Buttoni("$play", "icon-play-2",
                    new Buttoni("$campaign", "icon-play-2", ui.deploy::show),
                    new Buttoni("$joingame", "icon-add", ui.join::show),
                    new Buttoni("$customgame", "icon-editor", ui.custom::show),
                    new Buttoni("$loadgame", "icon-load", ui.load::show),
                    new Buttoni("$tutorial", "icon-info", control::playTutorial)
                ),
                new Buttoni("$editor", "icon-editor", ui.maps::show),
                new Buttoni("$settings", "icon-tools", ui.settings::show),
                new Buttoni("$about.button", "icon-info", ui.about::show),
                new Buttoni("$quit", "icon-exit", Core.app::exit)
            );

        }).width(width).growY();

        container.table(background, t -> {
            submenu = t;
            t.getColor().a = 0f;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());

        }).width(width).growY();
    }

    private void fadeInMenu(){
        submenu.clearActions();
        submenu.actions(Actions.alpha(1f, 0.15f, Interpolation.fade));
    }

    private void fadeOutMenu(){
        //nothing to fade out
        if(submenu.getChildren().isEmpty()){
            return;
        }

        submenu.clearActions();
        submenu.actions(Actions.alpha(1f), Actions.alpha(0f, 0.2f, Interpolation.fade), Actions.run(() -> submenu.clearChildren()));
    }

    private void buttons(Table t, Buttoni... buttons){
        for(Buttoni b : buttons){
            Button[] out = {null};
            out[0] = t.addImageTextButton(b.text, b.icon + "-small", "clear-toggle-menu",
                    iconsizesmall, () -> {
                if(currentMenu == out[0]){
                    currentMenu = null;
                    fadeOutMenu();
                }else{
                    if(b.submenu != null){
                        currentMenu = out[0];
                        submenu.clearChildren();
                        fadeInMenu();
                        //correctly offset the button
                        submenu.add().height((Core.graphics.getHeight() - out[0].getY(Align.topLeft)) / UnitScl.dp.scl(1f));
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

    private class Buttoni{
        final String icon;
        final String text;
        final Runnable runnable;
        final Buttoni[] submenu;

        public Buttoni(String text, String icon, Runnable runnable){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = null;
        }

        public Buttoni(String text, String icon, Buttoni... buttons){
            this.icon = icon;
            this.text = text;
            this.runnable = () -> {};
            this.submenu = buttons;
        }
    }
}
