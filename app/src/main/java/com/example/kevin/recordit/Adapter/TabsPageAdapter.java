package com.example.kevin.recordit.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.kevin.recordit.Fragment.ChatsFragment;
import com.example.kevin.recordit.Fragment.FriendsFragment;
import com.example.kevin.recordit.Fragment.RequestFragment;

/**
 * Created by Kevin on 11/18/2018.
 */

public class TabsPageAdapter extends FragmentPagerAdapter{

    public TabsPageAdapter(FragmentManager fm) {
        super(fm);
    }

    //return how many fragment are there (2)
    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                FriendsFragment friendsFragment = new FriendsFragment();
                return  friendsFragment;
            case 1:
                ChatsFragment chatFragment = new ChatsFragment();
                return chatFragment;
            default:
                return null;
        }
    }

    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                return "friends";
            case 1:
                return "chats";
            default:
                return null;
        }
    }
}
