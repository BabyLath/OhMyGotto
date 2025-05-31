package com.ohmygotto;

import java.util.ArrayList;
import java.util.List;

import com.almasb.fxgl.dsl.EntityBuilder;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.entity.components.IrremovableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.ohmygotto.OhMyGotto.EntityType;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class GameEntityFactory implements EntityFactory {
    private List<Entity> enemies = new ArrayList<>(); // arraylist if ever na different sprites for the enemies

    int spriteWidth = 64;
    int spriteHeight = 64;

    int hitboxWidth = 44;
    int hitboxHeight = 44;

    double offsetX = (spriteWidth - hitboxWidth) / 2.0;
    double offsetY = (spriteHeight - hitboxHeight) / 2.0;

    @Spawns("player")
    public Entity spawnPlayer(SpawnData data) {
        Image image = FXGL.image("player.png");
        AnimationChannel animChannel = new AnimationChannel(image, 4, spriteWidth, spriteHeight, Duration.seconds(1), 0, 3);
        AnimatedTexture texture = new AnimatedTexture(animChannel);
        texture.loop();

        Entity player = FXGL.entityBuilder(data)
            .type(EntityType.PLAYER)
            .view(texture)
            .bbox(new HitBox("PLAYER_HITBOX", new Point2D(offsetX, offsetY), BoundingShape.box(hitboxWidth, hitboxHeight))) // custom hitbox
            .with(new CollidableComponent(true))
            .build();

        player.setProperty("texture", texture);
        return player;
    }

    @Spawns("enemy")
    public Entity spawnEnemy(SpawnData data) {
        Image image = FXGL.image("enemy.png");
        AnimationChannel animChannel = new AnimationChannel(image, 6, spriteWidth, spriteHeight, Duration.seconds(1), 0, 5);
        AnimatedTexture enemyTexture = new AnimatedTexture(animChannel);
        enemyTexture.loop();

        Entity enemy = FXGL.entityBuilder(data)
            .type(EntityType.ENEMY)
            .view(enemyTexture)
            .bbox(new HitBox("ENEMY_HITBOX", new Point2D(offsetX, offsetY), BoundingShape.box(hitboxWidth, hitboxHeight))) // custom hitbox
            .with(new CollidableComponent(true))
            .with(new EnemyChaser())
            .with(new EnemyCollisionComponent())
            .build();

        return enemy;
    }

    @Spawns("gridLine")
    public Entity spawnGridLine(SpawnData data) {
        int width = data.get("width");
        int height = data.get("height");
        
        return FXGL.entityBuilder(data)
            .type(EntityType.GRID_LINE)
            .view(new Rectangle(width, height, Color.rgb(0, 0, 255, 0.3))) // Semi-transparent blue
            .zIndex(-50)
            .with(new IrremovableComponent())
            .build();
    }
    
    @Spawns("weapon")
    public Entity spawnWeapon(SpawnData data) {
        String weaponType = data.get("weaponType");
        Point2D direction = data.hasKey("direction") ? data.get("direction") : null;
        double speed = data.hasKey("speed") ? data.get("speed") : 0;
    
        EntityBuilder weapon = FXGL.entityBuilder(data)
            .type(OhMyGotto.EntityType.WEAPON)
            .with(new CollidableComponent(true))
            .with("weaponType", weaponType);
    
        switch (weaponType) {
            case "HG":
                weapon.viewWithBBox(new Rectangle(8, 8, Color.YELLOW));
                break;
            case "SG":
                weapon.viewWithBBox(new Rectangle(8, 8, Color.ORANGE));
                break;
            case "AR":
                weapon.viewWithBBox(new Rectangle(10, 10, Color.LIGHTYELLOW));
                break;
            case "SR":
                weapon.viewWithBBox(new Rectangle(10, 10, Color.WHITESMOKE));
                break;
            case "GL": case "MT": {
                weapon.viewWithBBox(new Rectangle(12, 12, Color.DARKGREEN));
                break;
            }
            case "FT":
                weapon.viewWithBBox(new Rectangle(8, 8, Color.ORANGERED));
                break;
            case "RL":
                weapon.viewWithBBox(new Rectangle(20, 8, Color.DARKGRAY));
                break;
            default:
                weapon.viewWithBBox(new Rectangle(10, 10, Color.WHITE));
        }
    
        if (direction != null) {
            Entity projectileEntity = weapon.build();
            projectileEntity.addComponent(new ProjectileComponent(direction, speed));
        
            // Despawn fallback
            FXGL.runOnce(() -> {
                if (projectileEntity.isActive()) {
                    projectileEntity.removeFromWorld();
                    // Checks if projectile actually despawns(cant check it if its out of the screen ykyk)
                    System.out.println("Despawning: " + projectileEntity);
                }
            }, Duration.seconds(3)); // lifespan
        
            return projectileEntity;
        }
        
    
        if (data.hasKey("lifespan")) {
            double lifespan = data.get("lifespan");
            Entity builtWeapon = weapon.build();
            FXGL.runOnce(() -> {
                if (builtWeapon.isActive()) builtWeapon.removeFromWorld();
            }, Duration.seconds(lifespan));
            return builtWeapon;
        }
    
        return weapon.build();
    }
    
    @Spawns("experience")
    public Entity spawnExperience(SpawnData data) {
        return FXGL.entityBuilder(data)
            .type(OhMyGotto.EntityType.EXPERIENCE)
            // .viewWithBBox(new Rectangle(10, 10, Color.GREEN))
            .viewWithBBox(new Circle(5, Color.GREEN))
            .with(new CollidableComponent(true))
            .build();
    }

    @Spawns("powerup")
    public Entity spawnPowerup(SpawnData data) {
        String type = data.get("type");

        Color color;
        switch (type) {
            case "speed":
                color = Color.LIGHTGREEN;
                break;
            case "invincibility":
                color = Color.YELLOW;
                break;
            case "magnet":
                color = Color.CYAN;
                break;
            case "heal":
                color = Color.RED;
                break;
            default:
                color = Color.WHITE;
                break;
        }

        Circle icon = new Circle(10, color);
        icon.setStroke(Color.WHITE);
        icon.setStrokeWidth(1.5);

        return FXGL.entityBuilder(data)
            .type(EntityType.POWERUP)
            .viewWithBBox(icon)
            .with(new CollidableComponent(true))
            .with("powerupType", type)
            .zIndex(100)
            .build();
    }
    
    @Spawns("border")
    public Entity spawnBorder(SpawnData data) {
        int width = data.get("width");
        int height = data.get("height");
        
        return FXGL.entityBuilder(data)
            .type(OhMyGotto.EntityType.BORDER)
            .viewWithBBox(new Rectangle(width, height, Color.DARKRED))
            .with(new CollidableComponent(true))
            .build();
    }

    @Spawns("explosive")
    public Entity spawnExplosive(SpawnData data) {
        Point2D direction = data.get("direction");
        double speed = data.get("speed");
        double damage = data.get("damage");
        double range = data.hasKey("range") ? data.get("range") : 1.5;
        String type = data.get("weaponType");

        Entity projectile = FXGL.entityBuilder(data)
            .type(OhMyGotto.EntityType.WEAPON)
            .viewWithBBox(new Circle(8, Color.DARKRED))
            .with(new CollidableComponent(true))
            .with("weaponType", type)
            .with("damage", damage)
            .build();

        projectile.addComponent(new ProjectileComponent(direction, speed));

        // Manually trigger AoE after delay (parang fuse)
        FXGL.runOnce(() -> {
            if (projectile.isActive()) {
                createExplosion(projectile.getCenter(), damage);
                projectile.removeFromWorld();
            }
        }, Duration.seconds(range));

        return projectile;
    }

    private void createExplosion(Point2D center, double damage) {
    FXGL.getGameWorld().getEntitiesByType(OhMyGotto.EntityType.ENEMY).forEach(enemy -> {
        if (enemy.getCenter().distance(center) < 100) {
            int hp = enemy.getInt("hp") - (int) damage;
            if (hp <= 0) enemy.removeFromWorld();
            else enemy.setProperty("hp", hp);
        }
    });

    // Create a proper FXGL entity with a circle view
    FXGL.spawn("explosionEffect", new SpawnData(center.getX() - 50, center.getY() - 50));
}


    @Spawns("explosionEffect")
    public Entity spawnExplosionEffect(SpawnData data) {
        // explosion visual effect
        Circle fx = new Circle(100, Color.rgb(255, 100, 0, 0.4));
        fx.setStroke(Color.ORANGERED);
        fx.setStrokeWidth(2);

        Entity explosion = FXGL.entityBuilder()
            .at(data.getX(), data.getY())
            .view(fx)
            .zIndex(500) // ensure it's on top
            .build();

        // check if explosion actually spawns
        System.out.println("Spawning visual explosion entity at: " + data.getX() + ", " + data.getY());

        FXGL.getGameWorld().addEntity(explosion);
        FXGL.runOnce(explosion::removeFromWorld, Duration.seconds(0.4));

        return explosion;
    }

    @Spawns("railbeam")
    public Entity spawnRailBeam(SpawnData data) {
        Point2D origin = new Point2D(data.getX(), data.getY());
        Point2D direction = data.get("direction");
        double damage = data.get("damage");

        Point2D end = origin.add(direction.multiply(1600));

        Line beamLine = new Line(origin.getX(), origin.getY(), end.getX(), end.getY());
        beamLine.setStroke(Color.CYAN);
        beamLine.setStrokeWidth(4);
        beamLine.setOpacity(0.9);

        Entity beam = FXGL.entityBuilder()
            .view(beamLine)
            .zIndex(1000)
            .build();

        FXGL.getGameWorld().addEntity(beam);
        FXGL.runOnce(beam::removeFromWorld, Duration.seconds(0.2));

        // Damage logic
        FXGL.getGameWorld().getEntitiesByType(OhMyGotto.EntityType.ENEMY).forEach(enemy -> {
            Point2D enemyPos = enemy.getCenter();
            Point2D v = enemyPos.subtract(origin);
            double projection = v.dotProduct(direction);

            if (projection >= 0 && projection <= 1600) {
                Point2D closest = origin.add(direction.multiply(projection));
                if (enemyPos.distance(closest) < 20) {
                    int hp = enemy.getInt("hp") - (int) damage;
                    
                    if (hp <= 0) {
                        FXGL.spawn("experience", enemy.getCenter());
                        GameState.getInstance().incVar("killCount", 1);
                        enemy.removeFromWorld();
                    }
                    else enemy.setProperty("hp", hp);
                }
            }
        });

        return beam;
    }

    @Spawns("damageNumber")
    public Entity spawnDamageNumber(SpawnData data) {
        double damage = data.get("damage");
        return FXGL.entityBuilder(data)
            .view(new Text()) // Empty, will be set by component
            .with(new DamageNumberComponent(damage))
            .zIndex(1000)
            .build();
    }
}