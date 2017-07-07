package com.idevicesinc.sweetblue.toolbox.activity;


import android.content.Intent;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;


public class SplashActivity extends AppCompatActivity
{

    private ImageView mSweetLogo;
    private ImageView mBlueLogo;
    private ImageView mSlogan;
    private ViewGroup mOuterLayout;
    private Handler mHandler;
    private boolean advanceCalled = false;
    private Runnable advanceRunnable = new Runnable()
    {

        @Override
        public void run()
        {
            advanceCalled = true;
            advanceToMain();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mHandler = new Handler();

        mOuterLayout = (ViewGroup) findViewById(R.id.outerLayout);
        mOuterLayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!advanceCalled)
                {
                    mHandler.removeCallbacks(advanceRunnable);
                    advanceToMain();
                }
            }
        });
        mSweetLogo = (ImageView) findViewById(R.id.sweetLogo);
        mBlueLogo = (ImageView) findViewById(R.id.blueLogo);
        mSlogan = (ImageView) findViewById(R.id.slogan);

        UuidUtil.makeStrings(getBaseContext());

        startAnimation();

        mHandler.postDelayed(advanceRunnable, 3000);
    }

    private void startAnimation()
    {
        AnimationSet set1 = new AnimationSet(false);
        TranslateAnimation anim = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, -2.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        anim.setDuration(1000);
        anim.setInterpolator(new BounceInterpolator());
        set1.addAnimation(anim);
        mSweetLogo.startAnimation(set1);
        AnimationSet set2 = new AnimationSet(false);
        TranslateAnimation anim2 = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 2.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        anim2.setDuration(1000);
        anim2.setInterpolator(new BounceInterpolator());
        set2.addAnimation(anim2);
        mBlueLogo.startAnimation(set2);
        TranslateAnimation anim3 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -3f, Animation.RELATIVE_TO_SELF, 0f);
        anim3.setDuration(500);
        anim3.setInterpolator(new DecelerateInterpolator());
        anim3.setStartOffset(1200);
        mSlogan.startAnimation(anim3);
    }

    private void advanceToMain()
    {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
