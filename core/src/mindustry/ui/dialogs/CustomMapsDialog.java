package mindustry.ui.dialogs;

import mindustry.maps.*;

public class CustomMapsDialog extends MapListDialog{
    private MapPlayDialog dialog = new MapPlayDialog();

    public CustomMapsDialog(){
        super("@customgame", false);
    }

    @Override
    void showMap(Map map){
        dialog.show(map);
    }
}
