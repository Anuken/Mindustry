package mindustry.mod;

import arc.audio.*;
import arc.mock.*;
import arc.util.ArcAnnotate.*;

public class ModLoadingMusic implements Music{
    public @NonNull Music music = new MockMusic();

    @Override
    public void play(){
        music.play();
    }

    @Override
    public void pause(){
        music.pause();
    }

    @Override
    public void stop(){
        music.stop();
    }

    @Override
    public boolean isPlaying(){
        return music.isPlaying();
    }

    @Override
    public boolean isLooping(){
        return music.isLooping();
    }

    @Override
    public void setLooping(boolean isLooping){
        music.setLooping(isLooping);
    }

    @Override
    public float getVolume(){
        return music.getVolume();
    }

    @Override
    public void setVolume(float volume){
        music.setVolume(volume);
    }

    @Override
    public void setPan(float pan, float volume){
        music.setPan(pan, volume);
    }

    @Override
    public float getPosition(){
        return music.getPosition();
    }

    @Override
    public void setPosition(float position){
        music.setPosition(position);
    }

    @Override
    public void dispose(){
        music.dispose();
    }

    @Override
    public void setCompletionListener(OnCompletionListener listener){
        music.setCompletionListener(listener);
    }

    @Override
    public boolean isDisposed(){
        return music.isDisposed();
    }
}
