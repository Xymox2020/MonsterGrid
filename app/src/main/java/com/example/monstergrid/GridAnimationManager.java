package com.example.monstergrid;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

public class GridAnimationManager {

    public interface AnimationCallback {
        void onFinished();
    }

    /**
     * Animates a character moving from one cell to another.
     * Logic: We place the character in the target cell immediately, 
     * but offset its position to the source cell and animate it back to center.
     */
    public static void animateStationaryToTarget(TextView sourceCell, TextView targetCell, String characterTag, int targetBgColor, AnimationCallback callback) {
        // 1. Get positions
        float startX = sourceCell.getX();
        float startY = sourceCell.getY();
        float endX = targetCell.getX();
        float endY = targetCell.getY();

        // 2. Prepare target cell visually
        targetCell.setText(characterTag);
        targetCell.setBackgroundColor(targetBgColor);
        
        // 3. Clear source cell
        sourceCell.setText("");
        sourceCell.setBackgroundColor(0xFF222222); // Reset to default grid color

        // 4. Calculate translation offset
        targetCell.setTranslationX(startX - endX);
        targetCell.setTranslationY(startY - endY);

        // 5. Animate to (0,0) which is the actual position in the GridLayout
        targetCell.animate()
                .translationX(0)
                .translationY(0)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (callback != null) callback.onFinished();
                })
                .start();
    }
}
