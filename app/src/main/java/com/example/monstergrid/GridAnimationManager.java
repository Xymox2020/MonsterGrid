package com.example.monstergrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class GridAnimationManager {

    public interface AnimationCallback {
        void onFinished();
    }

    private static final String DEFAULT_CELL_COLOR = "#1A1A1A";

    /**
     * Animates a character moving from one cell to another.
     */
    public static void animateStationaryToTarget(TextView sourceCell, TextView targetCell, String characterTag, int targetBgColor, AnimationCallback callback) {
        // Essential: Stop any previous animations on these cells and reset their state
        // to ensure getLocationInWindow returns the base layout position.
        sourceCell.animate().cancel();
        sourceCell.setTranslationX(0);
        sourceCell.setTranslationY(0);
        sourceCell.setScaleX(1.0f);
        sourceCell.setScaleY(1.0f);

        targetCell.animate().cancel();
        targetCell.setTranslationX(0);
        targetCell.setTranslationY(0);
        targetCell.setScaleX(1.0f);
        targetCell.setScaleY(1.0f);
        
        int[] sourcePos = new int[2];
        int[] targetPos = new int[2];
        sourceCell.getLocationInWindow(sourcePos);
        targetCell.getLocationInWindow(targetPos);

        float diffX = sourcePos[0] - targetPos[0];
        float diffY = sourcePos[1] - targetPos[1];

        // Prepare target view visually at the START position
        targetCell.setText(characterTag);
        targetCell.setBackgroundColor(targetBgColor);
        targetCell.setTranslationX(diffX);
        targetCell.setTranslationY(diffY);
        targetCell.setAlpha(1.0f);
        
        // Clear source view
        sourceCell.setText("");
        sourceCell.setBackgroundColor(Color.parseColor(DEFAULT_CELL_COLOR));

        // Bring target to front to ensure it's above other elements during move
        targetCell.bringToFront();

        targetCell.animate()
                .translationX(0)
                .translationY(0)
                .setDuration(300) 
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (callback != null) callback.onFinished();
                    }
                })
                .start();
    }

    /**
     * Animates a projectile (bullet) from source to target.
     */
    public static void animateProjectile(View sourceCell, View targetCell, FrameLayout effectLayer, AnimationCallback callback) {
        View bullet = new View(sourceCell.getContext());
        int size = sourceCell.getWidth() / 4;
        bullet.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        bullet.setBackgroundColor(Color.YELLOW);
        effectLayer.addView(bullet);

        int[] sourcePos = new int[2];
        int[] targetPos = new int[2];
        int[] layerPos = new int[2];
        
        sourceCell.getLocationInWindow(sourcePos);
        targetCell.getLocationInWindow(targetPos);
        effectLayer.getLocationInWindow(layerPos);

        float startX = sourcePos[0] - layerPos[0] + (sourceCell.getWidth() / 2f) - (size / 2f);
        float startY = sourcePos[1] - layerPos[1] + (sourceCell.getHeight() / 2f) - (size / 2f);
        float endX = targetPos[0] - layerPos[0] + (targetCell.getWidth() / 2f) - (size / 2f);
        float endY = targetPos[1] - layerPos[1] + (targetCell.getHeight() / 2f) - (size / 2f);

        bullet.setX(startX);
        bullet.setY(startY);

        bullet.animate()
                .x(endX)
                .y(endY)
                .setDuration(250)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        effectLayer.removeView(bullet);
                        showImpactEffect(targetCell, effectLayer, callback);
                    }
                })
                .start();
    }

    private static void showImpactEffect(View targetCell, FrameLayout effectLayer, AnimationCallback callback) {
        View impact = new View(targetCell.getContext());
        int size = (int)(targetCell.getWidth() * 1.2);
        impact.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        impact.setBackgroundColor(Color.WHITE);
        impact.setAlpha(0.8f);
        effectLayer.addView(impact);

        int[] targetPos = new int[2];
        int[] layerPos = new int[2];
        targetCell.getLocationInWindow(targetPos);
        effectLayer.getLocationInWindow(layerPos);

        impact.setX(targetPos[0] - layerPos[0] - (size - targetCell.getWidth())/2f);
        impact.setY(targetPos[1] - layerPos[1] - (size - targetCell.getHeight())/2f);

        impact.animate()
                .scaleX(1.8f)
                .scaleY(1.8f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        effectLayer.removeView(impact);
                        if (callback != null) callback.onFinished();
                    }
                })
                .start();
    }
}
