package de.marmaro.krt.ffupdater.animation;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;

import static android.view.View.GONE;

/**
 * Created by Tobiwan on 29.04.2020.
 */
public class FadeOutAnimation extends AlphaAnimation {

    public FadeOutAnimation(ProgressBar progressBar) {
        super(1.0f, 0.0f);
        setDuration(300);
        setFillAfter(false);
        setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                progressBar.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

}
