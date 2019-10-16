package bakar.labyrinta;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.constraint.ConstraintLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class StoredProgress {
    private static final StoredProgress ourInstance = new StoredProgress();

    private SharedPreferences sharedPreferences = null;

    private static final String titleFont = "fonts/CLiCHE 21.ttf";
    //private static final String trenchFont = "fonts/trench100free.ttf";
    private static final String trenchFont = "fonts/Gravity-UltraLight.otf";
    private static final String textFont = "fonts/Gravity-Regular.otf";

    Map<Skin, Integer> skinResMap = new HashMap<>();
    Map<Skin, Integer> skinShopItemBgMap = new HashMap<>();

    Typeface getTitleFont(AssetManager manager){
        return Typeface.createFromAsset(manager, titleFont);
    }

    Typeface getTrenchFont(AssetManager manager){
        return Typeface.createFromAsset(manager, trenchFont);
    }

    Typeface getTextFont(AssetManager manager){
        return Typeface.createFromAsset(manager, textFont);
    }

    static final String teleportAmountKey = "teleportAmount";
    static final String pathfinderAmountKey = "pathfinderAmount";
    static final String pointerAmountKey = "pointerAmount";

    static final String teleportUpgKey = "tp_upg";
    static final String pathfinderUpgKey = "pf_upg";
    static final String pointerUpgKey = "pt_upg";
    static final String levelUpgKey = "level_upg";

    static final String purchasedSkinPrefix = "is_skin_purchased: ";
    static final String activeSkinKey = "active_skin";

    private static final String goldKey = "gold";
    private static final String cameraZKey = "cameraZ";

    static final String usesJoystickKey = "uses_joystick";
    private static final String isDebugKey = "is_debug";
    private static final String isMusicOnKey = "isMusicOn";
    private static final String isSoundsOnKey = "isSoundsOn";

    static final String isNeedToShowTutorialFirst = "isNeedToShowTutorialFirst";
    static final String isNeedToShowTutorialPathfinder = "isNeedToShowTutorialPathfinder";
    static final String isNeedToShowTutorialPointer = "isNeedToShowTutorialPointer";
    static final String isNeedToShowTutorialTeleport = "isNeedToShowTutorialTeleport";
    static final String isNeedToLightBonusButton = "isNeedToLightBonusButton";
    static final String isNeedToShowTutorialBonusRange = "isNeedToShowTutorialBonusRange";
    static final String isNeedToShowTutorialNextLevel = "isNeedToShowTutorialNextLevel";

    static StoredProgress getInstance() {
        return ourInstance;
    }

    private StoredProgress() {}

    void setSkinResMap(){
        skinResMap.put(Skin.Default, R.drawable.bg_default);
        skinResMap.put(Skin.AnalogBlue, R.drawable.bg_analog_blue);
        skinResMap.put(Skin.Flare, R.drawable.bg_flare); // flare
        skinResMap.put(Skin.CheerUp, R.drawable.bg_cheer_up);
        skinResMap.put(Skin.CalmDaria, R.drawable.bg_calm_daria);
    }

    void setSkinShopItemBgMapMap(){
        skinShopItemBgMap.put(Skin.Default, R.xml.shop_item_bg_skin_default);
        skinShopItemBgMap.put(Skin.AnalogBlue, R.xml.shop_item_bg_analog_blue);
        skinShopItemBgMap.put(Skin.Flare, R.xml.shop_item_bg_flare);
        skinShopItemBgMap.put(Skin.CheerUp, R.xml.shop_item_bg_cheer_up);
        skinShopItemBgMap.put(Skin.CalmDaria, R.xml.shop_item_bg_calm_daria);
    }

    int getSkinId(Skin skin){
        try{
            return Objects.requireNonNull(skinResMap.get(skin));
        }
        catch (NullPointerException e){
            e.printStackTrace();
            return -1;
        }
    }

    int getShopItemBgIdId(Skin skin){
        try{
            return Objects.requireNonNull(skinShopItemBgMap.get(skin));
        }
        catch (NullPointerException e){
            e.printStackTrace();
            return -1;
        }
    }

    void setActiveSkin(Skin skin){
        setValue(activeSkinKey, skin.toString());
    }

    void applyActiveSkinToLayout(ConstraintLayout layout){
        layout.setBackgroundResource(getSkinId(getActiveSkin()));
    }

    Skin getActiveSkin(){
        String str = getValueString(activeSkinKey);
        if (str.equals("")){
            return Skin.Default;
        }
        else {
            Skin s;
            try{
                s = Skin.valueOf(str);
            }
            catch (Exception e){
                s = Skin.Default;
            }
            return s;
        }
    }

    void setSkinPurchased(Skin skin, boolean v){
        setValue(purchasedSkinPrefix + skin.toString(), v);
    }

    boolean isSkinPurchased(Skin skin){
        return getValueBoolean(
                purchasedSkinPrefix + skin.toString()
        );
    }

    void setSharedPreferences(SharedPreferences _sharedPreferences){
        sharedPreferences = _sharedPreferences;
    }

    void initSharedPreferences(){
        if (0 == getValue(levelUpgKey)){
            setValue(levelUpgKey, 1);
        }

        setSkinResMap();
        setSkinShopItemBgMapMap();

        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putBoolean(isNeedToShowTutorialFirst,
                sharedPreferences.getBoolean(isNeedToShowTutorialFirst, true));
        ed.putBoolean(isNeedToShowTutorialPointer,
                sharedPreferences.getBoolean(isNeedToShowTutorialPointer, true));
        ed.putBoolean(isNeedToShowTutorialPathfinder,
                sharedPreferences.getBoolean(isNeedToShowTutorialPathfinder, true));
        ed.putBoolean(isNeedToShowTutorialTeleport,
                sharedPreferences.getBoolean(isNeedToShowTutorialTeleport, true));
        ed.putBoolean(isNeedToLightBonusButton,
                sharedPreferences.getBoolean(isNeedToLightBonusButton, true));
        ed.putBoolean(isNeedToShowTutorialNextLevel,
                sharedPreferences.getBoolean(isNeedToShowTutorialNextLevel, true));
        ed.putBoolean(isNeedToShowTutorialBonusRange,
                sharedPreferences.getBoolean(isNeedToShowTutorialBonusRange, true));

        ed.putBoolean(isMusicOnKey,
                sharedPreferences.getBoolean(isMusicOnKey, true));
        ed.putBoolean(isSoundsOnKey,
                sharedPreferences.getBoolean(isSoundsOnKey, true));

        if (!isSkinPurchased(Skin.Default)){
            setSkinPurchased(Skin.Default, true);
        }

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
    private void reset(String key){
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
    private void setValue(String key, float value){
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
    String getValueString(String key){
        String result = sharedPreferences.getString(key, "");
        return result;
    }
    void setCameraZ(float z){
        setValue(cameraZKey, z);
    }
    float getCameraZ(){
        float result = sharedPreferences.getFloat(cameraZKey, 10.f);
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
    boolean getIsDebug(){
        boolean result = sharedPreferences.getBoolean(isDebugKey, false);
        return result;
    }

    boolean switchUsesJoystick(){
        boolean result = sharedPreferences.getBoolean(usesJoystickKey, false);
        setValue(usesJoystickKey, !result);
        return !result;
    }
    boolean switchIsDebug(){
        boolean result = sharedPreferences.getBoolean(isDebugKey, false);
        setValue(isDebugKey, !result);
        return !result;
    }

    int getGoldAmount(){
        return sharedPreferences.getInt(goldKey, 0);
    }
    void setGold(int ga){
        setValue(goldKey, ga);
    }
}
