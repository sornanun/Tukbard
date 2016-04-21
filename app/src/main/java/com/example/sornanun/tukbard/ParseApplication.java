package com.example.sornanun.tukbard;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by sornanun on 22/1/2559.
 */
public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "xwh8EUo1w8ZmezPsxYfOwlU7z5yghX9aJj0yGyw2", "HMIS2zYNT6F12TCqe0FmMJKgTgnntVGDwXr5cOHs");
    }
}