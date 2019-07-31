package bakar.labyrinta;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

enum Sounds{
    coinPickedUp,
    bonusPickedUp,
    levelFinished,
    correct,
    incorrect
}

public final class SoundCore {
    static private final SoundCore instance = new SoundCore();
    private SoundPool soundPool;
    private MediaPlayer mediaPlayer;

    boolean doPlayMusic = true;
    boolean doPlaySounds = true;

    private int started_activities_amount = 0;

    private Map<Sounds, Integer> resource_id_map = new HashMap<>();
    private Map<Sounds, Integer> sound_id_map = new HashMap<>();

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
        mediaPlayer = MediaPlayer.create(context, R.raw.background);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(.8f, .8f);

        for (Sounds sound : Sounds.values()){
            sound_id_map.put(sound,
                soundPool.load(context, resource_id_map.get(sound), 0)
            );
        }
    }

    void playSound(Sounds sound){

        if (!doPlaySounds){
            return;
        }

        soundPool.play(sound_id_map.get(sound),
                1,
                1,
                0,
                0,
                1.f);
    }

    void playBackgroungMusic(){
        started_activities_amount++;
        if (doPlayMusic){
            mediaPlayer.start();
        }
    }

    void pauseBackgroundMusic(){
        started_activities_amount--;
        if (0 >= started_activities_amount && mediaPlayer.isPlaying()){
            started_activities_amount = 0;
            mediaPlayer.pause();
        }
    }

    void playBackgroungMusicForced(){
        mediaPlayer.start();
    }

    void pauseBackgroundMusicForced(){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    void releaseMP(){
        mediaPlayer.release();
    }
}
