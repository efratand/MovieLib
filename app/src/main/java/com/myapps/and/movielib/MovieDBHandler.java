package com.myapps.and.movielib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Created by Efrat on 7/2/2017.
 */

public class MovieDBHandler {



    // Default order by is by subject
    static String fetchAllOrderBy=AppConstants.defaultOrderBy;

    // Set order by clause for query all from movie table
    // Used by fetchALLMovies and fetchByTitle
    public static void setOrderBy (String columnList){
        fetchAllOrderBy=columnList;
    }

    // Returns a cursor with select all from movie db
    public static Cursor fetchAllMovies (Context context) {

        MovieSqlHelper moviesDb = new MovieSqlHelper(context);
        Cursor C = moviesDb.getReadableDatabase().query(DBConstants.MOVIES_T,null,null,null,null,null,fetchAllOrderBy);
        return C;
    }


    // Returns a single movie selected by its id
    public static MyMovie fetchMovieFromDB(Context context,int id) {

        // TODO deal with empty query
        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        Log.d("-fetchMovie","id = "+id);
        Cursor MC = movieDB.getReadableDatabase().query(DBConstants.MOVIES_T,null,DBConstants.ID_C+"=?",new String[]{""+id},null,null,null);
        MC.moveToFirst();
        MyMovie M = new MyMovie(MC.getInt(MC.getColumnIndex(DBConstants.ID_C)),
                MC.getString(MC.getColumnIndex(DBConstants.SUBJECT_C)),
                MC.getString(MC.getColumnIndex(DBConstants.BODY_C)),
                MC.getString(MC.getColumnIndex(DBConstants.IMAGE_URL_C)));
        M.setImageString64(MC.getString(MC.getColumnIndex(DBConstants.MOVIE_IMAGE_C)));
        MC.close();
        movieDB.close();

        return M;
    }

    // Get list of movies with a title matches a string - case INsensitive
    // Mainly for filtering
    public static Cursor fetchByTitle (Context context,String str){

        String whereStr = "UPPER(" + DBConstants.SUBJECT_C + ") LIKE UPPER(?)";
        MovieSqlHelper moviesDb = new MovieSqlHelper(context);
        Cursor C = moviesDb.getReadableDatabase().query(DBConstants.MOVIES_T,null,whereStr,new String[]{"%"+str+"%"},null,null,fetchAllOrderBy);
        return C;
    };

    // Returns indication(true/false) for title existing in db - case sensitive
    public static boolean doesTitleExists ( Context context, String title ){

        boolean  exists = false;
        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        Log.d("-fetchTitle", title);
        Cursor MC = movieDB.getReadableDatabase().query(DBConstants.MOVIES_T,null,DBConstants.SUBJECT_C+"=?",new String[]{title},null,null,null);
        if ( MC.getCount() > 0 ) {
            exists=true;
        }
        else
        {
            exists=false;
        }

        MC.close();
        movieDB.close();
        return exists;
    }
    // Set watched status for a movie
    public static void setWatched(Context context, int id, boolean watched){

        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        ContentValues values=new ContentValues();
        if (watched) {
            values.put(DBConstants.WATCHED_C, DBConstants.WATCHED);
        }
        else
        {
            values.put(DBConstants.WATCHED_C, DBConstants.NOT_WATCHED);
        }
        movieDB.getWritableDatabase().update(DBConstants.MOVIES_T,values,DBConstants.ID_C+"=?",new String[]{""+id});
        movieDB.close();

    }

    // Set watched rating for a movie
    public static void setRating(Context context, int id, int rating){

        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        ContentValues values=new ContentValues();
        values.put(DBConstants.RATING_C, rating);
        movieDB.getWritableDatabase().update(DBConstants.MOVIES_T,values,DBConstants.ID_C+"=?",new String[]{""+id});
        movieDB.close();

    }

    // Update a single movie by id.
    public static void updateMovie(Context context, MyMovie M) {

        String sqlStr = "";
        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        ContentValues values = new ContentValues();

        values.put(DBConstants.SUBJECT_C, M.getSubject());
        values.put(DBConstants.BODY_C, M.getBody());
        values.put(DBConstants.IMAGE_URL_C, M.getImageUrl());

        if (AppConstants.saveImageLocaly)
        {
            values.put(DBConstants.MOVIE_IMAGE_C, M.getImageString64());
        }

        if ( M.getId() == AppConstants.EMPTY_ID || M.getId() == AppConstants.WEB_ID )
        {
            // New movie - do call insert instead of update
            Log.d("","new movie: " + M.getSubject());
            movieDB.getWritableDatabase().insert(DBConstants.MOVIES_T,null,values);

        } else
        {
            // Existing movie - call an update
            Log.d ("-updateMovie","id : " + M.getId());
            movieDB.getWritableDatabase().update(DBConstants.MOVIES_T,values,DBConstants.ID_C+"=?",new String[]{""+M.getId()});

        }

        // TODO verify successful completion
        movieDB.close();

    }

    // Deletes all rows in db. returns number of rows deleted
    // TODO consider Async operation
    public static int deleteAllMovies(Context context){
        int rowCount=0;
        String deleteStr="DELETE FROM " + DBConstants.MOVIES_T ;
        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        Log.d ("-deleteAllMovies",deleteStr);
        rowCount = movieDB.getWritableDatabase().delete(DBConstants.MOVIES_T,null,null);
        movieDB.close();
        return rowCount;
    }

    // Delete a single movie by id
    public static void deleteMovieById(Context context,int id) {
        String deleteStr="DELETE FROM " + DBConstants.MOVIES_T +
                " WHERE " + DBConstants.ID_C + "=" + id;
        MovieSqlHelper movieDB = new MovieSqlHelper(context);
        Log.d ("-deleteMovie",deleteStr);
        movieDB.getWritableDatabase().execSQL(deleteStr);

        // TODO verify successful completion
        movieDB.close();
    }

}
