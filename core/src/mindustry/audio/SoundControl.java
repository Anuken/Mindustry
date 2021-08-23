package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.audio.Filters.*;
import arc.files.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/** Controls playback of multiple audio tracks.*/
public class SoundControl{
    protected static final float finTime = 120f, foutTime = 120f, musicInterval = 3f * Time.toMinutes, musicChance = 0.6f, musicWaveChance = 0.46f;

    /** normal, ambient music, plays at any time */
    public Seq<Music> ambientMusic = Seq.with();
    /** darker music, used in times of conflict  */
    public Seq<Music> darkMusic = Seq.with();
    /** music used explicitly after boss spawns */
    public Seq<Music> bossMusic = Seq.with();

    protected Music lastRandomPlayed;
    protected Interval timer = new Interval(4);
    protected @Nullable Music current;
    protected float fade;
    protected boolean silenced;

    protected AudioBus uiBus = new AudioBus();
    protected boolean wasPlaying;
    protected AudioFilter filter = new BiquadFilter(){{
        set(0, 500, 1);
    }};

    protected ObjectMap<Sound, SoundData> sounds = new ObjectMap<>();

    public SoundControl(){
        Events.on(ClientLoadEvent.class, e -> reload());

        //only run music 10 seconds after a wave spawns
        Events.on(WaveEvent.class, e -> Time.run(Mathf.random(8f, 15f) * 60f, () -> {
            boolean boss = state.rules.spawns.contains(group -> group.getSpawned(state.wave - 2) > 0 && group.effect == StatusEffects.boss);

            if(boss){
                playOnce(bossMusic.random(lastRandomPlayed));
            }else if(Mathf.chance(musicWaveChance)){
                playRandom();
            }
        }));

        setupFilters();
    }

    protected void setupFilters(){
        Core.audio.soundBus.setFilter(0, filter);
        Core.audio.soundBus.setFilterParam(0, Filters.paramWet, 0f);
    }

    protected void reload(){
        current = null;
        fade = 0f;
        ambientMusic = Seq.with(Musics.game1, Musics.game3, Musics.game6, Musics.game8, Musics.game9);
        darkMusic = Seq.with(Musics.game2, Musics.game5, Musics.game7, Musics.game4);
        bossMusic = Seq.with(Musics.boss1, Musics.boss2, Musics.game2, Musics.game5);

        //setup UI bus for all sounds that are in the UI folder
        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            var file = Fi.get(Core.assets.getAssetFileName(sound));
            if(file.parent().name().equals("ui")){
                sound.setBus(uiBus);
            }
        }
    }

    public void loop(Sound sound, float volume){
        if(Vars.headless) return;

        loop(sound, Core.camera.position, volume);
    }

    public void loop(Sound sound, Position pos, float volume){
        if(Vars.headless) return;

        float baseVol = sound.calcFalloff(pos.getX(), pos.getY());
        float vol = baseVol * volume;

        SoundData data = sounds.get(sound, SoundData::new);
        data.volume += vol;
        data.volume = Mathf.clamp(data.volume, 0f, 1f);
        data.total += baseVol;
        data.sum.add(pos.getX() * baseVol, pos.getY() * baseVol);
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
        boolean paused = state.isGame() && Core.scene.hasDialog();
        boolean playing = state.isGame();

        //check if current track is finished
        if(current != null && !current.isPlaying()){
            current = null;
            fade = 0f;
        }

        //fade the lowpass filter in/out, poll every 30 ticks just in case performance is an issue
        if(timer.get(1, 30f)){
            Core.audio.soundBus.fadeFilterParam(0, Filters.paramWet, paused ? 1f : 0f, 0.4f);
        }

        //play/stop ordinary effects
        if(playing != wasPlaying){
            wasPlaying = playing;

            if(playing){
                Core.audio.soundBus.play();
                setupFilters();
            }else{
                //stopping a single audio bus stops everything else, yay!
                Core.audio.soundBus.stop();
                //play music bus again, as it was stopped above
                Core.audio.musicBus.play();

                Core.audio.soundBus.play();
            }
        }

        Core.audio.setPaused(Core.audio.soundBus.id, state.isPaused());

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

        updateLoops();
    }

    protected void updateLoops(){
        //clear loops when in menu
        if(!state.isGame()){
            sounds.clear();
            return;
        }

        float avol = Core.settings.getInt("ambientvol", 100) / 100f;

        sounds.each((sound, data) -> {
            data.curVolume = Mathf.lerpDelta(data.curVolume, data.volume * avol, 0.2f);

            boolean play = data.curVolume > 0.01f;
            float pan = Mathf.zero(data.total, 0.0001f) ? 0f : sound.calcPan(data.sum.x / data.total, data.sum.y / data.total);
            if(data.soundID <= 0 || !Core.audio.isPlaying(data.soundID)){
                if(play){
                    data.soundID = sound.loop(data.curVolume, 1f, pan);
                    Core.audio.protect(data.soundID, true);
                }
            }else{
                if(data.curVolume <= 0.001f){
                    sound.stop();
                    data.soundID = -1;
                    return;
                }
                Core.audio.set(data.soundID, pan, data.curVolume);
            }

            data.volume = 0f;
            data.total = 0f;
            data.sum.setZero();
        });
    }

    /** Plays a random track.*/
    public void playRandom(){
        if(isDark()){
            playOnce(darkMusic.random(lastRandomPlayed));
        }else{
            playOnce(ambientMusic.random(lastRandomPlayed));
        }
    }

    /** Whether to play dark music.*/
    protected boolean isDark(){
        if(player.team().data().hasCore() && player.team().data().core().healthf() < 0.85f){
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
        current.play();
    }

    protected boolean shouldPlay(){
        return Core.settings.getInt("musicvol") > 0;
    }

    /** Fades out the current track, unless it has already been silenced. */
    protected void silence(){
        play(null);
    }

    protected static class SoundData{
        float volume;
        float total;
        Vec2 sum = new Vec2();

        int soundID;
        float curVolume;
    }
}
