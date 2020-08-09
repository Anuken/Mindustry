package mindustry.ui.fragments;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mindustry.*;

/** Fragment for displaying overlays such as block inventories. */
public class OverlayFragment{
    public final BlockInventoryFragment inv;
    public final BlockConfigFragment config;

    private WidgetGroup group = new WidgetGroup();

    public OverlayFragment(){
        group.touchable = Touchable.childrenOnly;
        inv = new BlockInventoryFragment();
        config = new BlockConfigFragment();
    }

    public void add(){
        group.setFillParent(true);
        Vars.ui.hudGroup.addChildBefore(Core.scene.find("overlaymarker"), group);

        inv.build(group);
        config.build(group);
    }

    public void remove(){
        group.remove();
    }
}
