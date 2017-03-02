package com.myapps.and.movielib;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.DateFormat;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Efrat on 7/2/2017.
 */

public class AppConstants {

    static SharedPreferences MoviePrefs;

    static  final String KEY_ID = "id";
    static  final String KEY_SUBJECT="subject";
    static  final String KEY_IMAGE_URL="image_url";
    static  final String KEY_MOVIE_IMDB="imdb";

    static  final int EMPTY_ID = -1;  // Indicates new movie not loaded to db yet
    static  final int WEB_ID = 0;     // Indicates web movie not loaded to db yet
    static  final String EMPTY_FILTER = "";
    static  final int ACTION_NEW_MOVIE_MANUAL = 100;
    static  final int ACTION_NEW_MOVIE_WEB = 101;
    static  final int ACTION_EDIT_MOVIE = 200;
    static  final int ACTION_SHARE = 400;

    static final int MIN_CHAR_CHANGED = 6;
    static final int OMDB_PAGE_BATCH_COUNT = 10; // Max number of entries per page from OMDB

    // Setting variables
    static boolean saveImageLocaly = true;  // save image in DB once its downloaded
    static boolean autoDownloadImage = false; // Automatically download/select image if url exists
    static String  defaultOrderBy = DBConstants.SUBJECT_C;
    static boolean searchSuggestions = false; // Enable web search string suggestions

    static void loadSettings (Context context) {
        // Load setting from sharedPrefences
        MoviePrefs = PreferenceManager.getDefaultSharedPreferences(context);
        saveImageLocaly = MoviePrefs.getBoolean("saveImageLocaly",true);
        Log.d("App settings","saveImageLocaly=" + saveImageLocaly);
        autoDownloadImage = MoviePrefs.getBoolean("autoDownloadImage",false);
        Log.d("App settings","autoDownloadImage=" + autoDownloadImage);
        defaultOrderBy = MoviePrefs.getString("defaultOrderBy",DBConstants.SUBJECT_C);
        Log.d("App settings","defaultOrderBy=" + defaultOrderBy);
        searchSuggestions = MoviePrefs.getBoolean("searchSuggestions",false);
        Log.d("App settings","searchSuggestion=" + searchSuggestions);
    }

    static void updateSaveImageLocaly ( boolean value )
    {
        saveImageLocaly=value;
        SharedPreferences.Editor editor = MoviePrefs.edit();
        editor.putBoolean("saveImageLocaly",saveImageLocaly);
        editor.commit();

    }

    static void updateAutoDownloadImage ( Boolean value )
    {
        autoDownloadImage=value;
        SharedPreferences.Editor editor = MoviePrefs.edit();
        editor.putBoolean("autoDownloadImage",autoDownloadImage);
        editor.commit();
    }

    static void updateDeafultOrderBy ( String value )
    {
        defaultOrderBy=value;
        SharedPreferences.Editor editor = MoviePrefs.edit();
        editor.putString("defaultOrderBy",defaultOrderBy);
        editor.commit();

    }

    static void updateSearchSuggest ( boolean value )
    {
        searchSuggestions=value;
        SharedPreferences.Editor editor = MoviePrefs.edit();
        editor.putBoolean("searchSuggestion",searchSuggestions);
        editor.commit();

    }

}
