package bakar.labirynth;

import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

class StoredProgress {
    private static final StoredProgress ourInstance = new StoredProgress();

    SharedPreferences sharedPreferences = null;

    static final String teleportAmountKey = "teleportAmount";
    static final String pathfinderAmountKey = "pathfinderAmount";
    static final String pointerAmountKey = "pointerAmount";

    static final String teleportUpgKey = "tp_upg";
    static final String pathfinderUpgKey = "pf_upg";
    static final String pointerUpgKey = "pt_upg";
    static final String levelUpgKey = "level_upg";

    static final String goldKey = "gold";

    static StoredProgress getInstance() {
        return ourInstance;
    }

    private StoredProgress() {}

    void setSharedPreferences(SharedPreferences _sharedPreferences){
        if (sharedPreferences == null) {
            sharedPreferences = _sharedPreferences;
        }
        if (0 == getValue(levelUpgKey)){
            setValue(levelUpgKey, 1);
        }
    }

    void resetAll(){
        reset(teleportAmountKey);
        reset(pathfinderAmountKey);
        reset(pointerAmountKey);

        reset(teleportUpgKey);
        reset(pathfinderUpgKey);
        reset(pointerUpgKey);
        setValue(levelUpgKey, 1);

        reset(goldKey);
    }
    void reset(String key){
        setValue(key, 0);
    }
    void setValue(String key, int value){
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putInt(key, value);
        ed.apply();
    }
    void setValue(String key, boolean value) {
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putBoolean(key, value);
        ed.apply();
    }
    void setValue(String key, float value){
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putFloat(key, value);
        ed.apply();
    }
    void setValue(String key, String value){
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(key, value);
        ed.apply();
    }
    int getValue(String key){
        int result = sharedPreferences.getInt(key, 0);
        return result;
    }

    int getGoldAmount(){
        return sharedPreferences.getInt(goldKey, 0);
    }
    void setGold(int ga){
        setValue("gold", ga);
    }
}
