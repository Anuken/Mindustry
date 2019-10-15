package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.actions.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Styles;

import static io.anuke.arc.Core.assets;
import static io.anuke.mindustry.Vars.*;

public class MenuFragment extends Fragment{
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;

    public MenuFragment(){
        assets.load("sprites/logo.png", Texture.class);
        assets.finishLoading();
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
            parent.fill(c -> c.bottom().left().addButton("", Styles.infot, ui.about::show).size(84, 45));
            parent.fill(c -> c.bottom().right().addButton("", Styles.discordt, ui.discord::show).size(84, 45));
        }

        String versionText = "[#ffffffba]" + ((Version.build == -1) ? "[#fc8140aa]custom build" : (Version.type.equals("official") ? Version.modifier : Version.type) + " build " + Version.build + (Version.revision == 0 ? "" : "." + Version.revision));

        parent.fill((x, y, w, h) -> {
            Texture logo = Core.assets.get("sprites/logo.png");
            float logoscl = Scl.scl(1);
            float logow = Math.min(logo.getWidth() * logoscl, Core.graphics.getWidth() - Scl.scl(20));
            float logoh = logow * (float)logo.getHeight() / logo.getWidth();

            float fx = (int)(Core.graphics.getWidth() / 2f);
            float fy = (int)(Core.graphics.getHeight() - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);

            Draw.color();
            Draw.rect(Draw.wrap(logo), fx, fy, logow, logoh);

            Fonts.def.setColor(Color.white);
            Fonts.def.draw(versionText, fx, fy - logoh/2f, Align.center);
        }).touchable(Touchable.disabled);
    }

    private void buildMobile(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        container.defaults().size(size).pad(5).padTop(4f);

        MobileButton
            play = new MobileButton(Icon.play2, "$campaign", ui.deploy::show),
            custom = new MobileButton(Icon.playCustom, "$customgame", ui.custom::show),
            maps = new MobileButton(Icon.load, "$loadgame", ui.load::show),
            join = new MobileButton(Icon.add, "$joingame", ui.join::show),
            editor = new MobileButton(Icon.editor, "$editor", ui.maps::show),
            tools = new MobileButton(Icon.tools, "$settings", ui.settings::show),
            donate = new MobileButton(Icon.link, "$website", () -> Core.net.openURI("https://anuke.itch.io/mindustry")),
            exit = new MobileButton(Icon.exit, "$quit", () -> Core.app.exit());

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

                if(platform.canDonate()) table.add(donate);
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

                if(platform.canDonate()) table.add(donate);
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
                new Buttoni("$play", Icon.play2Small,
                    new Buttoni("$campaign", Icon.play2Small, ui.deploy::show),
                    new Buttoni("$joingame", Icon.addSmall, ui.join::show),
                    new Buttoni("$customgame", Icon.editorSmall, ui.custom::show),
                    new Buttoni("$loadgame", Icon.loadSmall, ui.load::show),
                    new Buttoni("$tutorial", Icon.infoSmall, control::playTutorial)
                ),
                new Buttoni("$editor", Icon.editorSmall, ui.maps::show), steam ? new Buttoni("$workshop", Icon.saveSmall, platform::openWorkshop) : null,
                new Buttoni(Core.bundle.get("mods") + "\n" + Core.bundle.get("mods.alpha"), Icon.wikiSmall, ui.mods::show),
                new Buttoni("$settings", Icon.toolsSmall, ui.settings::show),
                new Buttoni("$about.button", Icon.infoSmall, ui.about::show),
                new Buttoni("$quit", Icon.exitSmall, Core.app::exit)
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
            if(b == null) continue;
            Button[] out = {null};
            out[0] = t.addImageTextButton(b.text, b.icon, Styles.clearToggleMenut, () -> {
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

    private class Buttoni{
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
