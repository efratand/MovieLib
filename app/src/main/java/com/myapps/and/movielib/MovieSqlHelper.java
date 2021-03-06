package com.myapps.and.movielib;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.w3c.dom.Text;

/**
 * Created by Efrat on 6/2/2017.
 */

public class MovieSqlHelper  extends SQLiteOpenHelper{

    public MovieSqlHelper(Context context) {
        super(context, "MovieLib.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Creating movies table if not exist
        String createMovieTableStr="CREATE TABLE " + DBConstants.MOVIES_T + "( " +
                DBConstants.ID_C + " INTEGER PRIMARY KEY AUTOINCREMENT," +           // autogenerated id for movie
                DBConstants.SUBJECT_C + " TEXT, " +                                  // movie title
                DBConstants.BODY_C + " TEXT, " +                                     // movie plot
                DBConstants.IMAGE_URL_C + " TEXT," +                                 // url of the movie poster
                DBConstants.RATING_C + " INTEGER," +                                 // user rating for movie 1-5
                DBConstants.WATCHED_C + " INTEGER," +                                // 1-seen,2-unseen by user
                DBConstants.MOVIE_IMAGE_C + " TEXT," +                               // base64 string of image
                DBConstants.IS_LOCAL_URL_C + " INTEGER )";                           // TBD

        Log.d("onCreate",createMovieTableStr);
        db.execSQL(createMovieTableStr);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Add code for upgrade
    }


}
