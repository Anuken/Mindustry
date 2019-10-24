package io.anuke.mindustry.mod;

import io.anuke.arc.audio.*;
import io.anuke.arc.audio.mock.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.ArcAnnotate.*;

public class ModLoadingSound implements Sound{
    public @NonNull Sound sound = new MockSound();

    @Override
    public float calcPan(float x, float y){
        return sound.calcPan(x, y);
    }

    @Override
    public float calcVolume(float x, float y){
        return sound.calcVolume(x, y);
    }

    @Override
    public float calcFalloff(float x, float y){
        return sound.calcFalloff(x, y);
    }

    @Override
    public int at(float x, float y, float pitch){
        return sound.at(x, y, pitch);
    }

    @Override
    public int at(float x, float y){
        return sound.at(x, y);
    }

    @Override
    public int at(Position pos){
        return sound.at(pos);
    }

    @Override
    public int at(Position pos, float pitch){
        return sound.at(pos, pitch);
    }

    @Override
    public int play(){
        return sound.play();
    }

    @Override
    public int play(float volume){
        return sound.play(volume);
    }

    @Override
    public int play(float volume, float pitch, float pan){
        return sound.play(volume, pitch, pan);
    }

    @Override
    public int loop(){
        return sound.loop();
    }

    @Override
    public int loop(float volume){
        return sound.loop(volume);
    }

    @Override
    public int loop(float volume, float pitch, float pan){
        return sound.loop(volume, pitch, pan);
    }

    @Override
    public void stop(){
        sound.stop();
    }

    @Override
    public void pause(){
        sound.pause();
    }

    @Override
    public void resume(){
        sound.resume();
    }

    @Override
    public void dispose(){
        sound.dispose();
    }

    @Override
    public void stop(int soundId){
        sound.stop(soundId);
    }

    @Override
    public void pause(int soundId){
        sound.pause(soundId);
    }

    @Override
    public void resume(int soundId){
        sound.resume(soundId);
    }

    @Override
    public void setLooping(int soundId, boolean looping){
        sound.setLooping(soundId, looping);
    }

    @Override
    public void setPitch(int soundId, float pitch){
        sound.setPitch(soundId, pitch);
    }

    @Override
    public void setVolume(int soundId, float volume){
        sound.setVolume(soundId, volume);
    }

    @Override
    public void setPan(int soundId, float pan, float volume){
        sound.setPan(soundId, pan, volume);
    }

    @Override
    public boolean isDisposed(){
        return sound.isDisposed();
    }
}
