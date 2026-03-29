package com.example.monstergrid;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.Random;

public class ShimmerImageView extends AppCompatImageView {
    private Paint mPaint = new Paint();
    private float mProgress = -3f;
    private LinearGradient mShader;
    private Matrix mMatrix = new Matrix();
    private ValueAnimator mAnimator;

    public ShimmerImageView(Context context) { super(context); }
    public ShimmerImageView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void startShimmer() {
        if (mAnimator != null) mAnimator.cancel();
        // Wider range to ensure it starts and ends completely off-screen
        mAnimator = ValueAnimator.ofFloat(-2.5f, 3.5f);
        mAnimator.setDuration(3000); // Total cycle duration
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setStartDelay(new Random().nextInt(1000));
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Create a layer to apply Xfermode correctly (masks the shimmer to the image content)
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        
        super.onDraw(canvas);

        // Only draw the shimmer if it's within a reasonable range of the view
        if (mProgress > -2.5f && mProgress < 3.5f) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                if (mShader == null) {
                    // Create a sharp, high-quality diagonal glare
                    mShader = new LinearGradient(0, 0, w * 0.6f, h,
                            new int[]{0x00FFFFFF, 0x00FFFFFF, 0xB3FFFFFF, 0x00FFFFFF, 0x00FFFFFF},
                            new float[]{0f, 0.35f, 0.5f, 0.65f, 1f}, Shader.TileMode.CLAMP);
                    mPaint.setShader(mShader);
                    mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                }
                // Translate the glare based on progress
                mMatrix.setTranslate(w * mProgress, 0);
                mShader.setLocalMatrix(mMatrix);
                canvas.drawRect(0, 0, w, h, mPaint);
            }
        }
        
        canvas.restoreToCount(saveCount);
    }
}
