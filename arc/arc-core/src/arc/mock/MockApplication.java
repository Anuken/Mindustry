package arc.mock;

import arc.*;
import arc.struct.*;

public class MockApplication implements Application{

    @Override
    public Seq<ApplicationListener> getListeners(){
        return new Seq<>();
    }

    @Override
    public ApplicationType getType(){
        return ApplicationType.headless;
    }

    @Override
    public String getClipboardText(){
        return null;
    }

    @Override
    public void setClipboardText(String text){

    }

    @Override
    public void post(Runnable runnable){
        runnable.run();
    }

    @Override
    public void exit(){

    }
}
