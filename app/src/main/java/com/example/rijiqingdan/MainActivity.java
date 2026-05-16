package com.example.rijiqingdan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.rijiqingdan.ui.calendar.CalendarFragment;
import com.example.rijiqingdan.ui.notes.NotesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private boolean suppressNavCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavCallback) return true;
            int id = item.getItemId();
            if (id == R.id.nav_calendar) {
                showFragment(new CalendarFragment());
                return true;
            } else if (id == R.id.nav_notes) {
                showFragment(NotesFragment.newInstance(null));
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(new CalendarFragment());
        }
    }

    private void showFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Called from CalendarFragment to switch to notes for a specific date. */
    public void openNotesForDate(String date) {
        suppressNavCallback = true;
        bottomNav.setSelectedItemId(R.id.nav_notes);
        suppressNavCallback = false;
        showFragment(NotesFragment.newInstance(date));
    }
}
