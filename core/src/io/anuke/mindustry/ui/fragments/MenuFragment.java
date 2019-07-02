package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.Button;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.core.GameState.State;
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

        Core.scene.table().addRect((a, b, w, h) -> {
            renderer.render();
        });

        parent.fill(c -> {
            container = c;
            container.visible(() -> state.is(State.menu));

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

        Core.scene.table().addRect((a, b, w, h) -> {
            //Draw.shader(Shaders.menu);
            // Fill.crect(0, 0, w, h);
            //Draw.shader();

            boolean portrait = Core.graphics.getWidth() < Core.graphics.getHeight();
            float logoscl = (int)Unit.dp.scl(1);
            float logow = Math.min(Math.min(logo.getWidth() * logoscl, 768), Core.graphics.getWidth() - 10);
            float logoh = logow * (float)logo.getHeight() / logo.getWidth();

            Draw.color();
            Draw.rect(Draw.wrap(logo), (int)(w / 2), (int)(h - 10 - logoh) + logoh / 2, logow, logoh);
        }).visible(() -> state.is(State.menu)).grow().get().touchable(Touchable.disabled);
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

            /*
            t.addImageTextButton("$play", "icon-play-2" + suffix, "clear", isize, ui.deploy::show).marginLeft(margin);
            t.row();

            t.addImageTextButton("$joingame", "icon-add" + suffix, "clear", isize, ui.join::show).marginLeft(margin);
            t.row();

            t.addImageTextButton("$customgame", "icon-play-custom" + suffix, "clear", isize, this::showCustomSelect).marginLeft(margin);
            t.row();*/

        }).width(width).growY();

        container.table(background, t -> {
            submenu = t;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());

        }).width(width).growY();

        /*
        container.table(out -> {

            float w = 200f;
            float bw = w * 2f + 10f;

            out.margin(16);
            out.defaults().size(w, 66f).padTop(5).padRight(5);

            out.add(new MenuButton("icon-play-2", "$play", ui.deploy::show)).width(bw).colspan(2);

            out.row();

            out.add(new MenuButton("icon-add", "$joingame", ui.join::show));

            out.add(new MenuButton("icon-play-custom", "$customgame", this::showCustomSelect));

            out.row();

            out.add(new MenuButton("icon-editor", "$editor", () -> ui.loadAnd(ui.editor::show)));

            out.add(new MenuButton("icon-map", "$maps", ui.maps::show));

            out.row();

            out.add(new MenuButton("icon-info", "$about.button", ui.about::show));

            out.add(new MenuButton("icon-tools", "$settings", ui.settings::show));

            out.row();

            out.add(new MenuButton("icon-exit", "$quit", Core.app::exit)).width(bw).colspan(2);
        });*/
    }

    private void buttons(Table t, Buttoni... buttons){
        for(Buttoni b : buttons){
            Button[] out = {null};
            out[0] = t.addImageTextButton(b.text, b.icon + "-small", "clear-toggle-menu",
                    iconsizesmall, () -> {
                if(currentMenu == out[0]){
                    currentMenu = null;
                    submenu.clearChildren();
                }else{
                    if(b.submenu != null){
                        currentMenu = out[0];
                        submenu.clearChildren();
                        //correctly offset the button
                        submenu.add().height(Core.graphics.getHeight() - out[0].getY(Align.topLeft));
                        submenu.row();
                        buttons(submenu, b.submenu);
                    }else{
                        currentMenu = null;
                        submenu.clearChildren();
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
