package io.anuke.mindustry.ui.dialogs;

import io.anuke.ucore.scene.ui.Dialog;

//TODO
public class HostDialog extends Dialog{

    public HostDialog(){
        super("$text.hostserver", "dialog");
    }

    /*
    showTextInput("$text.hostserver", "$text.server.port", Vars.port + "", new DigitsOnlyFilter(), text -> {
			int result = Strings.parseInt(text);
			if(result == Integer.MIN_VALUE || result >= 65535){
				Vars.ui.showError("$text.server.invalidport");
			}else{
				try{
					Net.host(result);
				}catch (IOException e){
					Vars.ui.showError(Bundles.format("text.server.error", Strings.parseException(e, false)));
				}
			}
		});
     */
}
