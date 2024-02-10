package com.example.emptymdp.utilities;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.emptymdp.fragments.ArenaUpdatesFragment;
import com.example.emptymdp.fragments.NormalTextFragment;

public class MessagePagerAdapter extends FragmentStateAdapter {

    public MessagePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new NormalTextFragment();
            case 1:
                return new ArenaUpdatesFragment();
        }
        return new NormalTextFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
