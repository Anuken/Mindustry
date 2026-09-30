package mindustry.logic.statements;

import arc.*;
import arc.audio.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.logic.statements.PlaySoundStatement.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

@RegisterStatement("playsound")
public class PlaySoundStatement extends LStatement{
    public boolean positional;
    public String id = "@sfx-shoot", volume = "1", pitch = "1", pan = "0", x = "@thisx", y = "@thisy", limit = "true";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(bundle(positional ? "positional" : "global"), Styles.logict, () -> {
            positional = !positional;
            build(table);
        }).size(160f, 40f).pad(4f).color(table.color);

        table.table(t -> {
            t.setColor(table.color);
            field(t, id, str -> id = str).padRight(0f).get();

            t.button(b -> {
                b.image(Icon.pencilSmall);
                b.clicked(() -> showSoundSelect(b, table));
            }, Styles.logict, () -> {
            }).size(40).color(table.color).left().padLeft(-1);
        });

        String desc2 = bundle("volume");
        fields(table, desc2, volume, str3 -> volume = str3);
        String desc1 = bundle("pitch");
        fields(table, desc1, pitch, str2 -> pitch = str2);

        if(positional){
            fields(table, "x", x, str1 -> x = str1);

            fields(table, "y", y, str -> y = str);
        }else{
            String desc = bundle("pan");
            fields(table, desc, pan, str -> pan = str);
        }

        String desc = bundle("limit");
        fields(table, desc, limit, str -> limit = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new PlaySoundI(positional, builder.var(id), builder.var(volume), builder.var(pitch), builder.var(pan), builder.var(x), builder.var(y), builder.var(limit));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }

    private static @Nullable Sound lastPreview;

    private static class SoundChoice{
        final String category;
        final String name;
        final Sound sound;

        SoundChoice(String category, String name, Sound sound){
            this.category = category;
            this.name = name;
            this.sound = sound;
        }
    }

    protected void showSoundSelect(Button button, Table table){
        Seq<SoundChoice> choices = new Seq<>();
        ObjectMap<String, Seq<SoundChoice>> categories = new ObjectMap<>();

        for(var entry : Core.assets.getAllEntries(Sound.class, new Seq<>())){
            Sound sound = entry.value;
            if(sound == Sounds.none || sound == null || sound.file == null) continue;

            String name = Strings.getFileNameWithoutExtension(entry.key);
            String category = soundCategory(entry.key);

            SoundChoice choice = new SoundChoice(category, name, sound);
            choices.add(choice);
            categories.get(category, Seq::new).add(choice);
        }

        if(choices.isEmpty()) return;

        choices.sort((a, b) -> {
            int cmp = a.category.compareTo(b.category);
            return cmp == 0 ? a.name.compareTo(b.name) : cmp;
        });

        Seq<String> categoryNames = categories.keys().toSeq().sort();
        categoryNames.insert(0, "all");

        for(var seq : categories.values()){
            seq.sortComparing(a -> a.name);
        }

        String current = id.startsWith("@sfx-") ? id.substring(5) : id;
        String currentCategory = "all";
        for(var choice : choices){
            if(choice.name.equals(current)){
                currentCategory = choice.category;
                break;
            }
        }

        final String selectedCurrentCategory = currentCategory;

        showSelectTable(button, (root, hide) -> {
            root.left().top();

            String[] selectedCategory = {categories.containsKey(selectedCurrentCategory) ? selectedCurrentCategory : "all"};
            Table soundList = new Table();
            ButtonGroup<Button> tabGroup = new ButtonGroup<>();

            Runnable rebuild = () -> {
                soundList.clearChildren();
                soundList.defaults().left().pad(2f);
                soundList.top();

                Seq<SoundChoice> visible = new Seq<>();
                if("all".equals(selectedCategory[0])){
                    visible.addAll(choices);
                }else{
                    visible.addAll(categories.get(selectedCategory[0], Seq::new));
                }

                if(visible.isEmpty()){
                    soundList.add("@none.found").pad(8f);
                    return;
                }

                for(var choice : visible){
                    soundList.table(row -> {
                        row.left().top();
                        row.defaults().left();

                        row.button(Icon.play, Styles.cleari, 28f, () -> previewSound(choice.sound))
                        .update(b -> b.getStyle().imageUp = choice.sound != null && choice.sound == lastPreview && choice.sound.countPlaying() > 0 ? Icon.pause : Icon.play)
                        .size(40f).padRight(6f);

                        String label = "all".equals(selectedCategory[0]) ? bundle(choice.category) + "/" + choice.name : choice.name;
                        row.button(label, Styles.logicTogglet, () -> {
                            id = "@sfx-" + choice.name;
                            build(table);
                            hide.run();
                        }).growX().height(40f).left().checked(choice.name.equals(current)).with(t -> {
                            t.getLabelCell().growX().left().labelAlign(Align.left).padLeft(10f);
                        }).padRight(4f);
                    }).growX().height(40f).padBottom(3f).row();
                }
            };

            root.table(tabs -> {
                tabs.left().top();
                tabs.defaults().size(140f, 34f).left();

                for(String category : categoryNames){
                    tabs.button(bundle(category), Styles.logicTogglet, () -> {
                        selectedCategory[0] = category;
                        rebuild.run();
                        //fixes flickering
                        var parent = (Table)root.parent.parent;
                        parent.pack();
                        parent.act(0f);
                    }).checked(selectedCategory[0].equals(category)).group(tabGroup).growX().row();
                }
            }).top().left().width(160f);

            root.add(soundList).top().width(Math.min(Core.graphics.getWidth() / Scl.scl(1f) * 0.9f, 450f));

            rebuild.run();
        }, () -> {
            if(lastPreview != null){
                lastPreview.stop();
            }
        });
    }

    private static String soundCategory(String entryName){
        String normalized = entryName.replace('\\', '/');
        int end = normalized.lastIndexOf('/');
        if(end < 0) return "data patch";

        int start = normalized.lastIndexOf('/', end - 1);
        return normalized.substring(start + 1, end);
    }

    private static void previewSound(Sound sound){
        if(sound == null || sound == Sounds.none) return;

        if(lastPreview != null && lastPreview.countPlaying() > 0 && lastPreview != Sounds.uiButton){
            lastPreview.stop();
            if(lastPreview == sound) return; //double tap = stop
        }

        lastPreview = sound;
        //don't play the button sound every time
        if(sound != Sounds.uiButton) Sounds.uiButton.stop();

        //play sound on the UI bus as the main one is paused
        sound.play(1, 1, 0, false, true, control.sound.uiBus);
    }
}
