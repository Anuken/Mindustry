package mindustry.editor.data;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.mod.data.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapAudioView implements AssetView{
    public final DataAssetType type;
    public @Nullable AudioSource lastPlaying;

    public MapAudioView(DataAssetType type){
        this.type = type;
    }

    @Override
    public void build(MapAssetsDialog diag, Table list){
        var assets = state.data.getAssets(type);

        float h = 50f;

        list.defaults().pad(4f);
        for(var asset : assets){
            if(diag.searchString != null && !asset.name.toLowerCase().contains(diag.searchString)) continue;

            Fi file = asset.getCacheFile();
            var audioSource = (AudioSource)Core.assets.getOrNull(DataAudioLoader.prefix + asset.name, (Class<?>)(type == DataAssetType.music ? Music.class : Sound.class));

            if(file == null || audioSource == null || !audioSource.valid()){
                list.button(Icon.warning, Styles.graySquarei, iconMed, () -> ui.showInfo("@asset.broken")).size(h);
            }else{
                list.button(Icon.play, Styles.graySquarei, iconMed, () -> {
                    if(lastPlaying != null && lastPlaying.countPlaying() > 0){
                        lastPlaying.stop();
                        if(lastPlaying == audioSource){
                            return;
                        }
                    }

                    if(audioSource instanceof Music m){
                        lastPlaying = m;
                        m.play();
                    }else if(audioSource instanceof Sound s){
                        lastPlaying = s;
                        var bus = s.bus;
                        s.bus = control.sound.uiBus;
                        s.play();
                        s.bus = bus;
                    }
                }).update(i -> i.getStyle().imageUp = audioSource != null && audioSource.countPlaying() > 0 ? Icon.pause : Icon.play).size(h);
            }

            list.table(Styles.grayPanel, in -> {
                in.add("[accent]" + asset.name + "\n" + "[lightgray][[" + (audioSource == null ? "?" : UI.formatTime(audioSource.getLength() * 60f)) + "]").labelAlign(Align.left).grow();
                if(audioSource instanceof Music m){
                    var slider = new Slider(0f, m.getLength(), 0.1f, false);
                    slider.moved(value -> {
                        if(lastPlaying == m){
                            m.setPosition(value);
                        }
                    });
                    slider.visible(() -> {
                        boolean valid = lastPlaying == m && m.isPlaying();
                        if(valid){
                            slider.setValue(m.getPosition(), false);
                        }
                        return valid;
                    });

                    Label label = new Label(() -> slider.visible ? UI.formatTime(slider.getValue() * 60f) : "");
                    label.setAlignment(Align.center);
                    label.setStyle(Styles.outlineLabel);
                    label.touchable = Touchable.disabled;

                    in.stack(slider, label).growX().height(42f);
                }
            }).size(mobile ? 390f : 450f, h).margin(10f);

            list.button(Icon.export, Styles.graySquarei, Vars.iconMed, () -> {
                if(ios){
                    try{
                        Fi out = tmpDirectory.child(Strings.getFileName(asset.path));
                        file.copyTo(out);
                        platform.shareFile(out);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                }else{
                    platform.showFileChooser(false, Strings.getFileExtension(asset.path), result -> {
                        try{
                            file.copyTo(result);
                        }catch(Exception e){
                            ui.showException(e);
                        }
                    });
                }
            }).size(h).disabled(file == null);

            list.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                ui.showConfirm("@asset.delete.confirm",  () -> {
                    if(audioSource != null){
                        audioSource.dispose();
                    }
                    assets.remove(asset);
                    diag.rebuild();
                });
            }).size(h);

            list.row();
        }

        if(list.getChildren().isEmpty()){
            list.add("@patch.none");
        }

        list.add(new Element(){
            @Override
            public boolean remove(){
                if(lastPlaying != null){
                    lastPlaying.stop();
                }
                return super.remove();
            }
        });
    }

    @Override
    public void buildButtons(MapAssetsDialog diag, Table buttons){

        buttons.button("@add", Icon.add, () -> {
            platform.showMultiFileChooser(result -> {
                try{
                    //path and name are the same here; there's no path context.
                    String name = result.nameWithoutExtension();
                    String path = name;
                    var other = state.data.getAssets(type).find(p -> (p.path.equalsIgnoreCase(path) || p.name.equalsIgnoreCase(name)));
                    if(other != null){
                        ui.showErrorMessage(Core.bundle.format("asset.exists", other.name + " (" + other.path + ")"));
                        return;
                    }

                    var assets = state.data.getAssets(type);

                    DataAsset asset = (type == DataAssetType.music ? new MusicAsset() : new SoundAsset());
                    asset.setPath(path);
                    asset.updateData(result.readBytes());

                    assets.add(asset);
                    assets.sort();
                    state.data.reloadAudio();
                    diag.rebuild();
                    lastPlaying = null;
                }catch(Exception e){
                    ui.showException(e);
                }
            }, "ogg", "mp3");
        }).size(190f, 64f);

    }
}
