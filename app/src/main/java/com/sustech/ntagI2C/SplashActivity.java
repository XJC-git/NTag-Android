/*
****************************************************************************
* Copyright(c) 2014 NXP Semiconductors                                     *
* All rights are reserved.                                                 *
*                                                                          *
* Software that is described herein is for illustrative purposes only.     *
* This software is supplied "AS IS" without any warranties of any kind,    *
* and NXP Semiconductors disclaims any and all warranties, express or      *
* implied, including all implied warranties of merchantability,            *
* fitness for a particular purpose and non-infringement of intellectual    *
* property rights.  NXP Semiconductors assumes no responsibility           *
* or liability for the use of the software, conveys no license or          *
* rights under any patent, copyright, mask work right, or any other        *
* intellectual property rights in or to any products. NXP Semiconductors   *
* reserves the right to make changes in the software without notification. *
* NXP Semiconductors also makes no representation or warranty that such    *
* application will be suitable for the specified use without further       *
* testing or modification.                                                 *
*                                                                          *
* Permission to use, copy, modify, and distribute this software and its    *
* documentation is hereby granted, under NXP Semiconductors' relevant      *
* copyrights in the software, without fee, provided that it is used in     *
* conjunction with NXP Semiconductor products(UCODE I2C, NTAG I2C).        *
* This  copyright, permission, and disclaimer notice must appear in all    *
* copies of this code.                                                     *
****************************************************************************
*/
package com.sustech.ntagI2C;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
	public static final String TAG = "ActivitySplash";
	private CountDownTimer contador;
	private View lunchingFragment;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_splash);
		lunchingFragment = findViewById(R.id.lunchingFragment);
		startAnimation();
	}


	private void startAnimation(){
		View round = findViewById(R.id.round);
		ObjectAnimator animator1 = ObjectAnimator.ofFloat(lunchingFragment,"alpha",0f,1f);
		ObjectAnimator animator2 = ObjectAnimator.ofFloat(round,"ScaleX",1,10);
		ObjectAnimator animator3 = ObjectAnimator.ofFloat(round,"ScaleY",1,10);
		AnimatorSet animSet =new AnimatorSet();
		animSet.play(animator2).with(animator3).after(animator1);
		animSet.setDuration(750);
		animSet.start();
		animSet.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animator) {
			}
			@Override
			public void onAnimationEnd(Animator animator) {
				Intent intent = new Intent(getBaseContext(), MainActivity.class);
				startActivity(intent);
				finish();
			}
			@Override
			public void onAnimationCancel(Animator animator) {
			}
			@Override
			public void onAnimationRepeat(Animator animator) {
			}
		});
	}
}
