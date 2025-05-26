package com.ohmygotto;

import com.almasb.fxgl.entity.component.Component;

import javafx.geometry.Point2D;

public class ProjectileComponent extends Component {
    private Point2D direction;
    private double speed;
    
    public ProjectileComponent(Point2D direction, double speed) {
        this.direction = direction;
        this.speed = speed;
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translate(direction.multiply(speed * tpf));
        
        // Check if projectile is out of bounds
        if (entity.getX() < 0 || entity.getX() > 3 * 1024 || 
            entity.getY() < 0 || entity.getY() > 3 * 768) {
            entity.removeFromWorld();
        }
    }
}