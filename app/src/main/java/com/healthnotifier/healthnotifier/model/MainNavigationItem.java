package com.healthnotifier.healthnotifier.model;

/**
 * Created by charles on 2/2/17.
 */

public class MainNavigationItem extends Object {
    // this is some arbitrary struct data structure hack zone shee
    // TODO: badge state SON, SON, SON
    // not necessary to manage provider check internally, I don't think, just queue that crap up in the MainActivity son
    public String id; // short name in order to "switch" on the click handler son
    public String label; // getString from R.string
    public int icon; // from R.drawable
}
