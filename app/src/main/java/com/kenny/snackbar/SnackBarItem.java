package com.kenny.snackbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.ColorRes;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * Copyright (C) 2014 Kenny Campagna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class SnackBarItem {
    private View.OnClickListener mActionClickListener;

    private View mSnackBarView;

    private AnimatorSet mAnimator;

    private String mMessageString;

    private String mActionMessage;

    // The default color the action item will be
    private int mDefaultActionColor = Color.YELLOW;

    // The pressed color of the action item
    private int mPressedActionColor = Color.WHITE;

    // Flag for if when the animation is canceled, should the item be disposed of. Will be set to false when
    // the action button is selected so it removes immediately.
    private boolean mShouldDisposeOnCancel = true;

    private float mAnimationEnd;

    private Activity mActivity;

    // Callback for the SnackBarManager
    private SnackBarDisposeListner mListener;

    private SnackBarListener mSnackBarListener;

    private long mAnimationDuration = DateUtils.SECOND_IN_MILLIS * 2;

    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private Object mObject;

    /**
     * Create a SnackBarItem
     *
     * @param message The message for the SnackBarItem
     */
    SnackBarItem(String message) {
        mMessageString = message;
    }

    /**
     * Create a SnackbarItem
     *
     * @param message         The message for the SnackBarItem
     * @param actionMessage   The action message for the SnackBarItem
     * @param onClickListener THe onClickListener for the action
     */
    SnackBarItem(String message, String actionMessage, View.OnClickListener onClickListener) {
        mMessageString = message;
        mActionMessage = actionMessage.toUpperCase();
        mActionClickListener = onClickListener;
    }

    private SnackBarItem() {
        // Empty constructor
    }

    /**
     * Shows the Snack Bar
     *
     * @param activity
     * @param listener
     */
    public void show(Activity activity, SnackBarDisposeListner listener) {
        if (TextUtils.isEmpty(mMessageString)) {
            throw new IllegalArgumentException("No message has been set for the Snack Bar");
        }

        mActivity = activity;
        mListener = listener;
        FrameLayout parent = (FrameLayout) activity.findViewById(android.R.id.content);
        mSnackBarView = activity.getLayoutInflater().inflate(R.layout.snack_bar, parent, false);
        TextView messageTV = (TextView) mSnackBarView.findViewById(R.id.message);
        messageTV.setText(mMessageString);
        messageTV.setTypeface(Typeface.createFromAsset(mActivity.getAssets(), "RobotoCondensed-Regular.ttf"));

        if (!TextUtils.isEmpty(mActionMessage)) {
            // Only set up the action button when an action message ahs been supplied
            setupActionButton((ImageView) mSnackBarView.findViewById(R.id.action));
        }

        float snackBarHeight = activity.getResources().getDimension(R.dimen.snack_bar_height);
        mAnimationEnd = activity.getResources().getDimension(R.dimen.snack_bar_animation_height);
        mAnimator = new AnimatorSet();
        mAnimator.setInterpolator(mInterpolator);
        parent.addView(mSnackBarView);

        mAnimator.playSequentially(
                ObjectAnimator.ofFloat(mSnackBarView, "translationY", snackBarHeight, -mAnimationEnd).setDuration(500L),
                ObjectAnimator.ofFloat(mSnackBarView, "alpha", 1.0f, 1.0f).setDuration(mAnimationDuration),
                ObjectAnimator.ofFloat(mSnackBarView, "translationY", -mAnimationEnd, snackBarHeight).setDuration(500L)
        );

        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);


                if (mShouldDisposeOnCancel) {
                    dispose();
                    if (mSnackBarListener != null) {
                        mSnackBarListener.onSnackBarFinished(mObject);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if (mShouldDisposeOnCancel) dispose();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                if (mSnackBarListener != null) {
                    mSnackBarListener.onSnackBarStarted(mObject);
                }
            }
        });

        mAnimator.start();
    }

    /**
     * Sets up the action button if available
     *
     * @param action
     */
    private void setupActionButton(ImageView action) {
        action.setVisibility(View.VISIBLE);
        action.setImageDrawable(createActionButton(action.getResources()));

        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShouldDisposeOnCancel = false;
                mAnimator.cancel();
                ObjectAnimator anim = ObjectAnimator.ofFloat(mSnackBarView, "translationY", -mAnimationEnd, mSnackBarView.getHeight()).setDuration(500L);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        dispose();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dispose();

                        if (mSnackBarListener != null) {
                            mSnackBarListener.onSnackBarFinished(mObject);
                        }
                    }
                });
                anim.start();

                if (mActionClickListener != null) {
                    mActionClickListener.onClick(view);
                }

                if (mSnackBarListener != null) {
                    mSnackBarListener.onSnackBarAction(mObject);
                }
            }
        });
    }

    private Drawable createActionButton(Resources resources) {
        TextDrawable regular = new TextDrawable(mActionMessage, resources.getDimensionPixelSize(R.dimen.snack_bar_action_text_size),
                mDefaultActionColor, Typeface.createFromAsset(resources.getAssets(), "Roboto-Medium.ttf"));

        TextDrawable pressed = new TextDrawable(mActionMessage, resources.getDimensionPixelSize(R.dimen.snack_bar_action_text_size),
                mPressedActionColor, Typeface.createFromAsset(resources.getAssets(), "Roboto-Medium.ttf"));

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressed);
        stateListDrawable.addState(new int[]{}, regular);
        return stateListDrawable;
    }

    /**
     * Cancels the Snack Bar from being displayed
     */
    public void cancel() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }

        dispose();
    }

    /**
     * Cleans up the Snack Bar when finished
     */
    private void dispose() {
        if (mSnackBarView != null) {
            FrameLayout parent = (FrameLayout) mSnackBarView.getParent();

            if (parent != null) {
                parent.removeView(mSnackBarView);
            }
        }

        if (mAnimator != null) {
            mAnimator.removeAllListeners();
            mAnimator = null;
        }

        mSnackBarView = null;
        mActionClickListener = null;

        if (mListener != null) {
            mListener.onDispose(mActivity, this);
        }
    }

    /**
     * Factory for building custom SnackBarItems
     */
    public static class Builder {
        private SnackBarItem mSnackBarItem;

        /**
         * Default Constructor
         */
        public Builder() {
            mSnackBarItem = new SnackBarItem();
        }

        /**
         * Sets the message for the SnackBarItem
         *
         * @param message
         * @return
         */
        public Builder setMessage(String message) {
            mSnackBarItem.mMessageString = message;
            return this;
        }

        /**
         * Sets the Action Message of the SnackbarItem
         *
         * @param actionMessage
         * @return
         */
        public Builder setActionMessage(String actionMessage) {
            // guard against any null values being passed
            if (TextUtils.isEmpty(actionMessage)) return this;

            mSnackBarItem.mActionMessage = actionMessage.toUpperCase();
            return this;
        }

        /**
         * Sets the onClick listener for the action message
         *
         * @param onClickListener
         * @return
         */
        public Builder setActionClickListener(View.OnClickListener onClickListener) {
            mSnackBarItem.mActionClickListener = onClickListener;
            return this;
        }

        /**
         * Sets the default color of the action message
         *
         * @param color
         * @return
         */
        public Builder setActionMessageColor(@ColorRes int color) {
            mSnackBarItem.mDefaultActionColor = color;
            return this;
        }

        /**
         * Sets the color of the action message when pressed
         *
         * @param color
         * @return
         */
        public Builder setActionMessagePressedColor(@ColorRes int color) {
            mSnackBarItem.mPressedActionColor = color;
            return this;
        }

        /**
         * Sets the duration of the SnackBar
         *
         * @param duration
         * @return
         */
        public Builder setDuration(long duration) {
            mSnackBarItem.mAnimationDuration = duration;
            return this;
        }

        /**
         * Set the Interpolator of the SnackBar animation
         *
         * @param interpolator
         * @return
         */
        public Builder setInterpolator(Interpolator interpolator) {
            mSnackBarItem.mInterpolator = interpolator;
            return this;
        }

        /**
         * Set the SnackBars object that will be return in the SnackBarListener call backs
         *
         * @param object
         * @return
         */
        public Builder setObject(Object object) {
            mSnackBarItem.mObject = object;
            return this;
        }

        /**
         * Sets the SnackBarListener
         *
         * @param listener
         * @return
         */
        public Builder setSnackBarListener(SnackBarListener listener) {
            mSnackBarItem.mSnackBarListener = listener;
            return this;
        }

        /**
         * Creates the SnackBarItem
         *
         * @return
         */
        public SnackBarItem build() {
            return mSnackBarItem;
        }
    }

    public static interface SnackBarDisposeListner {
        void onDispose(Activity activity, SnackBarItem snackBar);
    }
}