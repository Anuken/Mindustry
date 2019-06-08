package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.ui.MenuButton;
import io.anuke.mindustry.ui.MobileButton;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

import static io.anuke.mindustry.Vars.*;

public class MenuFragment extends Fragment{
    private Table container;

    @Override
    public void build(Group parent){
        parent.fill(c -> {
            container = c;
            container.visible(() -> state.is(State.menu));

            if(!mobile){
                buildDesktop();
            }else{
                buildMobile();
                Events.on(ResizeEvent.class, event -> buildMobile());
            }
        });

        //discord icon in top right
        parent.fill(c -> c.top().right().addButton("", "discord", ui.discord::show).size(84, 45)
        .visible(() -> state.is(State.menu)));

        //info icon
        if(mobile){
            parent.fill(c -> c.top().left().addButton("", "info", ui.about::show).size(84, 45)
            .visible(() -> state.is(State.menu)));
        }

        //version info
        parent.fill(c -> c.bottom().left().add(Strings.format("Mindustry v{0} {1}-{2} {3}{4}", Version.number, Version.modifier, Version.type,
        (Version.build == -1 ? "custom build" : "build " + Version.build), Version.revision == 0 ? "" : "." + Version.revision))
        .visible(() -> state.is(State.menu)));
    }

    private void buildMobile(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        float isize = 14f * 4;
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
        });
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
}
