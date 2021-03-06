/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) 2016 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced
 ** Distributed Software Engineering |  or transmitted in any form or by any
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the
 ** 4131 NJ Vianen                   |  purpose, without the express written
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.ng.track;

import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import nl.sogeti.android.gpstracker.ng.about.AboutFragment;
import nl.sogeti.android.gpstracker.ng.graphs.GraphsActivity;
import nl.sogeti.android.gpstracker.ng.graphs.GraphsFragment;
import nl.sogeti.android.gpstracker.ng.trackdelete.TrackDeleteDialogFragment;
import nl.sogeti.android.gpstracker.ng.trackedit.TrackEditDialogFragment;
import nl.sogeti.android.gpstracker.ng.tracklist.TrackListActivity;
import nl.sogeti.android.gpstracker.ng.tracklist.TrackListFragment;
import nl.sogeti.android.gpstracker.v2.R;
import nl.sogeti.android.gpstracker.v2.databinding.ActivityTrackMapBinding;
import timber.log.Timber;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

public class TrackActivity extends AppCompatActivity implements TrackViewModel.View, TrackListFragment.Listener {

    private static final String KEY_SELECTED_TRACK_URI = "KEY_SELECTED_TRACK_URI";
    private static final String KEY_SELECTED_TRACK_NAME = "KEY_SELECTED_TRACK_NAME";
    private static final String TAG_DIALOG = "DIALOG";
    private static final String TRANSACTION_TRACKS = "FRAGMENT_TRANSACTION_TRACKS";
    private static final String TRANSACTION_GRAPHS = "FRAGMENT_TRANSACTION_GRAPGS";
    private TrackViewModel viewModel = new TrackViewModel();
    private TrackPresenter presenter = new TrackPresenter(viewModel, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTrackMapBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_track_map);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.bringToFront();
        binding.setViewModel(viewModel);
        if (savedInstanceState != null) {
            Uri uri = savedInstanceState.getParcelable(KEY_SELECTED_TRACK_URI);
            String name = savedInstanceState.getString(KEY_SELECTED_TRACK_NAME);
            viewModel.getTrackUri().set(uri);
            viewModel.getName().set(name);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.start(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.stop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_TRACK_URI, viewModel.getTrackUri().get());
        outState.putString(KEY_SELECTED_TRACK_NAME, viewModel.getName().get());
    }

    //region Context menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_map, menu);
        DrawableCompat.setTint(menu.findItem(R.id.action_edit).getIcon(), ContextCompat.getColor(this, R.color.primary_light));
        DrawableCompat.setTint(menu.findItem(R.id.action_list).getIcon(), ContextCompat.getColor(this, R.color.primary_light));
        DrawableCompat.setTint(menu.findItem(R.id.action_graphs).getIcon(), ContextCompat.getColor(this, R.color.primary_light));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_edit).setEnabled(viewModel.isEditable());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean consumed;
        if (item.getItemId() == R.id.action_edit) {
            presenter.onEditOptionSelected();
            consumed = true;
        } else if (item.getItemId() == R.id.action_about) {
            presenter.onAboutOptionSelected();
            consumed = true;
        } else if (item.getItemId() == R.id.action_list) {
            presenter.onListOptionSelected();
            consumed = true;
        } else if (item.getItemId() == R.id.action_graphs) {
            presenter.onGraphsOptionSelected();
            consumed = true;
        } else {
            consumed = super.onOptionsItemSelected(item);
        }

        return consumed;
    }

    //endregion

    //region View contract

    @Override
    public void showTrackName(@NotNull String name) {
        invalidateOptionsMenu();
    }

    @Override
    public void showAboutDialog() {
        new AboutFragment().show(getSupportFragmentManager(), AboutFragment.Companion.getTAG());
    }

    @Override
    public void showTrackEditDialog(Uri trackUri) {
        TrackEditDialogFragment.Companion.newInstance(trackUri).show(getSupportFragmentManager(), TAG_DIALOG);
    }

    @Override
    public void showTrackSelection() {
        View leftContainer = findViewById(R.id.track_leftcontainer);
        if (leftContainer != null && leftContainer instanceof ViewGroup) {
            toggleContainerFragment(TrackListFragment.Companion.newInstance(), TRANSACTION_TRACKS);
        } else {
            TrackListActivity.Companion.start(this);
        }
    }

    @Override
    public void showGraphs(Uri trackUri) {
        View leftContainer = findViewById(R.id.track_leftcontainer);
        if (leftContainer != null && leftContainer instanceof ViewGroup) {
            toggleContainerFragment(GraphsFragment.Companion.newInstance(trackUri), TRANSACTION_GRAPHS);
        } else {
            GraphsActivity.Companion.start(this, trackUri);
        }
    }

    //endregion

    //region TrackList hosting

    @Override
    public void hideTrackList(@NotNull TrackListFragment trackListFragment) {
        getSupportFragmentManager().popBackStack(TRANSACTION_TRACKS, POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void showTrackDeleteDialog(@NotNull Uri track) {
        TrackDeleteDialogFragment.Companion.newInstance(track).show(getSupportFragmentManager(), TAG_DIALOG);
    }

    //endregion


    private void toggleContainerFragment(Fragment goal, String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.track_leftcontainer);
        if (fragment != null) {
            getSupportFragmentManager().popBackStack(null, POP_BACK_STACK_INCLUSIVE);
        }
        if (fragment == null || fragment.getClass() != goal.getClass()) {
            replaceFragmentInLeftContainer(goal, tag);
        }
    }

    private void replaceFragmentInLeftContainer(Fragment goal, String tag) {
        try {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_left,
                            R.anim.enter_from_left, R.anim.exit_to_left)
                    .addToBackStack(tag)
                    .replace(R.id.track_leftcontainer, goal)
                    .commit();
        } catch (Exception e) {
            Timber.e(e, "Transaction to add Fragment failed");
        }
    }

}
