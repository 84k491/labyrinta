package bakar.labyrinta;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

enum Sounds{
    coinPickedUp,
    bonusPickedUp,
    levelFinished,
    correct,
    incorrect
}

final class SoundCore {
    static private final SoundCore instance = new SoundCore();
    private final SoundPool soundPool;
    private MediaPlayer mediaPlayer_menu;
    private MediaPlayer mediaPlayer_game;
    boolean currenPlayerIsGame = false;

    boolean doPlayMusic;
    boolean doPlaySounds;

    private int started_activities_amount = 0;

    private final Map<Sounds, Integer> resource_id_map = new HashMap<>();
    private final Map<Sounds, Integer> sound_id_map = new HashMap<>();

    static SoundCore inst(){
        return instance;
    }

    private SoundCore(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(5).build();
        }
        else{
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        resource_id_map.put(Sounds.coinPickedUp, R.raw.coin_pu);
        resource_id_map.put(Sounds.bonusPickedUp, R.raw.bonus_pu);
        resource_id_map.put(Sounds.levelFinished, R.raw.level_finished);
        resource_id_map.put(Sounds.correct, R.raw.correct);
        resource_id_map.put(Sounds.incorrect, R.raw.incorrect);

        doPlayMusic = StoredProgress.getInstance().getValueBoolean("isMusicOn");
        doPlaySounds = StoredProgress.getInstance().getValueBoolean("isSoundsOn");
    }

    void loadSounds(Context context){
        mediaPlayer_menu = MediaPlayer.create(context, R.raw.background);
        mediaPlayer_menu.setLooping(true);
        mediaPlayer_menu.setVolume(.8f, .8f);

        mediaPlayer_game = MediaPlayer.create(context, R.raw.in_game_theme);
        mediaPlayer_game.setLooping(true);
        mediaPlayer_game.setVolume(.8f, .8f);

        for (Sounds sound : Sounds.values()){
            sound_id_map.put(sound,
                soundPool.load(context, Objects.requireNonNull(resource_id_map.get(sound)), 0)
            );
        }
    }

    void playSound(Sounds sound){

        if (!doPlaySounds){
            return;
        }

        soundPool.play(Objects.requireNonNull(sound_id_map.get(sound)),
                1,
                1,
                0,
                0,
                1.f);
    }

    void playMenuBackgroundMusic(){
        started_activities_amount++;
        if (doPlayMusic){
            mediaPlayer_menu.start();
        }
    }

    void playGameBackgroundMusic(){
        if (doPlayMusic){
            mediaPlayer_game.start();
        }
        currenPlayerIsGame = true;
    }

    void pauseMenuBackgroundMusic(){
        started_activities_amount--;
        if (0 >= started_activities_amount && mediaPlayer_menu.isPlaying()){
            started_activities_amount = 0;
            mediaPlayer_menu.pause();
        }
    }

    void pauseGameBackgroundMusic(){
        if (mediaPlayer_game.isPlaying()){
            mediaPlayer_game.pause();
        }
        currenPlayerIsGame = false;
    }

    void playMenuBackgroundMusicForced(){
        mediaPlayer_menu.start();
    }

    void pauseMenuBackgroundMusicForced(){
        if (mediaPlayer_menu.isPlaying()){
            mediaPlayer_menu.pause();
        }
    }

    void releaseMP(){
        mediaPlayer_menu.release();
    }
}
