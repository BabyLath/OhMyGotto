package com.ohmygotto;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class DamageNumberComponent extends Component {
    private Text text;
    private double time = 0;
    private double duration = 0.8;
    
    public DamageNumberComponent(double damage) {
        text = new Text(String.format("%.0f", damage));
        text.setFill(damage >= 10 ? Color.RED : Color.YELLOW);
        text.setStroke(damage >= 10 ? Color.WHITE : Color.BLACK);
        text.setStrokeWidth(0.5);
    }
    
    @Override
    public void onAdded() {
        entity.getViewComponent().addChild(text);
        FXGL.runOnce(() -> entity.removeFromWorld(), Duration.seconds(duration));
    }
    
    @Override
    public void onUpdate(double tpf) {
        time += tpf;
        double progress = time / duration;
        
        text.setTranslateY(-50 * progress);
        text.setOpacity(1 - progress * 0.8);
    }
}