package bakar.labirynth;

import android.graphics.Point;

import java.util.Random;

public class Economist {
    private static final Economist ourInstance = new Economist();

    final int coinCost = 3;

    private final Random random;

    final float nextLevelHypotIncrementation = (float)(5.f * Math.sqrt(2));

    private final LinearFunction avgLengthOfHypot = new LinearFunction(1.7f, 3.f);
    private final LinearFunction levelAmountOfLength = new LinearFunction(0.f, 5.f);

    private final LinearFunction pathfinderCostOfUpgLevel = new LinearFunction(200.f, 400.f);
    private final LinearFunction teleportCostOfUpgLevel = new LinearFunction(230.f, 500.f);
    private final LinearFunction pointerCostOfUpgLevel = new LinearFunction(170f, 300.f);

    private final LinearFunction pathfinderUpgCostOfUpgLevel = new LinearFunction(3000.f, 8000.f);
    private final LinearFunction teleportUpgCostOfUpgLevel = new LinearFunction(4000.f, 12000.f);
    private final LinearFunction pointerUpgCostOfUpgLevel = new LinearFunction(2000.f, 5000.f);

    private final LinearFunction levelRewardOfSide;
    private final LinearFunction coinsAmountOfSide;

    public static Economist getInstance() {
        return ourInstance;
    }

    private Economist(){
        random = new Random(System.currentTimeMillis());

        coinsAmountOfSide = new LinearFunction(.5f, .0f);
        levelRewardOfSide = new LinearFunction((float)coinCost, .0f);
    }

    float getSquareSide(float hypot){
        return (float)(hypot / Math.sqrt(2));
    }

    float getAverageLength(float hypot){
        return avgLengthOfHypot.calc(hypot);
    }

    float getHypot(Point levelSize){
        return (float)Math.sqrt(levelSize.x * levelSize.x + levelSize.y * levelSize.y);
    }

    int getNextLevelCost(float currentHypot){
        return Math.round(getNumOfLevelsToUnlockNext(currentHypot) *
                getLevelTotalIncome(currentHypot, 50.f));
    }

    float getNumOfLevelsToUnlockNext(float hypot){
        return levelAmountOfLength.calc(getAverageLength(hypot));
    }

    int getLevelReward(float hypot){
        return Math.round(levelRewardOfSide.calc(getSquareSide(hypot)));
    }

    int getLevelTotalIncome(float hypot, float coinsPercent){
        int result = 0;
        result += getCoinsAmountAvg(hypot) * coinCost * (coinsPercent / 100.f);
        result += getLevelReward(hypot);
        return result;
    }

    float getCoinsAmountAvg(float hypot){
        return coinsAmountOfSide.calc(getSquareSide(hypot));
    }

    int getCoinsAmountRand(float hypot){
        float avg = getCoinsAmountAvg(hypot);
        return Math.round(avg + random.nextFloat() * avg * 0.15f);
    }

    int getPathfinderCost(int upgLevel){
        return Math.round(pathfinderCostOfUpgLevel.calc(upgLevel));
    }
    int getPointerCost(int upgLevel){
        return Math.round(pointerCostOfUpgLevel.calc(upgLevel));
    }
    int getTeleportCost(int upgLevel){
        return Math.round(teleportCostOfUpgLevel.calc(upgLevel));
    }

    int getPointerUpgCost(int upgLevel){
        return Math.round(pointerUpgCostOfUpgLevel.calc(upgLevel));
    }
    int getPathfinderUpgCost(int upgLevel){
        return Math.round(pathfinderUpgCostOfUpgLevel.calc(upgLevel));
    }
    int getTeleportUpgCost(int upgLevel){
        return Math.round(teleportUpgCostOfUpgLevel.calc(upgLevel));
    }

    private class LinearFunction{
        final private float _coeficient;
        final private float _constant;

        LinearFunction(float coeficient, float constant){
            _coeficient = coeficient;
            _constant = constant;
        }

        float calc(float arg){
            return arg * _coeficient + _constant;
        }
    }
}
