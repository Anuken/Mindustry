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

            if(file == null || audioSource == null || (!audioSource.valid() && !audioSource.isLazy())){
                list.button(Icon.warning, Styles.graySquarei, iconMed, () -> ui.showInfo(file == null ? "@asset.broken" : "@asset.audio.invalid")).size(h);
            }else{
                list.button(Icon.play, Styles.graySquarei, iconMed, () -> {
                    if(!audioSource.valid() && !audioSource.isLazy()){
                        ui.showInfo("@asset.audio.invalid");
                        return;
                    }

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
                        s.play(control.sound.uiBus);
                    }
                }).update(i -> i.getStyle().imageUp = (!audioSource.isLazy() && !audioSource.valid()) ? Icon.warning : audioSource.countPlaying() > 0 ? Icon.pause : Icon.play).size(h);
            }

            float w = (mobile ? 390f : 450f);
            list.table(Styles.grayPanel, in -> {
                in.left();
                in.table(v -> {
                    v.left();
                    v.add("[accent]" + asset.name).labelAlign(Align.left).left().ellipsis(true).width(audioSource instanceof Music ? w / 2f : w * 0.8f);
                    v.row();
                    if(audioSource != null){
                        Runnable addTime = () -> v.add("[lightgray][[" + UI.formatTime(audioSource.getLength() * 60f) + "]").left();

                        if(audioSource.valid()){
                            addTime.run();
                        }else if(audioSource instanceof Sound s){
                            //force-load sounds when viewing so that length/invalid status is shown
                            s.checkLazyLoad(addTime);
                        }
                    }
                }).width(w / 2f).tooltip("dp-" + asset.name.replace(' ', '_'));

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
            }).size(w, h).margin(10f);

            list.button(Icon.export, Styles.graySquarei, Vars.iconMed, () -> FileChooser.export(asset.name, Strings.getFileExtension(asset.path), file::copyTo)).size(h).disabled(file == null);

            list.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                ui.showConfirm("@asset.delete.confirm",  () -> {
                    assets.remove(asset);
                    state.data.reloadAudio();
                    diag.rebuild();
                });
            }).size(h);

            list.row();
        }

        if(list.getChildren().isEmpty()){
            list.add("@none.found");
        }

        list.add(new Element(){
            @Override
            public boolean remove(){
                if(lastPlaying != null){
                    lastPlaying.stop();
                }
                return super.remove();
            }

            @Override
            public void act(float delta){
                if(lastPlaying != null && lastPlaying.countPlaying() > 0){
                    control.sound.keepSilent();
                }
            }
        });
    }

    @Override
    public void buildButtons(MapAssetsDialog diag, Table buttons){

        buttons.button("@add", Icon.add, () -> {
            FileChooser.open("ogg", "mp3").submitMulti(result -> {
                var assets = state.data.getAssets(type);

                for(Fi file : result){
                    try{
                        String name = file.nameWithoutExtension();
                        String path = file.name();
                        var other = state.data.getAssets(type).find(p -> (p.path.equalsIgnoreCase(path) || p.name.equalsIgnoreCase(name)));
                        if(other != null){
                            ui.showErrorMessage(Core.bundle.format("asset.exists", other.name + " (" + other.path + ")"));
                            return;
                        }else if(Core.assets.getOrNull("dp-" + name, Music.class) != null || Core.assets.getOrNull("dp-" + name, Sound.class) != null){
                            ui.showErrorMessage(Core.bundle.format("asset.exists.audio", name));
                            return;
                        }

                        DataAsset asset = (type == DataAssetType.music ? new MusicAsset() : new SoundAsset());
                        asset.setPath(path);
                        asset.updateData(file.readBytes());

                        assets.add(asset);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                }

                assets.sort();
                state.data.reloadAudio();
                diag.rebuild();
                lastPlaying = null;
            });
        }).size(190f, 64f);

    }
}
