package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BannedContentDialog<T extends UnlockableContent> extends BaseDialog{
    private final ContentType type;
    private Table selectedTable;
    private Table deselectedTable;
    private ObjectSet<T> contentSet;
    private final Boolf<T> pred;
    private String contentSearch;
    private Category selectedCategory;
    private Seq<T> filteredContent;

    public BannedContentDialog(String title, ContentType type, Boolf<T> pred){
        super(title);
        this.type = type;
        this.pred = pred;
        contentSearch = "";

        selectedTable = new Table();
        deselectedTable = new Table();

        addCloseButton();

        shown(this::build);
        resized(this::build);
    }

    public void show(ObjectSet<T> contentSet){
        this.contentSet = contentSet;
        show();
    }

    public void build(){
        cont.clear();

        var cell = cont.table(t -> {
            t.table(s -> {
                s.label(() -> "@search").padRight(10);
                var field = s.field(contentSearch, value -> {
                    contentSearch = value.trim().replaceAll(" +", " ").toLowerCase();
                    rebuildTables();
                }).get();
                s.button(Icon.cancel, Styles.emptyi, () -> {
                    contentSearch = "";
                    field.setText("");
                    rebuildTables();
                }).padLeft(10f).size(35f);
            });
            if(type == ContentType.block){
                t.row();
                t.table(c -> {
                    c.marginTop(8f);
                    c.defaults().marginRight(4f);
                    for(Category category : Category.values()){
                        c.button(ui.getIcon(category.name()), Styles.squareTogglei, () -> {
                            if(selectedCategory == category){
                                selectedCategory = null;
                            }else{
                                selectedCategory = category;
                            }
                            rebuildTables();
                        }).size(45f).update(i -> i.setChecked(selectedCategory == category)).padLeft(4f);
                    }
                    c.add("").padRight(4f);
                }).center();
            }
        });
        cont.row();
        if(!Core.graphics.isPortrait()) cell.colspan(2);

        filteredContent = content.<T>getBy(type).select(pred);
        if(!contentSearch.isEmpty()) filteredContent.removeAll(content -> !content.localizedName.toLowerCase().contains(contentSearch.toLowerCase()));

        cont.table(table -> {
            if(type == ContentType.block){
                table.add("@bannedblocks").color(Color.valueOf("f25555")).padBottom(-1).top().row();
            }else{
                table.add("@bannedunits").color(Color.valueOf("f25555")).padBottom(-1).top().row();
            }

            table.image().color(Color.valueOf("f25555")).height(3f).padBottom(5f).fillX().expandX().top().row();
            table.pane(table2 -> selectedTable = table2).fill().expand().row();
            table.button("@addall", Icon.add, () -> {
                contentSet.addAll(filteredContent);
                rebuildTables();
            }).disabled(button -> contentSet.toSeq().containsAll(filteredContent)).padTop(10f).bottom().fillX();
        }).fill().expandY().uniform();

        if(Core.graphics.isPortrait()) cont.row();

        var cell2 = cont.table(table -> {
            if(type == ContentType.block){
                table.add("@unbannedblocks").color(Pal.accent).padBottom(-1).top().row();
            }else{
                table.add("@unbannedunits").color(Pal.accent).padBottom(-1).top().row();
            }

            table.image().color(Pal.accent).height(3f).padBottom(5f).fillX().top().row();
            table.pane(table2 -> deselectedTable = table2).fill().expand().row();
            table.button("@addall", Icon.add, () -> {
                contentSet.removeAll(filteredContent);
                rebuildTables();
            }).disabled(button -> {
                Seq<T> array = content.getBy(type);
                array = array.copy();
                array.removeAll(contentSet.toSeq());
                return array.containsAll(filteredContent);
            }).padTop(10f).bottom().fillX();
        }).fill().expandY().uniform();
        if(Core.graphics.isPortrait()){
            cell2.padTop(10f);
        }else{
            cell2.padLeft(10f);
        }

        rebuildTables();
    }

    private void rebuildTables(){
        filteredContent.clear();
        filteredContent = content.getBy(type);
        filteredContent = filteredContent.select(pred);

        if(!contentSearch.isEmpty()) filteredContent.removeAll(content -> !content.localizedName.toLowerCase().contains(contentSearch.toLowerCase()));
        if(type == ContentType.block){
            filteredContent.removeAll(content -> selectedCategory != null && ((Block)content).category != selectedCategory);
        }

        rebuildTable(selectedTable, true);
        rebuildTable(deselectedTable, false);
    }

    private void rebuildTable(Table table, boolean isSelected){
        table.clear();

        int cols;
        if(Core.graphics.isPortrait()){
            cols = Math.max(4, (int)((Core.graphics.getWidth() / Scl.scl() - 100f) / 50f));
        }else{
            cols = Math.max(4, (int)((Core.graphics.getWidth() / Scl.scl() - 300f) / 50f / 2));
        }

        if((isSelected && contentSet.isEmpty()) || (!isSelected && contentSet.size == content.<T>getBy(type).count(pred))){
            table.add("@empty").width(50f * cols).padBottom(5f).get().setAlignment(Align.center);
        }else{
            Seq<T> array;
            if(!isSelected){
                array = content.getBy(type);
                array = array.copy();
                array.removeAll(contentSet.toSeq());
            }else{
                array = contentSet.toSeq();
            }
            array.sort();
            array.removeAll(content -> !filteredContent.contains(content));

            if(array.isEmpty()){
                table.add("@empty").width(50f * cols).padBottom(5f).get().setAlignment(Align.center);
                return;
            }
            int i = 0;
            boolean requiresPad = true;

            for(T content : array){
                TextureRegion region = content.uiIcon;

                ImageButton button = new ImageButton(Tex.whiteui, Styles.clearNonei);
                button.getStyle().imageUp = new TextureRegionDrawable(region);
                button.resizeImage(8 * 4f);
                if(isSelected) button.clicked(() -> {
                    contentSet.remove(content);
                    rebuildTables();
                });
                else button.clicked(() -> {
                    contentSet.add(content);
                    rebuildTables();
                });
                table.add(button).size(50f).tooltip(content.localizedName);

                if(++i % cols == 0){
                    table.row();
                    requiresPad = false;
                }
            }

            if(requiresPad){
                table.add("").padRight(50f * (cols - i));
            }
        }
    }
}
