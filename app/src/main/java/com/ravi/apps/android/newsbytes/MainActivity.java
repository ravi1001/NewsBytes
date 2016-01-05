/*
 * Copyright (C) 2015 Ravi
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

package com.ravi.apps.android.newsbytes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.ravi.apps.android.newsbytes.sync.NewsSyncAdapter;

public class MainActivity extends AppCompatActivity
        implements HeadlinesFragment.OnHeadlineSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Tag for logging messages.
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Holds whether the main activity layout contains two panes.
    private boolean mIsTwoPaneMode;

    // Flag indicating whether widget list item was clicked.
    public static boolean isWidgetItemClicked = false;

    // Position of the widget list item that was clicked.
    public static int widgetItemClickedPosition = ListView.INVALID_POSITION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start analytics tracking.
        ((NewsApplication) getApplication()).startTracking();

        // Set default values only the first time.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Register to receive events upon any changes to the shared preferences.
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        // Check if the layout has two panes and set the flag accordingly.
        if(findViewById(R.id.news_details_container) != null) {
            mIsTwoPaneMode = true;
        } else {
            mIsTwoPaneMode = false;

            // Set the app bar elevation.
//            getSupportActionBar().setElevation(0f);
        }

        // Check if it was a configuration change.
        if (savedInstanceState == null) {
            // Check if the widget list item was clicked.
            Intent intent = getIntent();
            if (intent != null
                    && intent.getAction() != null
                    && intent.getAction().equals(getString(R.string.action_item_clicked))) {
                Log.d(LOG_TAG, getString(R.string.log_on_create_widget_item_clicked));

                // Set the widget list item clicked flag.
                isWidgetItemClicked = true;

                // Extract the match id and position from the intent.
                widgetItemClickedPosition = intent.getIntExtra(
                        getString(R.string.extra_widget_item_position), ListView.INVALID_POSITION);

                Log.d(LOG_TAG, getString(R.string.log_on_create_widget_item_position)
                        + ((Integer) widgetItemClickedPosition).toString());
            }
        }

        // Initialize the sync adapter.
        NewsSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    protected void onDestroy() {
        // Unregister from receiving events upon any changes to the shared preferences.
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Check if it's in two-pane mode.
        if(mIsTwoPaneMode) {
            // Get the fragment manager.
            FragmentManager fragmentManager = getFragmentManager();

            // Get the details fragment.
            DetailsFragment detailsFragment = (DetailsFragment) fragmentManager
                    .findFragmentByTag(DetailsFragment.DETAILS_FRAGMENT_TAG);

            // Remove the details fragment.
            if(detailsFragment != null) {
                fragmentManager.beginTransaction()
                        .remove(detailsFragment)
                        .commit();
            }
        }

        // Check the current news category preference.
        if(Utility.getNewsCategoryPreference(this, key)
                .equals(getString(R.string.pref_news_category_favorites))) {
            // Current preference is favorites, inform widget to refresh it's data.
            Utility.sendDataUpdatedBroadcast(this);
        } else {
            // Current preference is not favorites, trigger an immediate sync.
            // Once sync is completed the sync adapter will inform widget to refresh it's data.
            NewsSyncAdapter.syncImmediately(this);
        }
    }

    @Override
    public void onHeadlineSelected(News news) {
        // Check if it's in two pane mode.
        if(mIsTwoPaneMode) {
            // Package the parcelable news data into the arguments bundle.
            Bundle arguments = new Bundle();
            arguments.putParcelable(DetailsFragment.NEWS_DETAILS, news);

            // Create the details fragment object.
            DetailsFragment detailsFragment = new DetailsFragment();

            // Set arguments containing news details.
            detailsFragment.setArguments(arguments);

            // Add the fragment onto the container.
            getFragmentManager().beginTransaction()
                    .replace(R.id.news_details_container, detailsFragment, DetailsFragment.DETAILS_FRAGMENT_TAG)
                    .commit();
        } else {
            // Create intent to launch details activity.
            Intent intent = new Intent(this, DetailsActivity.class);

            // Add news details into extra.
            intent.putExtra(DetailsFragment.NEWS_DETAILS, news);

            // Start details activity.
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get id of the menu item selected.
        int id = item.getItemId();

        // Check if the settings item was selected.
        if (id == R.id.action_settings) {
            // Start the settings activity.
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
