package com.example.emptymdp.utilities;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.emptymdp.fragments.ModeManualFragment;
import com.example.emptymdp.fragments.ModePathFragment;

public class ModePagerAdapter extends FragmentStateAdapter {

    public ModePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ModePathFragment();
            case 1:
                return new ModeManualFragment();
        }
        return new ModePathFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
