/*
 * *
 *  * This file is part of QuickLyric
 *  * Created by geecko
 *  *
 *  * QuickLyric is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * QuickLyric is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License
 *  * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.evandroid.musica.fragment;

import android.animation.ObjectAnimator;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.evandroid.musica.MainLyricActivity;
import com.evandroid.musica.R;
import com.evandroid.musica.adapter.LocalAdapter;
import com.evandroid.musica.lyrics.Lyrics;
import com.evandroid.musica.tasks.DBContentLister;
import com.evandroid.musica.tasks.WriteToDatabaseTask;
import com.evandroid.musica.utils.AnimatorActionListener;
import com.evandroid.musica.view.AnimatedExpandableListView;
import com.evandroid.musica.view.BackgroundContainer;

import java.util.ArrayList;
import java.util.HashMap;

public class LocalLyricsFragment extends ListFragment {


    public static final int REQUEST_CODE = 0;
    public boolean showTransitionAnim = true;
    public boolean isActiveFragment = false;
    public ArrayList<ArrayList<Lyrics>> lyricsArray = null;
    private AnimatedExpandableListView megaListView;
    private ProgressBar progressBar;
    private BackgroundContainer mBackgroundContainer;
    private boolean mSwiping;

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        float mDownX;
        private int mSwipeSlop = -1;
        private boolean mItemPressed;
        private VelocityTracker mVelocityTracker = null;
        private HashMap<Long, Integer> mItemIdTopMap = new HashMap<>();

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            int index = event.getActionIndex();
            int pointerId = event.getPointerId(index);

            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(getActivity())
                        .getScaledTouchSlop();
            }
            int groupPosition = ((LocalAdapter.ChildViewHolder) v.getTag()).groupPosition;
            int childPosition = 90;

            for (int c = 0; c < megaListView.getChildCount(); c++)
                if (megaListView.getChildAt(c) == v) {
                    childPosition = c;
                    break;
                }

            v.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mItemPressed) {
                        // Multi-item swipes not handled
                        return false;
                    }
                    mItemPressed = true;
                    mDownX = event.getX();
                    if (mVelocityTracker == null) {
                        // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        // Reset the velocity tracker back to its initial state.
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1);
                    v.setTranslationX(0);
                    if (((LocalAdapter) megaListView.getExpandableListAdapter())
                            .getGroup(groupPosition).size() <= 1) {
                        View groupView = megaListView.getChildAt(childPosition - 1);
                        if (groupView != null && groupView.getTag() instanceof LocalAdapter.GroupViewHolder) {
                            groupView.setTranslationX(0);
                            groupView.setAlpha(1);
                        }
                    }
                    mItemPressed = false;
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    break;
                case MotionEvent.ACTION_MOVE: {
                    mVelocityTracker.addMovement(event);
                    float x = event.getX() + v.getTranslationX();
                    float deltaX = x - mDownX;
                    float deltaXAbs = Math.abs(deltaX);
                    if (!mSwiping) {
                        if (deltaXAbs > mSwipeSlop) {
                            mSwiping = true;
                            getListView().requestDisallowInterceptTouchEvent(true);
                            int diff = 0;
                            if (((LocalAdapter) megaListView.getExpandableListAdapter())
                                    .getGroup(groupPosition).size() <= 1) {
                                View groupView = megaListView.getChildAt(childPosition - 1);
                                if (groupView != null)
                                    diff = groupView.getHeight();
                            }
                            mBackgroundContainer.showBackground(v.getTop() - diff, v.getHeight() + diff);
                        }
                    }
                    if (mSwiping) {
                        v.setTranslationX((x - mDownX));
                        v.setAlpha(1 - deltaXAbs / v.getWidth());
                        if (((LocalAdapter) megaListView.getExpandableListAdapter())
                                .getGroup(groupPosition).size() <= 1) {
                            View groupView = megaListView.getChildAt(childPosition - 1);
                            if (groupView != null && groupView.getTag() instanceof LocalAdapter.GroupViewHolder) {
                                groupView.setTranslationX((x - mDownX));
                                groupView.setAlpha(1 - deltaXAbs / v.getWidth());
                            }
                        }
                    }
                }
                break;
                case MotionEvent.ACTION_UP: {
                    // User let go - figure out whether to animate the view out, or back into place
                    if (mSwiping) {
                        float x = event.getX() + v.getTranslationX();
                        float deltaX = x - mDownX;
                        float deltaXAbs = Math.abs(deltaX);
                        float fractionCovered;
                        float endX;
                        float endAlpha;
                        final boolean remove;
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float velocityX = Math.abs(VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId));
                        if (velocityX > 700 || deltaXAbs > v.getWidth() / 4) {
                            fractionCovered = deltaXAbs / v.getWidth();
                            endX = deltaX < 0 ? -v.getWidth() : v.getWidth();
                            endAlpha = 0;
                            remove = true;
                        } else {
                            // Not far enough - animate it back
                            fractionCovered = 1 - (deltaXAbs / v.getWidth());
                            endX = 0;
                            endAlpha = 1;
                            remove = false;
                        }
                        mVelocityTracker.clear();
                        int SWIPE_DURATION = 600;
                        long duration = (int) ((1 - fractionCovered) * SWIPE_DURATION);
                        getListView().setEnabled(false);
                        v.animate().setDuration(Math.abs(duration)).
                                alpha(endAlpha).translationX(endX)
                                .setListener(new AnimatorActionListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Restore animated values
                                        v.setAlpha(1);
                                        v.setTranslationX(0);
                                        if (remove) {
                                            animateRemoval(getListView(), v);
                                        } else {
                                            mBackgroundContainer.hideBackground();
                                            getListView().setEnabled(true);
                                        }
                                    }
                                }, AnimatorActionListener.ActionType.END));
                        if (((LocalAdapter) megaListView.getExpandableListAdapter())
                                .getGroup(groupPosition).size() <= 1) {
                            View groupView = megaListView.getChildAt(childPosition - 1);
                            if (groupView != null && groupView.getTag() instanceof LocalAdapter.GroupViewHolder)
                                groupView.animate().setDuration(Math.abs(duration))
                                        .alpha(endAlpha).translationX(endX);
                        }
                    }
                }
                mSwiping = false;
                mItemPressed = false;
                break;
                default:
                    return false;
            }
            return true;
        }

        private void animateRemoval(final ListView listview, View viewToRemove) {
            int firstVisiblePosition = listview.getFirstVisiblePosition();
            for (int i = 0; i < listview.getChildCount(); ++i) {
                View child = listview.getChildAt(i);
                if (child != viewToRemove) {
                    int position = firstVisiblePosition + i;
                    long itemId = listview.getAdapter().getItemId(position);
                    mItemIdTopMap.put(itemId, child.getTop());
                }
            }
            mBackgroundContainer.hideBackground();
            final boolean[] firstAnimation = {true};
            // Delete the item from the adapter
            LocalAdapter.ChildViewHolder childViewHolder = (LocalAdapter.ChildViewHolder) viewToRemove.getTag();
            new WriteToDatabaseTask(LocalLyricsFragment.this)
                    .execute(LocalLyricsFragment.this, null, childViewHolder.lyrics);
            ((LocalAdapter) getExpandableListAdapter())
                    .remove(childViewHolder.groupPosition, viewToRemove);

            final ViewTreeObserver[] observer = {listview.getViewTreeObserver()};
            observer[0].addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    observer[0].removeOnPreDrawListener(this);
                    firstAnimation[0] = true;
                    int firstVisiblePosition = listview.getFirstVisiblePosition();
                    for (int i = 0; i < listview.getChildCount(); ++i) {
                        final View child = listview.getChildAt(i);
                        int position = firstVisiblePosition + i;
                        long itemId = getListView().getAdapter().getItemId(position);
                        Integer formerTop = mItemIdTopMap.get(itemId);
                        int newTop = child.getTop();
                        if (formerTop != null) {
                            if (formerTop != newTop) {
                                int delta = formerTop - newTop;
                                child.setTranslationY(delta);
                                int MOVE_DURATION = 500;
                                child.animate().setDuration(MOVE_DURATION).translationY(0);
                                if (firstAnimation[0]) {
                                    child.animate().setListener(new AnimatorActionListener(new Runnable() {
                                        public void run() {
                                            mSwiping = false;
                                            getListView().setEnabled(true);
                                        }
                                    }, AnimatorActionListener.ActionType.END));
                                    firstAnimation[0] = false;
                                }
                            }
                        } else {
                            // Animate new views along with the others. The catch is that they did not
                            // exist in the start state, so we must calculate their starting position
                            // based on neighboring views.
                            int childHeight = child.getHeight() + listview.getDividerHeight();
                            formerTop = newTop + (i > 0 ? childHeight : -childHeight);
                            int delta = formerTop - newTop;
                            child.setTranslationY(delta);
                            int MOVE_DURATION = 500;
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation[0]) {
                                child.animate().setListener(new AnimatorActionListener(new Runnable() {
                                    public void run() {
                                        mSwiping = false;
                                        getListView().setEnabled(true);
                                    }
                                }, AnimatorActionListener.ActionType.END));
                                firstAnimation[0] = false;
                            }
                        }
                    }
                    if (firstAnimation[0]) {
                        mSwiping = false;
                        getListView().setEnabled(true);
                        firstAnimation[0] = false;
                    }
                    mItemIdTopMap.clear();
                    return true;
                }
            });
        }
    };

    public void animateUndo(Lyrics[] lyricsArray) {
        final HashMap<Long, Integer> itemIdTopMap = new HashMap<>();
        int firstVisiblePosition = megaListView.getFirstVisiblePosition();
        for (int i = 0; i < megaListView.getChildCount(); ++i) {
            View child = megaListView.getChildAt(i);
            int position = firstVisiblePosition + i;
            long itemId = megaListView.getAdapter().getItemId(position);
            itemIdTopMap.put(itemId, child.getTop());
        }
        final boolean[] firstAnimation = {true};
        // Delete the item from the adapter
        final int groupPosition = ((LocalAdapter) getExpandableListAdapter()).add(lyricsArray[0]);
        megaListView.setAdapter(getExpandableListAdapter());
        megaListView.post(new Runnable() {
            @Override
            public void run() {
                megaListView.expandGroupWithAnimation(groupPosition);
            }
        });
        new WriteToDatabaseTask(LocalLyricsFragment.this)
                .execute(LocalLyricsFragment.this, null, lyricsArray);

        final ViewTreeObserver[] observer = {megaListView.getViewTreeObserver()};
        observer[0].addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer[0].removeOnPreDrawListener(this);
                firstAnimation[0] = true;
                int firstVisiblePosition = megaListView.getFirstVisiblePosition();
                for (int i = 0; i < megaListView.getChildCount(); ++i) {
                    final View child = megaListView.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = getListView().getAdapter().getItemId(position);
                    Integer formerTop = itemIdTopMap.get(itemId);
                    int newTop = child.getTop();
                    if (formerTop != null) {
                        if (formerTop != newTop) {
                            int delta = formerTop - newTop;
                            child.setTranslationY(delta);
                            int MOVE_DURATION = 500;
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation[0]) {
                                child.animate().setListener(new AnimatorActionListener(new Runnable() {
                                    public void run() {
                                        mBackgroundContainer.hideBackground();
                                        mSwiping = false;
                                        getListView().setEnabled(true);
                                    }
                                }, AnimatorActionListener.ActionType.END));
                                firstAnimation[0] = false;
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        int childHeight = child.getHeight() + megaListView.getDividerHeight();
                        formerTop = newTop - childHeight;
                        int delta = formerTop - newTop;
                        final float z = ((CardView) child).getCardElevation();
                        ((CardView) child).setCardElevation(0f);
                        child.setTranslationY(delta);
                        final int MOVE_DURATION = 500;
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        child.animate().setListener(new AnimatorActionListener(new Runnable() {
                            public void run() {
                                mBackgroundContainer.hideBackground();
                                mSwiping = false;
                                getListView().setEnabled(true);
                                ObjectAnimator anim = ObjectAnimator.ofFloat(child, "cardElevation", 0f, z);
                                anim.setDuration(200);
                                anim.setInterpolator(new AccelerateInterpolator());
                                anim.start();
                            }
                        }, AnimatorActionListener.ActionType.END));
                        firstAnimation[0] = false;
                    }
                }
                if (firstAnimation[0]) {
                    mBackgroundContainer.hideBackground();
                    mSwiping = false;
                    getListView().setEnabled(true);
                    firstAnimation[0] = false;
                }
                itemIdTopMap.clear();
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        View layout = inflater.inflate(R.layout.local_listview, container, false);
        megaListView = (AnimatedExpandableListView) layout.findViewById(android.R.id.list);
        mBackgroundContainer = (BackgroundContainer) layout.findViewById(R.id.listViewBackground);
        progressBar = (ProgressBar) layout.findViewById(R.id.list_progress);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle onSavedInstanceState) {
        super.onActivityCreated(onSavedInstanceState);
        if (megaListView != null) {
            View fragmentView = getView();
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
            if (fragmentView != null)
                fragmentView.setBackgroundColor(typedValue.data);
            megaListView.setDividerHeight(0);
            megaListView.setFastScrollEnabled(true);
            megaListView.setDrawSelectorOnTop(true);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.isHidden())
            return;

        megaListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                final ImageView indicator = (ImageView) v.findViewById(R.id.group_indicator);
                RotateAnimation anim;
                if (megaListView.isGroupExpanded(groupPosition)) {
                    megaListView.collapseGroupWithAnimation(groupPosition);
                    if (indicator != null) {
                        anim = new RotateAnimation(180f, 360f, indicator.getWidth() / 2, indicator.getHeight() / 2);
                        anim.setInterpolator(new DecelerateInterpolator(3));
                        anim.setDuration(500);
                        anim.setFillAfter(true);
                        indicator.startAnimation(anim);
                    }
                } else {
                    megaListView.expandGroupWithAnimation(groupPosition);
                    if (indicator != null) {
                        anim = new RotateAnimation(0f, 180f, indicator.getWidth() / 2, indicator.getHeight() / 2);
                        anim.setInterpolator(new DecelerateInterpolator(2));
                        anim.setDuration(500);
                        anim.setFillAfter(true);
                        indicator.startAnimation(anim);
                    }
                }
                return true;
            }
        });

        megaListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                if (mSwiping) {
                    mSwiping = false;
                    return false;
                }
                final MainLyricActivity mainLyricActivity = (MainLyricActivity) getActivity();
                megaListView.setOnChildClickListener(null); // prevents bug on double tap
                mainLyricActivity.updateLyricsFragment(R.animator.slide_out_start, R.animator.slide_in_start,
                        true, lyricsArray.get(groupPosition).get(childPosition));
                return true;
            }
        });

        this.isActiveFragment = true;
        new DBContentLister(this).execute();
    }

    public void update(final ArrayList<ArrayList<Lyrics>> results) {
        if (getView() == null)
            return;
        int index = megaListView.getFirstVisiblePosition();
        View v = megaListView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - megaListView.getPaddingTop());
        lyricsArray = results;

        megaListView.setAdapter(new LocalAdapter(getActivity(), results, mTouchListener, megaListView));
        megaListView.setEmptyView(((ViewGroup) getView().findViewById(R.id.local_empty_database_textview).getParent()));
        getListView().setSelectionFromTop(index, top);
        setListShown(true);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            this.onViewCreated(getView(), null);
        else
            this.isActiveFragment = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainLyricActivity mainLyricActivity = (MainLyricActivity) this.getActivity();
        ActionBar actionBar = (mainLyricActivity).getSupportActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
        }
        return false;
    }

    public void setListShown(final boolean visible) {
        final Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        final Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                progressBar.setVisibility(visible ? View.GONE : View.VISIBLE);
                megaListView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        fadeIn.setAnimationListener(listener);
        fadeOut.setAnimationListener(listener);
        progressBar.startAnimation(visible ? fadeOut : fadeIn);
        megaListView.startAnimation(visible ? fadeIn : fadeOut);
    }

    public ExpandableListAdapter getExpandableListAdapter() {
        return megaListView.getExpandableListAdapter();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}