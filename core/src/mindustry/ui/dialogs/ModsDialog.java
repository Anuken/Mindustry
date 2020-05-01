package mindustry.ui.dialogs;

import arc.*;
import arc.Net.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.TextButton.*;
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

        buttons.button("$mods.guide", Icon.link, () -> Core.net.openURI(modGuideURL)).size(210, 64f);


        shown(this::setup);

        hidden(() -> {
            if(mods.requiresReload()){
                reload();
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
        float h = 110f;
        float w = mobile ? 430f : 524f;

        cont.clear();
        cont.defaults().width(mobile ? 500 : 560f).pad(4);
        cont.add("$mod.reloadrequired").visible(mods::requiresReload).center().get().setAlignment(Align.center);
        cont.row();

        cont.table(buttons -> {
            buttons.left().defaults().growX().height(60f).uniformX();

            TextButtonStyle style = Styles.clearPartialt;
            float margin = 12f;

            buttons.button("$mod.import", Icon.add, style, () -> {
                FloatingDialog dialog = new FloatingDialog("$mod.import");

                TextButtonStyle bstyle = Styles.cleart;

                dialog.cont.table(Tex.button, t -> {
                    t.defaults().size(300f, 70f);
                    t.margin(12f);

                    t.button("$mod.import.file", Icon.file, bstyle, () -> {
                        dialog.hide();

                        platform.showFileChooser(true, "zip", file -> {
                            try{
                                mods.importMod(file);
                                setup();
                            }catch(IOException e){
                                ui.showException(e);
                                e.printStackTrace();
                            }
                        });
                    }).margin(12f);

                    t.row();

                    t.button("$mod.import.github", Icon.github, bstyle, () -> {
                        dialog.hide();

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
                                }, t2 -> Core.app.post(() -> modError(t2)));
                            }, t2 -> Core.app.post(() -> modError(t2)));
                        });
                    }).margin(12f);
                });

                dialog.addCloseButton();

                dialog.show();
            }).margin(margin);

            buttons.button("$mods.reload", Icon.refresh, style, this::reload).margin(margin);

            if(!mobile){
                buttons.button("$mods.openfolder", Icon.link, style, () -> Core.app.openFolder(modDirectory.absolutePath())).margin(margin);
            }
        }).width(w);

        cont.row();

        if(!mods.list().isEmpty()){
            cont.pane(table -> {
                table.margin(10f).top();

                boolean anyDisabled = false;
                for(LoadedMod mod : mods.list()){
                    String letter = (Strings.stripColors(mod.name).charAt(0) + "").toUpperCase();

                    if(!mod.enabled() && !anyDisabled && mods.list().size > 0){
                        anyDisabled = true;
                        table.row();
                        table.image().growX().height(4f).pad(6f).color(Pal.gray);
                        table.row();
                    }

                    table.button(t -> {
                        t.top().left();
                        t.margin(12f);

                        t.add(new BorderImage(){
                            {
                                if(mod.iconTexture != null){
                                    setDrawable(new TextureRegion(mod.iconTexture));
                                }else{
                                    setDrawable(Tex.clear);
                                }
                            }

                            @Override
                            public void draw(){
                                super.draw();

                                if(mod.iconTexture == null){
                                    Fonts.def.draw(letter, x + width/2f, y + height/2f, Align.center);
                                }
                            }
                        }.border(Pal.accent)).size(h - 8f).padTop(-8f).padLeft(-8f).padRight(2f);

                        t.defaults().left().top();
                        t.table(title -> {
                            title.left();
                            title.add("" + mod.meta.displayName() + "\n[lightgray]v" + mod.meta.version + (mod.enabled() ? "" : "\n" + Core.bundle.get("mod.disabled") + "")).wrap().width(170f).growX();
                            title.add().growX();

                            title.button(mod.enabled() ? "$mod.disable" : "$mod.enable", mod.enabled() ? Icon.downOpen : Icon.upOpen, Styles.transt, () -> {
                                mods.setEnabled(mod, !mod.enabled());
                                setup();
                            }).height(50f).margin(8f).width(130f).disabled(!mod.isSupported());

                            if(steam && !mod.hasSteamID()){
                                title.button(Icon.download, Styles.clearTransi, () -> {
                                    platform.publish(mod);
                                }).size(50f);
                            }

                            title.button(mod.hasSteamID() ? Icon.link : Icon.trash, Styles.clearPartiali, () -> {
                                if(!mod.hasSteamID()){
                                    ui.showConfirm("$confirm", "$mod.remove.confirm", () -> {
                                        mods.removeMod(mod);
                                        setup();
                                    });
                                }else{
                                    platform.viewListing(mod);
                                }
                            }).size(50f);
                        }).growX().left();

                        t.row();
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
                    }, Styles.clearPartialt, () -> showMod(mod)).size(w, h);
                    table.row();
                }
            });

        }else{
            cont.table(Styles.black6, t -> t.add("$mods.none")).height(80f);
        }

        cont.row();


    }

    private void reload(){
        ui.loadAnd("$reloading", () -> {
            mods.eachEnabled(mod -> {
                if(mod.hasUnmetDependencies()){
                    ui.showErrorMessage(Core.bundle.format("mod.nowdisabled", mod.name, mod.missingDependencies.toString(", ")));
                }
            });
            mods.reloadContent();
            setup();
        });
    }

    private void showMod(LoadedMod mod){
        FloatingDialog dialog = new FloatingDialog(mod.meta.displayName());

        dialog.addCloseButton();

        if(!mobile){
            dialog.buttons.button("$mods.openfolder", Icon.link, () -> Core.app.openFolder(mod.file.absolutePath()));
        }

        //TODO improve this menu later
        dialog.cont.pane(desc -> {
            desc.center();
            desc.defaults().padTop(10).left();

            desc.add("$editor.name").padRight(10).color(Color.gray).padTop(0);
            desc.row();
            desc.add(mod.meta.displayName()).growX().wrap().padTop(2);
            desc.row();
            if(mod.meta.author != null){
                desc.add("$editor.author").padRight(10).color(Color.gray);
                desc.row();
                desc.add(mod.meta.author).growX().wrap().padTop(2);
                desc.row();
            }
            if(mod.meta.description != null){
                desc.add("$editor.description").padRight(10).color(Color.gray).top();
                desc.row();
                desc.add(mod.meta.description).growX().wrap().padTop(2);
            }

            //TODO add this when mods work properly
            /*
            Array<UnlockableContent> all = Array.with(content.getContentMap()).<Content>flatten().select(c -> c.minfo.mod == mod && c instanceof UnlockableContent).as(UnlockableContent.class);
            if(all.any()){
                desc.add("$mod.content").padRight(10).color(Color.gray).top();
                desc.row();
                desc.pane(cs -> {
                    int i = 0;
                    for(UnlockableContent c : all){
                        cs.addImageButton(new TextureRegionDrawable(c.icon(Cicon.medium)), () -> {
                            ui.content.show(c);
                        });

                        if(++i % 8 == 0) cs.row();
                    }
                }).growX().minHeight(60f);
            }*/
        }).width(400f);



        dialog.show();
    }
}
