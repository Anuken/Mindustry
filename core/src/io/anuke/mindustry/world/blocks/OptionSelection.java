package io.anuke.mindustry.world.blocks;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.scene.ui.ButtonGroup;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Cell;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.ui.Styles;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class OptionSelection {

    public static final int DEFAULT_CELL_SIZE = 44;
    private Array<Option> options = new Array<>(true, 5);
    private Consumer<Cell<ImageButton>> imageCellConfigurator = c -> {};
    private Consumer<Table> tableConfigurator = t -> {};

    public static OptionSelection create(){
        return new OptionSelection();
    }

    private class Option {
        final String drawableName;
        final Runnable onSelect;
        final Supplier<Boolean> checkedCondition;

        Option(String drawableName, Runnable onSelect, Supplier<Boolean> checkedCondition){
            this.drawableName = drawableName;
            this.onSelect = onSelect;
            this.checkedCondition = checkedCondition;
        }
    }

    private OptionSelection(){
    }

    public OptionSelection addOption(String drawableName, Runnable onSelect, Supplier<Boolean> checkedCondition){
        options.add(new Option(drawableName, onSelect, checkedCondition));
        return this;
    }

    public OptionSelection withImageCellConfig(Consumer<Cell<ImageButton>> cellConfigurator){
        this.imageCellConfigurator = cellConfigurator;
        return this;
    }

    public OptionSelection withTableConfig(Consumer<Table> tableConfigurator){
        this.tableConfigurator = tableConfigurator;
        return this;
    }

    public Cell<Table> buildOptionTable(Table table){

        ButtonGroup<ImageButton> group = new ButtonGroup<>();

        Table cont = new Table();
        cont.defaults().size(DEFAULT_CELL_SIZE);
        tableConfigurator.accept(cont);

        options.forEach(option -> imageCellConfigurator.accept(cont.addImageButton(Core.atlas.drawable(option.drawableName), Styles.clearToggleTransi, option.onSelect)
                .group(group)
                .checked(ignored -> option.checkedCondition.get())));

        return table.add(cont);
    }
}
