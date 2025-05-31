package com.ohmygotto;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.time.LocalTimer;
import com.ohmygotto.OhMyGotto.EntityType;

import javafx.geometry.Point2D;
import javafx.util.Duration;

public class EnemyChaser extends Component {
    private Entity player;
    private Point2D velocity = new Point2D(0, 0);
    private LocalTimer changeDirectionTimer;
    private double speed = 1.0;

    @Override
    public void onAdded() {
        // Initialize timer to change direction every x(speed) seconds
        changeDirectionTimer = FXGL.newLocalTimer();
        changeDirectionTimer.capture();
        
        // Get the player entity
        player = FXGL.getGameWorld().getEntitiesByType(EntityType.PLAYER).get(0);
    }

    @Override
    public void onUpdate(double tpf) {
        
        // Don't move if game is paused or over
        if (GameState.getInstance().isPaused() || GameState.getInstance().isGameOver()) {
            return;
        }

        if (changeDirectionTimer.elapsed(Duration.seconds(1))) {
            // Calculate direction toward player and normalize it
            velocity = player.getPosition()
                .subtract(entity.getPosition())
                .normalize()
                .multiply(speed);

            changeDirectionTimer.capture(); // Reset the timer
        }
        
        // Move the entity
        entity.translate(velocity);
    }
}