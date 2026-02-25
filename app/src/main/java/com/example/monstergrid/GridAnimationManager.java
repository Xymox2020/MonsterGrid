package com.example.monstergrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class GridAnimationManager {

    public interface AnimationCallback {
        void onFinished();
    }

    /**
     * Animates a character moving from one cell to another.
     */
    public static void animateStationaryToTarget(TextView sourceCell, TextView targetCell, String characterTag, int targetBgColor, AnimationCallback callback) {
        float startX = sourceCell.getX();
        float startY = sourceCell.getY();
        float endX = targetCell.getX();
        float endY = targetCell.getY();

        targetCell.setText(characterTag);
        targetCell.setBackgroundColor(targetBgColor);
        
        sourceCell.setText("");
        sourceCell.setBackgroundColor(Color.parseColor("#222222"));

        targetCell.setTranslationX(startX - endX);
        targetCell.setTranslationY(startY - endY);

        targetCell.animate()
                .translationX(0)
                .translationY(0)
                .setDuration(350) // Slightly longer for "smoothness"
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (callback != null) callback.onFinished();
                })
                .start();
    }

    /**
     * Animates a projectile (bullet) from source to target.
     */
    public static void animateProjectile(View sourceCell, View targetCell, FrameLayout effectLayer, AnimationCallback callback) {
        // Create bullet view
        View bullet = new View(sourceCell.getContext());
        int size = sourceCell.getWidth() / 4;
        bullet.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        bullet.setBackgroundColor(Color.YELLOW);
        effectLayer.addView(bullet);

        // Calculate positions relative to effectLayer
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
                .setDuration(200)
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
        int size = targetCell.getWidth();
        impact.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        impact.setBackgroundColor(Color.WHITE);
        impact.setAlpha(0.6f);
        effectLayer.addView(impact);

        int[] targetPos = new int[2];
        int[] layerPos = new int[2];
        targetCell.getLocationInWindow(targetPos);
        effectLayer.getLocationInWindow(layerPos);

        impact.setX(targetPos[0] - layerPos[0]);
        impact.setY(targetPos[1] - layerPos[1]);

        impact.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .alpha(0f)
                .setDuration(150)
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
