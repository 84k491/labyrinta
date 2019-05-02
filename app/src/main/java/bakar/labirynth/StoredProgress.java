package bakar.labirynth;

import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

class StoredProgress {
    private static final StoredProgress ourInstance = new StoredProgress();

    private SharedPreferences sharedPreferences = null;

    static final String teleportAmountKey = "teleportAmount";
    static final String pathfinderAmountKey = "pathfinderAmount";
    static final String pointerAmountKey = "pointerAmount";

    static final String teleportUpgKey = "tp_upg";
    static final String pathfinderUpgKey = "pf_upg";
    static final String pointerUpgKey = "pt_upg";
    static final String levelUpgKey = "level_upg";

    static final String goldKey = "gold";

    static final String usesJoystickKey = "uses_joystick";
    static final String isDebugKey = "is_debug";
    static final String isMusicOnKey = "isMusicOn";

    static final String isNeedToShowTutorialFirst = "isNeedToShowTutorialFirst";
    static final String isNeedToShowTutorialPathfinder = "isNeedToShowTutorialPathfinder";
    static final String isNeedToShowTutorialPointer = "isNeedToShowTutorialPointer";
    static final String isNeedToShowTutorialTeleport = "isNeedToShowTutorialTeleport";

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

        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putBoolean(isNeedToShowTutorialFirst,
                sharedPreferences.getBoolean(isNeedToShowTutorialFirst, true));
        ed.putBoolean(isNeedToShowTutorialPointer,
                sharedPreferences.getBoolean(isNeedToShowTutorialPointer, true));
        ed.putBoolean(isNeedToShowTutorialPathfinder,
                sharedPreferences.getBoolean(isNeedToShowTutorialPathfinder, true));
        ed.putBoolean(isNeedToShowTutorialTeleport,
                sharedPreferences.getBoolean(isNeedToShowTutorialTeleport, true));
        ed.apply();
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
    boolean getValueBoolean(String key){
        boolean result = sharedPreferences.getBoolean(key, false);
        return result;
    }
    boolean switchValueBoolean(String key){
        setValue(key, !getValueBoolean(key));
        return getValueBoolean(key);
    }

    int getTeleportUpg(){
        return sharedPreferences.getInt(teleportUpgKey, 0);
    }
    int getPathfinderUpg(){
        return sharedPreferences.getInt(pathfinderUpgKey, 0);
    }
    int getPointerUpg(){
        return sharedPreferences.getInt(pointerUpgKey, 0);
    }

    int getTeleportAmount(){
        return sharedPreferences.getInt(teleportAmountKey, 0);
    }
    int getPathfinderAmount(){
        return sharedPreferences.getInt(pathfinderAmountKey, 0);
    }
    int getPointerAmount(){
        return sharedPreferences.getInt(pointerAmountKey, 0);
    }

    boolean getUsesJoystick(){
        boolean result = sharedPreferences.getBoolean(usesJoystickKey, false);
        return result;
    }
    boolean getIsBebug(){
        boolean result = sharedPreferences.getBoolean(isDebugKey, false);
        return result;
    }

    boolean switchUsesJoystick(){
        boolean result = sharedPreferences.getBoolean(usesJoystickKey, false);
        setValue(usesJoystickKey, !result);
        return !result;
    }
    boolean switchIsBebug(){
        boolean result = sharedPreferences.getBoolean(isDebugKey, false);
        setValue(isDebugKey, !result);
        return !result;
    }

    int getGoldAmount(){
        return sharedPreferences.getInt(goldKey, 0);
    }
    void setGold(int ga){
        setValue("gold", ga);
    }
}
