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

    //return how many fragment are there (3)
    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                RequestFragment requestFragment = new RequestFragment();
                return requestFragment;
            case 1:
                ChatsFragment chatFragment = new ChatsFragment();
                return chatFragment;
            case 2:
                FriendsFragment friendsFragment = new FriendsFragment();
                return  friendsFragment;
            default:
                return null;
        }
    }

    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                return "requests";
            case 1:
                return "chats";
            case 2:
                return "friends";
            default:
                return null;
        }
    }
}
