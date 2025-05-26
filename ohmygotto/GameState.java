package com.ohmygotto;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized game state management using singleton pattern.
 * Tracks all game progress, player stats, and global variables.
 */
public class GameState {
    // Singleton instance
    private static final GameState instance = new GameState();
    
    // Game flow states
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean invincible = false;
    private boolean magnetActive = false;
    private boolean justLeveledUp = false;

    // Game constants
    public static final int LEVEL_UP_THRESHOLD = 5; // Now constants live here
    
    // Player attributes
    private int playerLevel = 1;
    private double playerFireRate = 1.0;
    private double playerDamageBoost = 0.0;
    private double playerSpeed = 200;
    private int playerMaxHP = 3;
    private int xpToNextLevel = 2;
    
    // Dynamic variables storage (replaces FXGL.geti()/seti())
    private final Map<String, Object> gameVars = new HashMap<>() {{
        put("playerHp", 3);
        put("playerXp", 0);
        put("killCount", 0);
    }};

    // Private constructor for singleton
    private GameState() {}

    public static GameState getInstance() {
        return instance;
    }

    //================ Game Flow Controls ================//
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    
    public boolean isInvincible() { return invincible; }
    public void setInvincible(boolean invincible) { this.invincible = invincible; }
    
    public boolean isMagnetActive() { return magnetActive; }
    public void setMagnetActive(boolean magnetActive) { this.magnetActive = magnetActive; }

    public boolean didLevelUp() {
        return justLeveledUp;
    }
    public boolean consumeLevelUpFlag() {
        if (justLeveledUp) {
            justLeveledUp = false;
            return true;
        }
        return false;
    }

    

    //================ Player Stats ================//
    public int getPlayerLevel() { return playerLevel; }
    public void levelUp() { 
        playerLevel++; 
        xpToNextLevel = LEVEL_UP_THRESHOLD * playerLevel;
        setVar("playerXp", 0);
        justLeveledUp = true;
    }
    
    public double getPlayerFireRate() { return playerFireRate; }
    public void modifyFireRate(double multiplier) { playerFireRate *= multiplier; }
    
    public double getPlayerDamageBoost() { return playerDamageBoost; }
    public void addDamageBoost(double amount) { playerDamageBoost += amount; }
    
    public double getPlayerSpeed() { return playerSpeed; }
    public void setPlayerSpeed(double speed) { playerSpeed = speed; }
    
    public int getPlayerMaxHP() { return playerMaxHP; }
    public void increaseMaxHP(int amount) { playerMaxHP += amount; }

    //================ Variable Storage ================//
    public <T> T getVar(String key, Class<T> type) {
        return type.cast(gameVars.get(key));
    }
    
    public void setVar(String key, Object value) {
        gameVars.put(key, value);
    }
    
    public void incVar(String key, Number amount) {
        Object current = gameVars.get(key);
        if (current instanceof Integer) {
            gameVars.put(key, (Integer)current + amount.intValue());
        } else if (current instanceof Double) {
            gameVars.put(key, (Double)current + amount.doubleValue());
        }
    }

    //================ XP System ================//
    public int getCurrentXP() { return getVar("playerXp", Integer.class); }
    public int getXPToNextLevel() { return xpToNextLevel; }
    
    public void addXP(int amount) {
        int newXP = getVar("playerXp", Integer.class) + amount;
        setVar("playerXp", newXP);
        
        if (newXP >= xpToNextLevel) {
            levelUp();
            setVar("playerXp", 0); // Reset XP after level up
        }
    }

    //================ Helper Methods ================//
    public void resetGameState() {
        paused = false;
        gameOver = false;
        invincible = false;
        magnetActive = false;
        playerLevel = 1;
        playerFireRate = 1.0;
        playerDamageBoost = 0.0;
        playerSpeed = 200;
        playerMaxHP = 3;
        xpToNextLevel = 10;
        gameVars.replaceAll((k,v) -> 0);
        gameVars.put("playerHp", 3);
    }
}