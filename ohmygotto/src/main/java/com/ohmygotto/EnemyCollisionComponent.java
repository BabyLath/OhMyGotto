package com.ohmygotto;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.ohmygotto.OhMyGotto.EntityType;

import javafx.geometry.Point2D;

public class EnemyCollisionComponent extends Component {
    private double pushForce = 30.0;
    private double detectionRadius = 40.0;

    @Override
    public void onUpdate(double tpf) {
        // Get all nearby enemies
        FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).forEach(other -> {
            if (other != entity) { // Don't check collision with self
                double distance = entity.getCenter().distance(other.getCenter());
                
                if (distance < detectionRadius) {
                    // Calculate push direction
                    Point2D direction = entity.getCenter().subtract(other.getCenter()).normalize();
                    
                    // Apply push force inversely proportional to distance
                    double force = pushForce * (1 - (distance / detectionRadius));
                    entity.translate(direction.multiply(force * tpf));
                }
            }
        });
    }
}