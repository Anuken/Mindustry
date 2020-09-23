package mindustry.ui;

import arc.scene.ui.layout.Table;
import mindustry.type.Item;
import mindustry.type.ItemStack;

/** An item image with text. */
public class ItemDisplay extends Table{
    public final Item item;
    public final int amount;

    public ItemDisplay(Item item){
        this(item, 0);
    }

    public ItemDisplay(Item item, int amount, boolean showName){
        int digits = (int) Math.log10(amount) - 1 >= 0 ? (int) Math.log10(amount) - 1 : 0;

        add(new ItemImage(new ItemStack(item, amount))).size(8 * 4).padRight(2 + (12 * digits));
        if(showName) add(item.localizedName).padLeft(4 + 4 * digits);

        this.item = item;
        this.amount = amount;
    }

    public ItemDisplay(Item item, int amount){
        this(item, amount, true);
    }
}
