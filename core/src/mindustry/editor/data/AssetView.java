package mindustry.editor.data;

import arc.scene.ui.layout.*;

public interface AssetView{
    void build(MapAssetsDialog diag, Table table);
    void buildButtons(MapAssetsDialog diag, Table buttons);
}
