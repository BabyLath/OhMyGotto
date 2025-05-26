package com.ohmygotto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.components.IrremovableComponent;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.ui.ProgressBar;

import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class OhMyGotto extends GameApplication {
    // Game constants
    private static final int WIDTH = 1024; // screen width
    private static final int HEIGHT = 768; // screen height
    private static final int WORLD_WIDTH = WIDTH * 3; // actual world border width
    private static final int WORLD_HEIGHT = HEIGHT * 3; // actual world border height
    private static final int ENEMY_SPAWN_INTERVAL = 5; // seconds
    // private static final int LEVEL_UP_THRESHOLD = 10; // enemies killed to level up
    

    // Game variables
    private Entity player;
    // protected boolean isGameOver = false;
    private boolean isFacingLeft = false;
    private AnimatedTexture playerTexture;
    // private int playerLevel = 1;
    private int baseEnemyHP = 10;
    // private int xpToNextLevel = LEVEL_UP_THRESHOLD;
    private Random random = new Random();
    private ProgressBar xpBar;
    private Text hpText;
    private Text levelText;
    private Text killText;

    private Map<String, Weapon> unlockedWeapons = new HashMap<>();
    private Weapon currentWeapon;
    private Weapon nextWeapon;

    private Text weaponText;
    private Text ammoText;
    private Text nextWeaponText;

    // Stat fields
    private double playerFireRate = 1.0;
    private double playerDamageBoost = 0.0;
    // private double playerSpeed = 200;
    // private int playerMaxHP = 3;
    // private int playerHP = 3;
    // private boolean isInvincible = false;
    // private boolean isMagnetActive = false;
    // protected boolean isGamePaused = false;

    // Enum for game entity types
    public enum EntityType {
        PLAYER, ENEMY, WEAPON, GRID_LINE, EXPERIENCE, BORDER, POWERUP
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setTitle("Oh My Gotto!");
        settings.setVersion("1.0");
        settings.setManualResizeEnabled(false);
        settings.setPreserveResizeRatio(true);
        settings.setAppIcon("icon.png");
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);
    }

    @Override
    protected void initGame() {
        
        try {
            // 1. First initialize the factory with proper access
            GameEntityFactory factory = new GameEntityFactory();
            FXGL.getGameWorld().addEntityFactory(factory);
    
            // 2. Create minimal viable entities first
            createEssentialBackground();
            
            // 3. Spawn player
            player = FXGL.spawn("player", WORLD_WIDTH / 2.0, WORLD_HEIGHT / 2.0);
            FXGL.runOnce(() -> {
                playerTexture = (AnimatedTexture) player.getObject("texture");
            }, Duration.seconds(0.01));

            
            // 4. Set up viewport
            initViewport();
            
            // 5. Schedule delayed initialization for non-critical systems
            initWeaponHUD();
            initWeapons(); // now it can call updateWeaponHUD safely

            // 6. Plays the background music (duh)
            playBackgroundMusic();

            FXGL.runOnce(() -> {
                startEnemySpawner();
                createFullGridBackground();
            }, Duration.seconds(0.1));
            
            // Try catch for error debugging :33 (if nandito parin to it means nakalimutan ko tanggalin)
        } catch (Exception e) {
            System.err.println("CRITICAL: Game initialization failed");
            e.printStackTrace();
            throw new RuntimeException("Game initialization failed", e);
        }
    }
    
    private void createEssentialBackground() {
        // Just a simple black background first
        Rectangle bg = new Rectangle(WORLD_WIDTH, WORLD_HEIGHT, Color.BLACK);
        FXGL.entityBuilder()
            .at(0, 0)
            .view(bg)
            .zIndex(-100)
            .with(new IrremovableComponent())
            .buildAndAttach();
    }

    private void createFullGridBackground() {
        // Separated it cuz of runtime errors
        int gridSize = 100;
        for (int x = 0; x <= WORLD_WIDTH; x += gridSize) {
            // For vertical lines (width=1, height=WORLD_HEIGHT)
            FXGL.spawn("gridLine", 
                new SpawnData(x, 0)
                    .put("width", 1)
                    .put("height", WORLD_HEIGHT)
            );
        }
        for (int y = 0; y <= WORLD_HEIGHT; y += gridSize) {
            // For horizontal lines (width=WORLD_WIDTH, height=1)
            FXGL.spawn("gridLine",
                new SpawnData(0, y)
                    .put("width", WORLD_WIDTH)
                    .put("height", 1)
            );
        }
    }
    
    private void initViewport() {
        FXGL.getGameScene().getViewport().setBounds(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        FXGL.getGameScene().getViewport().bindToEntity(player, WIDTH / 2.0, HEIGHT / 2.0);
        FXGL.getGameScene().getViewport().setLazy(true);
    }

    private void playBackgroundMusic() {
        FXGL.getAudioPlayer().loopMusic(FXGL.getAssetLoader().loadMusic("bgm.mp3"));
    }
    
    private int enemiesPerWave = 1;
    private void startEnemySpawner() {
        FXGL.runOnce(this::spawnEnemyWave, Duration.seconds(ENEMY_SPAWN_INTERVAL));
    }

    private void spawnEnemyWave() {
        if (!GameState.getInstance().isPaused() && !GameState.getInstance().isGameOver()) {
            for (int i = 0; i < enemiesPerWave; i++) {
                spawnEnemy();
            }

            if (GameState.getInstance().getPlayerLevel() % 2 == 0 && enemiesPerWave < 20) {
                enemiesPerWave++;
            }
        }

        // Schedule next wave
        startEnemySpawner();
    }

    // Initilize weapon list
    private void initWeapons() {
        List<String> weaponIds = List.of("SG", "SMG", "AR", "GL", "HG", "SR", "RG", "MG", "RL", "MT", "FT");
        for (String id : weaponIds) {
            unlockedWeapons.put(id, createWeapon(id));
        }
        
        currentWeapon = unlockedWeapons.get("HG");
        updateWeaponHUD();
        /*
        Weapon Types(index):
        Shotgun (SG)
        Submachine Gun (SMG)
        Assault Rifle (AR)
        Grenade Launcher (GL)
        Handgun (HG)
        Sniper Rifle (SR)
        Railgun (RG)
        Minigun (MG)
        Rocket Launcher (RL)
        Mortar (MT)
        Flamethrower (FT)
        */
    }

    private Weapon createWeapon(String id) {
        switch (id) {
            case "SG": return new Weapon(id, 6, 6, 1000 * playerFireRate, 4 + playerDamageBoost, 1.5);
            case "SMG": return new Weapon(id, 30, 30, 500 * playerFireRate, 2 + playerDamageBoost, 1.2);
            case "AR": return new Weapon(id, 20, 20, 250 * playerFireRate, 3 + playerDamageBoost, 2.0);
            case "GL": return new Weapon(id, 3, 3, 1200 * playerFireRate, 8 + playerDamageBoost, 1.8);
            case "HG": return new Weapon(id, 12, 12, 400 * playerFireRate, 2.5 + playerDamageBoost, 1.5);
            case "SR": return new Weapon(id, 5, 5, 1500 * playerFireRate, 15 + playerDamageBoost, 2.8);
            case "RG": return new Weapon(id, 3, 3, 2000 * playerFireRate, 12 + playerDamageBoost, 3.0);
            case "MG": return new Weapon(id, 60, 60, 100 * playerFireRate, 1.8 + playerDamageBoost, 1.0);
            case "RL": return new Weapon(id, 2, 2, 2000 * playerFireRate, 10 + playerDamageBoost, 1.5);
            case "MT": return new Weapon(id, 1, 1, 3000 * playerFireRate, 25 + playerDamageBoost, 2.5);
            case "FT": return new Weapon(id, 50, 50, 100 * playerFireRate, 0.8 + playerDamageBoost, 0.6);
            default: return new Weapon(id, 10, 10, 500 * playerFireRate, 3 + playerDamageBoost, 1.5);
        }
    }

    // Main collision handler
    @Override
    protected void initPhysics() {
        // Collision between player and enemy
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.ENEMY) {
            @Override
            protected void onCollisionBegin(Entity player, Entity enemy) {
                if (GameState.getInstance().isGameOver()) return;
                if (GameState.getInstance().isInvincible()) return; // Ignore collision if invincibility buff is applied
                
                int currentHp = GameState.getInstance().getVar("playerHp", Integer.class);
                if (currentHp > 1) {
                    GameState.getInstance().incVar("playerHp", -1);
                    enemy.removeFromWorld();
                    FXGL.play("hitHurt.wav");
                    updateHpText();
                } else {
                    gameOver();
                }
            }
        });

        // Collision between weapon and enemy
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.WEAPON, EntityType.ENEMY) {
            @Override
            protected void onCollisionBegin(Entity weapon, Entity enemy) {
                if (GameState.getInstance().isGameOver()) return;

                String type = weapon.getString("weaponType");
                double damage = weapon.getDouble("damage");
                Point2D pos = weapon.getCenter();

                // Show damage number
                Point2D dmgpos = enemy.getCenter().subtract(0, 20);
                FXGL.spawn("damageNumber", 
                    new SpawnData(dmgpos.getX(), dmgpos.getY())
                        .put("damage", damage));

                if (type.equals("GL") || type.equals("RL") || type.equals("MT")) {
                    createExplosion(pos);
                    weapon.removeFromWorld();
                    return; // don't continue with regular hit logic
                }

                // Regular projectile hit logic
                int hp = enemy.getInt("hp");
                hp -= damage;

                if (hp <= 0) {
                    enemy.removeFromWorld();
                    FXGL.spawn("experience", enemy.getCenter());
                    GameState.getInstance().incVar("killCount", 1);
                    updateKillText();
                    // checkLevelUp();
                    spawnPowerupOnDeath(enemy.getX(), enemy.getY());
                } else {
                    enemy.setProperty("hp", hp);
                }

                // If flamethower = dont remove projectile on contact
                if (!weapon.getString("weaponType").equals("FT")) {
                    weapon.removeFromWorld();
                }
            }
        });

        // Collision between player and experience
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.EXPERIENCE) {
            @Override
            protected void onCollisionBegin(Entity player, Entity experience) {
                experience.removeFromWorld();
                // GameState.getInstance().incVar("playerXp", 1);
                GameState.getInstance().addXP(1);
                updateXpBar();
                
                // Check for level up
                // checkLevelUp();
            }
        });

        // Collision between player and border
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.BORDER) {
            @Override
            protected void onCollisionBegin(Entity player, Entity border) {
                // Push player away from border
                Point2D center = new Point2D(WORLD_WIDTH/2, WORLD_HEIGHT/2);
                Point2D direction = center.subtract(player.getPosition()).normalize().multiply(10);
                player.translate(direction);
            }
        });

        // Collision between player and powerup
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.POWERUP) {
            @Override
            protected void onCollisionBegin(Entity player, Entity powerup) {
                String type = powerup.getString("powerupType");
                applyPowerup(type);
                powerup.removeFromWorld();
                FXGL.getNotificationService().pushNotification("Powerup: " + type);
            }
        });
    }

    private void initWeaponHUD() {
        // Create background panel
        Rectangle bg = new Rectangle(350, 80, Color.rgb(20, 20, 20, 0.7));
        bg.setArcWidth(15);
        bg.setArcHeight(15);
        bg.setStroke(Color.GRAY);
        bg.setStrokeWidth(1.5);
        bg.setTranslateX(10);
        bg.setTranslateY(HEIGHT - 90);
        
        // Initialize text elements with proper styling
        weaponText = FXGL.getUIFactoryService().newText("", Color.WHITE, 20);
        ammoText = FXGL.getUIFactoryService().newText("", Color.YELLOW, 18);
        nextWeaponText = FXGL.getUIFactoryService().newText("", Color.LIGHTGRAY, 16);
        
        // Position text relative to background
        weaponText.setTranslateX(20);
        weaponText.setTranslateY(HEIGHT - 70);
        
        ammoText.setTranslateX(20);
        ammoText.setTranslateY(HEIGHT - 50);
        
        nextWeaponText.setTranslateX(20);
        nextWeaponText.setTranslateY(HEIGHT - 30);
        
        // Add to scene - BACKGROUND FIRST so text appears on top
        FXGL.addUINode(bg);
        FXGL.addUINode(weaponText);
        FXGL.addUINode(ammoText);
        FXGL.addUINode(nextWeaponText);
        
        // Force immediate update
        updateWeaponHUD();
    }

    private void updateWeaponHUD() {
        if (currentWeapon == null) return;

        weaponText.setText("Weapon: " + currentWeapon.getDisplayName() +"");
        ammoText.setText("Ammo: " + currentWeapon.ammo + "/" + currentWeapon.maxAmmo);
        nextWeaponText.setText("Next: " + (nextWeapon != null ? nextWeapon.getDisplayName() : "-"));

        // Change ammo text color based on status
        if (currentWeapon.ammo <= currentWeapon.maxAmmo * 0.25) {
            ammoText.setFill(Color.RED);
        } else if (currentWeapon.ammo <= currentWeapon.maxAmmo * 0.5) {
            ammoText.setFill(Color.ORANGE);
        } else {
            ammoText.setFill(Color.YELLOW);
        }
    }

    private void handleWeaponReload() {
        List<Weapon> pool = new ArrayList<>(unlockedWeapons.values());
        if (pool.size() > 1) {
            pool.remove(currentWeapon);
        }
        nextWeapon = pool.get(random.nextInt(pool.size()));

        // Flash the weapon text when reloading
        weaponText.setFill(Color.GRAY);
        FXGL.runOnce(() -> {
            if (weaponText != null) {
                weaponText.setFill(Color.WHITE);
            }
        }, Duration.seconds(0.2));

        FXGL.runOnce(() -> {
            currentWeapon = nextWeapon;
            nextWeapon = null;
            currentWeapon.reload();
            updateWeaponHUD();
        }, Duration.seconds(1));

        // Add this to ensure HUD updates after delay
        FXGL.runOnce(() -> {
            updateWeaponHUD();
            // Visual feedback
            weaponText.setFill(Color.CYAN);
            FXGL.runOnce(() -> updateWeaponHUD(), Duration.seconds(0.3));
        }, Duration.seconds(1));
    }

    private void checkLevelUp() {
        // int currentXp = GameState.getInstance().getVar("playerXp", Integer.class);
        
        // if (GameState.getInstance().getCurrentXP() >= GameState.getInstance().getXPToNextLevel()) {
        //     levelUp();
        // }
    }


    private int getPlayerLevel() {
        return GameState.getInstance().getPlayerLevel();
    }

    private void handleLevelUpEffects() {
        if (GameState.getInstance().consumeLevelUpFlag()) {
            FXGL.play("levelup.wav");
            updateLevelText();
            showStatUpgradeOptions();
        }
    }

    private void levelUp() {
        GameState.getInstance().levelUp();
        FXGL.set("playerXp", 0);
        // xpToNextLevel = LEVEL_UP_THRESHOLD * playerLevel;
        
        FXGL.play("levelup.wav"); // Add sound effect
        updateLevelText();
        updateXpBar();
        
        // Show levelup upgrades selection UI
        showStatUpgradeOptions();
    }

    // Show stat upgrade options on level-up
    private void showStatUpgradeOptions() {
        GameState.getInstance().setPaused(true);

        String[] options = { "Fire Rate", "Damage", "Move Speed", "Max Health" };

        VBox box = new VBox(15);
        box.setTranslateX(WIDTH / 2.0 - 150);
        box.setTranslateY(HEIGHT / 2.0 - 100);

        for (String stat : options) {
            Text label = FXGL.getUIFactoryService().newText("• " + stat, Color.WHITE, 18);
            Rectangle bg = new Rectangle(300, 40, Color.DARKSLATEGRAY);
            bg.setArcWidth(8);
            bg.setArcHeight(8);

            StackPane item = new StackPane(bg, label);
            item.setOnMouseClicked(e -> {
                applyStatUpgrade(stat);
                FXGL.getGameScene().removeUINode(box);
                GameState.getInstance().setPaused(false);
            });

            box.getChildren().add(item);
        }

        FXGL.getGameScene().addUINode(box);
    }

    private void applyStatUpgrade(String stat) {
        if (stat == null) return;

        switch (stat) {
            case "Fire Rate":
            GameState.getInstance().modifyFireRate(0.9);
                break;
            case "Damage":
                playerDamageBoost += 1.0;
                break;
            case "Move Speed":
                GameState.getInstance().setPlayerSpeed(
                    GameState.getInstance().getPlayerSpeed() + 20
                );
                break;
            case "Max Health":
                GameState.getInstance().increaseMaxHP(1);
                GameState.getInstance().incVar("playerHp", 1); // either works lol
                updateHpText();
                break;
            default:
                return;
        }
        FXGL.getNotificationService().pushNotification(stat + " upgraded!");
    }

    // Apply powerup effects
    private void applyPowerup(String type) {
        switch (type) {
            case "speed":
                GameState.getInstance().setPlayerSpeed(
                    GameState.getInstance().getPlayerSpeed() + 100
                );
                FXGL.runOnce(() -> GameState.getInstance().setPlayerSpeed(
                    GameState.getInstance().getPlayerSpeed() - 100 ), Duration.seconds(5)
                );
                break;
            case "invincibility":
                GameState.getInstance().setInvincible(true);
                FXGL.runOnce(() -> GameState.getInstance().setInvincible(false), Duration.seconds(4));
                break;
            case "magnet":
                GameState.getInstance().setMagnetActive(true);
                FXGL.runOnce(() -> GameState.getInstance().setMagnetActive(false), Duration.seconds(6));
                break;
            case "heal":
                if(GameState.getInstance().getVar("playerHp", Integer.class) < GameState.getInstance().getPlayerMaxHP()) {
                    GameState.getInstance().incVar("playerHp", 1);
                    updateHpText();
                }
                break;
            default:
                break;
        }
        // FXGL.getNotificationService().pushNotification("Powerup: " + type);
    }

    // Add random powerup drop chance on enemy death
    private void spawnPowerupOnDeath(double x, double y) {
        if (Math.random() < 0.25) {
            String[] types = {"speed", "invincibility", "magnet", "heal"};
            String type = types[(int) (Math.random() * types.length)];
            FXGL.spawn("powerup", new SpawnData(x, y).put("type", type));
        }
    }

    // Exp magnet effect
    private void applyMagnetEffect(double tpf) {
        if (player == null) return;

        double baseAttractDistance = GameState.getInstance().isMagnetActive() ? 250 : 100;
        double baseSpeed = GameState.getInstance().isMagnetActive() ? 150 : 60;

        FXGL.getGameWorld().getEntitiesByType(EntityType.EXPERIENCE).forEach(xp -> {
            double distance = player.getCenter().distance(xp.getCenter());

            if (distance < baseAttractDistance) {
                Point2D direction = player.getCenter().subtract(xp.getCenter()).normalize();
                // double speedMultiplier = 1.0 + (baseAttractDistance - distance) / baseAttractDistance * 2.0;
                double normalizedDistance = 1.0 - (distance / baseAttractDistance);
                double speedMultiplier = 1.0 + normalizedDistance * normalizedDistance * 4.0;
                
                xp.translate(direction.multiply(baseSpeed * speedMultiplier * tpf));
            }
        });
    }

    // Explosions for the 3 explosive type weapons
    private void createExplosion(Point2D center) {
        double radius = 100;

        // Damage all enemies within radius
        FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).forEach(enemy -> {
            double dist = enemy.getCenter().distance(center);
            if (dist <= radius) {
                int hp = enemy.getInt("hp");
                double damage = currentWeapon.damage;

                hp -= damage;
                if (hp <= 0) {
                    enemy.removeFromWorld();
                    FXGL.spawn("experience", enemy.getCenter());
                    GameState.getInstance().incVar("killCount", 1);
                    updateKillText();
                    // checkLevelUp();
                } else {
                    enemy.setProperty("hp", hp);
                }
            }
        });

        // Explosion visual
        Circle fx = new Circle(radius, Color.rgb(255, 100, 0, 0.4));
        fx.setStroke(Color.ORANGERED);
        fx.setStrokeWidth(2);

        Entity explosion = FXGL.entityBuilder()
            .at(center)
            .view(fx)
            .zIndex(500)
            .buildAndAttach();

        FXGL.runOnce(() -> explosion.removeFromWorld(), Duration.seconds(0.4));
    }

    private void updateHpText() {
        hpText.setText("HP: " + GameState.getInstance().getVar("playerHp", Integer.class) + "/" + GameState.getInstance().getPlayerMaxHP());
    }

    private void updateLevelText() {
        levelText.setText("Level: " + GameState.getInstance().getPlayerLevel());
    }

    private void updateKillText() {
        killText.setText("Kills: " + GameState.getInstance().getVar("killCount", Integer.class));
    }

    private void updateXpBar() {
        // int currentXp = GameState.getInstance().getVar("playerXp", Integer.class);
        xpBar.setCurrentValue(GameState.getInstance().getCurrentXP());
        xpBar.setMaxValue(GameState.getInstance().getXPToNextLevel());
        
        // Optional: Add level-up effect
        // if (GameState.getInstance().consumeLevelUpFlag()) {
        //     FXGL.play("levelup.wav");
        //     showStatUpgradeOptions();
        // }
    }

    @Override
    protected void initInput() {
        // Movement controls
        FXGL.onKey(KeyCode.W, () -> movePlayer(0, -1));
        FXGL.onKey(KeyCode.S, () -> movePlayer(0, 1));
        FXGL.onKey(KeyCode.A, () -> movePlayer(-1, 0));
        FXGL.onKey(KeyCode.D, () -> movePlayer(1, 0));
        
        // Attack button
        FXGL.getInput().addAction(new UserAction("Fire") {
            private long lastFireTime = 0;

            @Override
            protected void onAction() {
                if (!GameState.getInstance().isGameOver() && !GameState.getInstance().isPaused() && currentWeapon != null && currentWeapon.canFire()) {
                    long now = System.currentTimeMillis();
                    if (now - lastFireTime >= currentWeapon.fireRate) {
                        fireWeapon(currentWeapon.id);
                        currentWeapon.ammo--;
                        lastFireTime = now;

                        if (currentWeapon.ammo <= 0) {
                            handleWeaponReload();
                        }
                        updateWeaponHUD();
                    }
                }
            }
        }, MouseButton.PRIMARY);
    }

    private void movePlayer(double dx, double dy) {
        if (GameState.getInstance().isGameOver() || GameState.getInstance().isPaused()) return;
        
        // Normalize diagonal movement, without this the player moves faster when moving diagonal
        if (dx != 0 && dy != 0) {
            dx *= 0.7071; // 1/sqrt(2)
            dy *= 0.7071;
        }
        
        player.translate(dx * GameState.getInstance().getPlayerSpeed() * FXGL.tpf(), dy * GameState.getInstance().getPlayerSpeed() * FXGL.tpf());
    }

    private void fireWeapon(String weaponType) {
        double playerCenterX = player.getX() + player.getWidth() / 2;
        double playerCenterY = player.getY() + player.getHeight() / 2;
        Point2D playerPosition = new Point2D(playerCenterX, playerCenterY);
        
        Point2D mousePosition = FXGL.getInput().getMousePositionWorld();
        Point2D direction = mousePosition.subtract(playerPosition).normalize();

        switch (weaponType) {
            case "SG":
                for (int i = -2; i <= 2; i++) {
                    double angle = Math.atan2(direction.getY(), direction.getX()) + Math.toRadians(i * 10);
                    Point2D spread = new Point2D(Math.cos(angle), Math.sin(angle));
                    spawnProjectile(playerPosition, spread, weaponType);
                }
                break;
            case "SMG":
                for (int i = 0; i < 3; i++) {
                    FXGL.runOnce(() -> {
                        Point2D inaccurate = direction.add(Math.random() * 0.1 - 0.05, Math.random() * 0.1 - 0.05).normalize();
                        spawnProjectile(playerPosition, inaccurate, weaponType);
                    }, Duration.millis(i * 50));
                }
                break;
            case "MG":
                Point2D noisy = direction.add(Math.random() * 0.3 - 0.1, Math.random() * 0.3 - 0.1).normalize();
                spawnProjectile(playerPosition, noisy, weaponType);
                break;
            case "SR":
                FXGL.spawn("weapon", new SpawnData(playerPosition.getX(), playerPosition.getY())
                    .put("direction", direction)
                    .put("speed", 900.0)
                    .put("weaponType", weaponType)
                    .put("damage", currentWeapon.damage));
                break;
            case "GL": case "RL": case "MT": {
                Point2D inaccurate = direction.add(Math.random() * 0.1 - 0.05, Math.random() * 0.1 - 0.05).normalize();
                FXGL.spawn("explosive", new SpawnData(playerPosition.getX(), playerPosition.getY())
                    .put("direction", inaccurate)
                    .put("speed", weaponType.equals("MT") ? 200.0 : 300.0)
                    .put("weaponType", weaponType)
                    .put("damage", currentWeapon.damage)
                    .put("range", currentWeapon.range));
                break;
            }
            case "RG":
                FXGL.spawn("railbeam", new SpawnData(playerPosition.getX(), playerPosition.getY())
                    .put("direction", direction)
                    .put("damage", currentWeapon.damage));
                break;
            case "FT":
                for (int i = 0; i < 5; i++) {
                    Point2D offset = direction.add(Math.random() - 0.5, Math.random() - 0.5).normalize();
                    FXGL.spawn("weapon", new SpawnData(playerPosition.getX(), playerPosition.getY())
                        .put("direction", offset)
                        .put("speed", 100.0)
                        .put("weaponType", weaponType)
                        .put("damage", currentWeapon.damage)
                        .put("lifespan", 0.5));
                }
                break;
            case "AR": case "HG": {
                Point2D inaccurate = direction.add(Math.random() * 0.05 - 0.025, Math.random() * 0.05 - 0.025).normalize();
                spawnProjectile(playerPosition, inaccurate, weaponType);
                break;
            }
            default:
                spawnProjectile(playerPosition, direction, weaponType);
                break;
        }

        FXGL.play("shoot.wav");
    }

    private void spawnProjectile(Point2D position, Point2D direction, String weaponType) {
        FXGL.spawn("weapon", new SpawnData(position.getX(), position.getY())
            .put("direction", direction)
            .put("speed", 500.0)
            .put("weaponType", weaponType)
            .put("damage", currentWeapon.damage));
    }

    private void spawnEnemy() {
        if (GameState.getInstance().isGameOver() || GameState.getInstance().isPaused()) return;
        
        // Get player position
        Point2D playerPos = player.getPosition();
        
        // Spawn enemy outside screen but inside world bounds
        double spawnDistance = 800;
        double angle = random.nextDouble() * 360;
        double x = playerPos.getX() + spawnDistance * Math.cos(Math.toRadians(angle));
        double y = playerPos.getY() + spawnDistance * Math.sin(Math.toRadians(angle));
        
        // Keep within world bounds
        x = Math.max(50, Math.min(WORLD_WIDTH - 50, x));
        y = Math.max(50, Math.min(WORLD_HEIGHT - 50, y));
        
        // Spawn enemy
        Entity enemy = FXGL.spawn("enemy", new SpawnData(x, y));
        enemy.setProperty("hp", baseEnemyHP + GameState.getInstance().getPlayerLevel() * 2);
        enemy.setProperty("speed", 100 + GameState.getInstance().getPlayerLevel() * 2);
    }

    // function to keep player from going out of bounds
    private void clampPlayerPosition() {
        double x = Math.max(0, Math.min(WORLD_WIDTH - player.getWidth(), player.getX()));
        double y = Math.max(0, Math.min(WORLD_HEIGHT - player.getHeight(), player.getY()));
        player.setPosition(x, y);
    }

    // game variables (couldve just used normal variables but this is easier to use especialy with the HUD)
    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("playerHp", 3);
        vars.put("playerXp", 0);
        vars.put("killCount", 0);
    }

    @Override
    protected void initUI() {
        // Create UI elements
        hpText = FXGL.getUIFactoryService().newText("HP: " + GameState.getInstance().getVar("playerHp", Integer.class) + "/" +  GameState.getInstance().getPlayerMaxHP(), Color.WHITE, 24);
        levelText = FXGL.getUIFactoryService().newText("Level: " + GameState.getInstance().getPlayerLevel(), Color.WHITE, 24);
        killText = FXGL.getUIFactoryService().newText("Kills: 0", Color.WHITE, 24);
        
        // Position UI elements
        hpText.setTranslateX(20);
        hpText.setTranslateY(40);
        levelText.setTranslateX(20);
        levelText.setTranslateY(70);
        killText.setTranslateX(20);
        killText.setTranslateY(100);
        
        // Create XP bar
        xpBar = new ProgressBar(false);
        xpBar.setFill(Color.GREEN);
        xpBar.setBackgroundFill(Color.DARKGRAY);
        xpBar.setMinValue(0);
        xpBar.setMaxValue(GameState.LEVEL_UP_THRESHOLD);
        xpBar.setCurrentValue(0);
        xpBar.setWidth(200);
        xpBar.setHeight(15);
        xpBar.setTranslateX(WIDTH - 220);
        xpBar.setTranslateY(20);
        
        Text xpLabel = FXGL.getUIFactoryService().newText("XP", Color.WHITE, 18);
        xpLabel.setTranslateX(WIDTH - 250);
        xpLabel.setTranslateY(32);
        
        // Add UI elements to scene
        FXGL.addUINode(hpText);
        FXGL.addUINode(levelText);
        FXGL.addUINode(killText);
        FXGL.addUINode(xpBar);
        FXGL.addUINode(xpLabel);
    }

    private void gameOver() {
        GameState.getInstance().setGameOver(true);
        FXGL.play("gameover.wav");
        
        Text gameOverText = FXGL.getUIFactoryService().newText("GAME OVER", Color.RED, 72);
        gameOverText.setTranslateX(WIDTH / 2.0 - 180);
        gameOverText.setTranslateY(HEIGHT / 2.0);
        
        Text finalScoreText = FXGL.getUIFactoryService().newText(
            "Survived to Level: " + GameState.getInstance().getPlayerLevel() + "\nEnemies Killed: " + GameState.getInstance().getVar("killCount", Integer.class), 
            Color.WHITE, 36
        );
        finalScoreText.setTranslateX(WIDTH / 2.0 - 150);
        finalScoreText.setTranslateY(HEIGHT / 2.0 + 100);
        
        FXGL.addUINode(gameOverText);
        FXGL.addUINode(finalScoreText);
    }

    // fxgl's built-in callback method to update game logic per frame(tpf) sabihin mo to kung nagtanong si sir xDD
    @Override
    protected void onUpdate(double tpf) {
        if (GameState.getInstance().isGameOver() || GameState.getInstance().isPaused()) return;

        clampPlayerPosition();
        applyMagnetEffect(tpf);
        handleLevelUpEffects();

        // For player sprite face direction
        if (player != null && playerTexture != null) {
            Point2D playerCenter = player.getCenter();
            Point2D mousePos = FXGL.getInput().getMousePositionWorld();

            boolean shouldFaceLeft = mousePos.getX() < playerCenter.getX();
            if (shouldFaceLeft != isFacingLeft) {
                playerTexture.setScaleX(shouldFaceLeft ? -1 : 1);
                isFacingLeft = shouldFaceLeft;
            }
        }
        
        // Update enemies to move toward player
        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY);
        Point2D playerPos = player.getPosition();

        for (Entity enemy : enemies) {
            Point2D direction = playerPos.subtract(enemy.getPosition()).normalize();
            double speed = 100 + (GameState.getInstance().getPlayerLevel() * 5); // Enemies get faster as levels increase
            enemy.translate(direction.multiply(speed * tpf));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
