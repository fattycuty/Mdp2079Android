package com.example.emptymdp.utilities;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.emptymdp.fragments.BluetoothFragment;
import com.example.emptymdp.fragments.HomeFragment;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new BluetoothFragment();
        }
        return new HomeFragment();
    }

    @Override
    public int getCount() {
        return 2;
    }
}
