package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/** Controls playback of multiple music tracks.*/
public class MusicControl{
    protected static final float finTime = 120f, foutTime = 120f, musicInterval = 60 * 60 * 3f, musicChance = 0.6f, musicWaveChance = 0.5f;

    /** normal, ambient music, plays at any time */
    public Seq<Music> ambientMusic = Seq.with();
    /** darker music, used in times of conflict  */
    public Seq<Music> darkMusic = Seq.with();

    protected Music lastRandomPlayed;
    protected Interval timer = new Interval();
    protected @Nullable Music current;
    protected float fade;
    protected boolean silenced;

    public MusicControl(){
        Events.on(ClientLoadEvent.class, e -> reload());

        //only run music 10 seconds after a wave spawns
        Events.on(WaveEvent.class, e -> Time.run(60f * 10f, () -> {
            if(Mathf.chance(musicWaveChance)){
                playRandom();
            }
        }));
    }

    protected void reload(){
        current = null;
        fade = 0f;
        ambientMusic = Seq.with(Musics.game1, Musics.game3, Musics.game4, Musics.game6);
        darkMusic = Seq.with(Musics.game2, Musics.game5, Musics.game7);
    }

    public void stop(){
        silenced = true;
        if(current != null){
            current.stop();
            current = null;
            fade = 0f;
        }
    }

    /** Update and play the right music track.*/
    public void update(){
        if(state.isMenu()){
            silenced = false;
            if(ui.planet.isShown()){
                play(Musics.launch);
            }else if(ui.editor.isShown()){
                play(Musics.editor);
            }else{
                play(Musics.menu);
            }
        }else if(state.rules.editor){
            silenced = false;
            play(Musics.editor);
        }else{
            //this just fades out the last track to make way for ingame music
            silence();

            //play music at intervals
            if(timer.get(musicInterval)){
                //chance to play it per interval
                if(Mathf.chance(musicChance)){
                    playRandom();
                }
            }
        }
    }

    /** Plays a random track.*/
    protected void playRandom(){
        if(isDark()){
            playOnce(darkMusic.random(lastRandomPlayed));
        }else{
            playOnce(ambientMusic.random(lastRandomPlayed));
        }
    }

    /** Whether to play dark music.*/
    protected boolean isDark(){
        if(state.teams.get(player.team()).hasCore() && state.teams.get(player.team()).core().healthf() < 0.85f){
            //core damaged -> dark
            return true;
        }

        //it may be dark based on wave
        if(Mathf.chance((float)(Math.log10((state.wave - 17f)/19f) + 1) / 4f)){
            return true;
        }

        //dark based on enemies
        return Mathf.chance(state.enemies / 70f + 0.1f);
    }

    /** Plays and fades in a music track. This must be called every frame.
     * If something is already playing, fades out that track and fades in this new music.*/
    protected void play(@Nullable Music music){
        if(!shouldPlay()){
            if(current != null){
                current.setVolume(0);
            }

            fade = 0f;
            return;
        }

        //update volume of current track
        if(current != null){
            current.setVolume(fade * Core.settings.getInt("musicvol") / 100f);
        }

        //do not update once the track has faded out completely, just stop
        if(silenced){
            return;
        }

        if(current == null && music != null){
            //begin playing in a new track
            current = music;
            current.setLooping(true);
            current.setVolume(fade = 0f);
            current.play();
            silenced = false;
        }else if(current == music && music != null){
            //fade in the playing track
            fade = Mathf.clamp(fade + Time.delta /finTime);
        }else if(current != null){
            //fade out the current track
            fade = Mathf.clamp(fade - Time.delta /foutTime);

            if(fade <= 0.01f){
                //stop current track when it hits 0 volume
                current.stop();
                current = null;
                silenced = true;
                if(music != null){
                    //play newly scheduled track
                    current = music;
                    current.setVolume(fade = 0f);
                    current.setLooping(true);
                    current.play();
                    silenced = false;
                }
            }
        }
    }

    /** Plays a music track once and only once. If something is already playing, does nothing.*/
    protected void playOnce(Music music){
        if(current != null || music == null || !shouldPlay()) return; //do not interrupt already-playing tracks

        //save last random track played to prevent duplicates
        lastRandomPlayed = music;

        //set fade to 1 and play it, stopping the current when it's done
        fade = 1f;
        current = music;
        current.setVolume(1f);
        current.setLooping(false);
        current.setCompletionListener(m -> {
            if(current == m){
                current = null;
                fade = 0f;
            }
        });
        current.play();
    }

    protected boolean shouldPlay(){
        return Core.settings.getInt("musicvol") > 0;
    }

    /** Fades out the current track, unless it has already been silenced. */
    protected void silence(){
        play(null);
    }
}
