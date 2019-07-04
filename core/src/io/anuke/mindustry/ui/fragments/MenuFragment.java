package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.Button;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.graphics.MenuRenderer;
import io.anuke.mindustry.ui.MenuButton;
import io.anuke.mindustry.ui.MobileButton;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

import static io.anuke.mindustry.Vars.*;

public class MenuFragment extends Fragment{
    private Texture logo = new Texture("sprites/logo.png");
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;

    @Override
    public void build(Group parent){
        renderer = new MenuRenderer();

        parent.fill((x, y, w, h) -> {
            renderer.render();
        });

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
        //if(mobile){
        //    parent.fill(c -> c.top().left().addButton("", "info", ui.about::show).size(84, 45)
        //    .visible(() -> state.is(State.menu)));
        //}

        //version info
        //parent.fill(c -> c.bottom().left().add(Strings.format("v{0} {1}-{2} {3}{4}", Version.number, Version.modifier, Version.type,
        //(Version.build == -1 ? "custom build" : "build " + Version.build), Version.revision == 0 ? "" : "." + Version.revision)).color(Color.DARK_GRAY)
        //.visible(() -> state.is(State.menu)));

        parent.fill((x, y, w, h) -> {
            float logoscl = (int)Unit.dp.scl(1);
            float logow = Math.min(logo.getWidth() * logoscl, Core.graphics.getWidth() - Unit.dp.scl(20));
            float logoh = logow * (float)logo.getHeight() / logo.getWidth();

            Draw.color();
            Draw.rect(Draw.wrap(logo), (int)(Core.graphics.getWidth() / 2), (int)(Core.graphics.getHeight() - 10 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Unit.dp.scl(30f) : 0f), logow, logoh);
        }).touchable(Touchable.disabled);
    }

    private void buildMobile(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        float isize = iconsize;
        container.defaults().size(size).pad(5).padTop(4f);

        MobileButton
        play = new MobileButton("icon-play-2", isize, "$play", ui.deploy::show),
        maps = new MobileButton("icon-map", isize, "$maps", ui.maps::show),
        custom = new MobileButton("icon-play-custom", isize, "$customgame", this::showCustomSelect),
        join = new MobileButton("icon-add", isize, "$joingame", ui.join::show),
        editor = new MobileButton("icon-editor", isize, "$editor", () -> ui.loadAnd(ui.editor::show)),
        tools = new MobileButton("icon-tools", isize, "$settings", ui.settings::show),
        donate = new MobileButton("icon-donate", isize, "$donate", () -> Core.net.openURI(donationURL)),
        exit = new MobileButton("icon-exit", isize, "$quit", () -> Core.app.exit());

        if(!Core.graphics.isPortrait()){
            container.add(play);
            container.add(join);
            container.add(custom);
            container.add(maps);
            container.row();

            container.table(table -> {
                table.defaults().set(container.defaults());

                table.add(editor);
                table.add(tools);

                if(Platform.instance.canDonate()) table.add(donate);
                table.add(exit);
            }).colspan(4);
        }else{
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
                table.add(exit);
            }).colspan(2);
        }
    }

    private void buildDesktop(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());


        float width = 230f;
        String background = "flat";

        container.left();
        container.add().width(Core.graphics.getWidth()/10f);
        container.table(background, t -> {
            t.defaults().width(width).height(70f);

            buttons(t,
                new Buttoni("$play", "icon-play-2",
                    new Buttoni("$campaign", "icon-play-2", ui.deploy::show),
                    new Buttoni("$joingame", "icon-add", ui.join::show),
                    new Buttoni("$customgame", "icon-terrain", ui.custom::show),
                    new Buttoni("$loadgame", "icon-load", ui.load::show)
                ),
                new Buttoni("$editor", "icon-editor", ui.maps::show),
                new Buttoni("$settings", "icon-tools", ui.settings::show), //todo submenu
                new Buttoni("$about.button", "icon-info", ui.about::show), //todo submenu
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
                        submenu.add().height(Core.graphics.getHeight() - out[0].getY(Align.topLeft));
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

    private void showCustomSelect(){
        FloatingDialog dialog = new FloatingDialog("$play");
        dialog.setFillParent(false);
        dialog.addCloseButton();
        dialog.cont.defaults().size(210f, 64f);
        dialog.cont.add(new MenuButton("icon-editor", "$newgame", () -> {
            dialog.hide();
            ui.custom.show();
        }));
        dialog.cont.row();
        dialog.cont.add(new MenuButton("icon-load", "$loadgame", () -> {
            ui.load.show();
            dialog.hide();
        }));
        dialog.show();
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
