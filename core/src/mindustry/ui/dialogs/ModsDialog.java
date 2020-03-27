package mindustry.ui.dialogs;

import arc.*;
import arc.Net.*;
import arc.files.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.Mods.*;
import mindustry.ui.*;

import java.io.*;

import static mindustry.Vars.*;

public class ModsDialog extends FloatingDialog{

    public ModsDialog(){
        super("$mods");
        addCloseButton();

        buttons.addImageTextButton("$mods.openfolder", Icon.link,
        () -> Core.app.openFolder(modDirectory.absolutePath())).size(250f, 64f);

        buttons.row();

        buttons.addImageTextButton("$mods.guide", Icon.link,
        () -> Core.net.openURI(modGuideURL))
        .size(210, 64f);

        buttons.addImageTextButton("$mod.import.github", Icon.github, () -> {
            ui.showTextInput("$mod.import.github", "", 64, "Anuken/ExampleMod", text -> {
                ui.loadfrag.show();
                Core.net.httpGet("http://api.github.com/repos/" + text + "/zipball/master", loc -> {
                    Core.net.httpGet(loc.getHeader("Location"), result -> {
                        if(result.getStatus() != HttpStatus.OK){
                            ui.showErrorMessage(Core.bundle.format("connectfail", result.getStatus()));
                            ui.loadfrag.hide();
                        }else{
                            try{
                                Fi file = tmpDirectory.child(text.replace("/", "") + ".zip");
                                Streams.copy(result.getResultAsStream(), file.write(false));
                                mods.importMod(file);
                                file.delete();
                                Core.app.post(() -> {
                                    try{
                                        mods.reloadContent();
                                        setup();
                                        ui.loadfrag.hide();
                                    }catch(Throwable e){
                                        ui.showException(e);
                                    }
                                });
                            }catch(Throwable e){
                                modError(e);
                            }
                        }
                    }, t -> Core.app.post(() -> modError(t)));
                }, t -> Core.app.post(() -> modError(t)));
            });
        }).size(250f, 64f);


        shown(this::setup);

        hidden(() -> {
            if(mods.requiresReload()){
                ui.loadAnd("$reloading", () -> {
                    mods.eachEnabled(mod -> {
                        if(mod.hasUnmetDependencies()){
                            ui.showErrorMessage(Core.bundle.format("mod.nowdisabled", mod.name, mod.missingDependencies.toString(", ")));
                        }
                    });
                    mods.reloadContent();
                });
            }
        });

        shown(() -> Core.app.post(() -> {
            Core.settings.getBoolOnce("modsalpha", () -> {
                ui.showText("$mods", "$mods.alphainfo");
            });
        }));
    }

    void modError(Throwable error){
        ui.loadfrag.hide();

        if(Strings.getCauses(error).contains(t -> t.getMessage() != null && (t.getMessage().contains("trust anchor") || t.getMessage().contains("SSL") || t.getMessage().contains("protocol")))){
            ui.showErrorMessage("$feature.unsupported");
        }else{
            ui.showException(error);
        }
    }

    void setup(){
        cont.clear();
        cont.defaults().width(mobile ? 500 : 560f).pad(4);
        cont.add("$mod.reloadrequired").visible(mods::requiresReload).center().get().setAlignment(Align.center);
        cont.row();
        if(!mods.list().isEmpty()){
            cont.pane(table -> {
                table.margin(10f).top();

                boolean anyDisabled = false;
                for(LoadedMod mod : mods.list()){
                    if(!mod.enabled() && !anyDisabled && mods.list().size > 0){
                        anyDisabled = true;
                        table.row();
                        table.addImage().growX().height(4f).pad(6f).color(Pal.gray);
                        table.row();
                    }

                    table.table(Styles.black6, t -> {
                        t.defaults().pad(2).left().top();
                        t.margin(14f).left();
                        t.table(title -> {
                            title.left();
                            title.add("[accent]" + mod.meta.displayName() + "[lightgray] v" + mod.meta.version + (mod.enabled() ? "" : "\n" + Core.bundle.get("mod.disabled") + "")).width(200f).wrap();
                            title.add().growX();

                            title.addImageTextButton(mod.enabled() ? "$mod.disable" : "$mod.enable", mod.enabled() ? Icon.downOpen : Icon.upOpen, Styles.cleart, () -> {
                                mods.setEnabled(mod, !mod.enabled());
                                setup();
                            }).height(50f).margin(8f).width(130f).disabled(!mod.isSupported());

                            if(steam && !mod.hasSteamID()){
                                title.addImageButton(Icon.download, Styles.cleari, () -> {
                                    platform.publish(mod);
                                }).size(50f);
                            }

                            title.addImageButton(mod.hasSteamID() ? Icon.link : Icon.trash, Styles.cleari, () -> {
                                if(!mod.hasSteamID()){
                                    ui.showConfirm("$confirm", "$mod.remove.confirm", () -> {
                                        mods.removeMod(mod);
                                        setup();
                                    });
                                }else{
                                    platform.viewListing(mod);
                                }
                            }).size(50f);
                        }).growX().left().padTop(-14f).padRight(-14f);

                        t.row();
                        if(mod.meta.author != null){
                            t.add(Core.bundle.format("mod.author", mod.meta.author));
                            t.row();
                        }
                        if(mod.meta.description != null){
                            t.labelWrap("[lightgray]" + mod.meta.description).growX();
                            t.row();
                        }
                        if(!mod.isSupported()){
                            t.labelWrap(Core.bundle.format("mod.requiresversion", mod.meta.minGameVersion)).growX();
                            t.row();
                        }else if(mod.hasUnmetDependencies()){
                            t.labelWrap(Core.bundle.format("mod.missingdependencies", mod.missingDependencies.toString(", "))).growX();
                            t.row();
                        }else if(mod.hasContentErrors()){
                            t.labelWrap("$mod.erroredcontent").growX();
                            t.row();
                        }
                    }).width(mobile ? 430f : 500f);
                    table.row();
                }
            });

        }else{
            cont.table(Styles.black6, t -> t.add("$mods.none")).height(80f);
        }

        cont.row();

        cont.addImageTextButton("$mod.import", Icon.add, () -> {
            platform.showFileChooser(true, "zip", file -> {
                try{
                    mods.importMod(file);
                    setup();
                }catch(IOException e){
                    ui.showException(e);
                    e.printStackTrace();
                }
            });
        }).margin(12f).width(400f);
    }
}
