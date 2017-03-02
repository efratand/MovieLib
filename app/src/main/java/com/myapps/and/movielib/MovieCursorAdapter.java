package com.myapps.and.movielib;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * Created by Efrat on 9/2/2017.
 */

public class MovieCursorAdapter extends CursorAdapter implements Filterable{


    private LayoutInflater cursorInflater;
    private Context mcontext;
    private Cursor mycursor;

    public MovieCursorAdapter(Context context, Cursor c) {
        super(context, c);
        this.mcontext = context;
        this.mycursor = c;
        cursorInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        ImageView watchedIV=(ImageView) view.findViewById(R.id.watchedIV);
        TextView subject=(TextView) view.findViewById(R.id.movieTV);
        subject.setText(cursor.getString(cursor.getColumnIndex(DBConstants.SUBJECT_C)));
        RatingBar movieRatingBAR=(RatingBar) view.findViewById(R.id.movieRB);
        movieRatingBAR.setRating(cursor.getFloat(cursor.getColumnIndex(DBConstants.RATING_C)));
        int watched=cursor.getInt(cursor.getColumnIndex(DBConstants.WATCHED_C));
        if ( watched == DBConstants.WATCHED )
        {
            watchedIV.setVisibility(View.VISIBLE);
            watchedIV.setImageResource(R.drawable.visible);

        }
        else
        {
            watchedIV.setVisibility(View.INVISIBLE);
        }


    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return cursorInflater.inflate(R.layout.single_movie, parent, false);
    }

    @Override
    public Filter getFilter() {
        return new Filter(){

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults filterReturn = new FilterResults();
                Cursor c = null;
                if(constraint != null){
                    c = getFilterQueryProvider().runQuery(constraint);
                }
                filterReturn.values = c;
                return filterReturn;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mycursor = (Cursor) results.values;
                swapCursor(mycursor);

            }

        };
    }
}
