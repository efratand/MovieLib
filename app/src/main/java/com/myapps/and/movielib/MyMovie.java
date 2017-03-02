package com.myapps.and.movielib;

/**
 * Created by Efrat on 6/2/2017.
 */

public class MyMovie {

    int id;              // Unique movie id in DB
    String subject;      // Movie's title
    String body;         // Movie's description
    String imageUrl;     // Movie's image url
    boolean isLocalUrl;  // Indication if image is url is located localy on device
    int Rating;          // Rating for movie 1-5. or 0 for not rated
    int Watched;         // Watched movie indicator (0 - not watched/or 1 - watched)
    String Imdb;         // Imdb identifier
    String imageString64;// Image as string


    public MyMovie(int id, String subject, String body, String imageUrl,  int rating, int watched) {
        this.id = id;

        this.subject = subject;
        this.body = body;
        this.imageUrl = imageUrl;
        this.isLocalUrl = false;
        this.Rating = rating;
        this.Watched = watched;
        this.imageString64 = "";
    }

    public MyMovie(int id, String subject, String body, String imageUrl) {
        this.id = id;

        this.subject = subject;
        this.body = body;
        this.imageUrl = imageUrl;
        this.isLocalUrl = false;                // TBD
        this.Rating = 0;                        // default value for a new inserted movie
        this.Watched = DBConstants.NOT_WATCHED; // Default value for a new inserted movie
        this.imageString64 = "";                // Default value for a new inserted movie
    }

    public String getImageString64() {
        if ( imageString64 == null ) {
            imageString64="";
        }
        return imageString64;
    }

    public void setImageString64(String imageString64) {
        if ( imageString64 == null )
        {
            imageString64 = "";
        }
        this.imageString64 = imageString64;
    }

    public String getImdb() {
        return Imdb;
    }

    public void setImdb(String imdb) {
        Imdb = imdb;
    }

    @Override
    public String toString() {
        return subject;
    }


    public int getRating() {
        return Rating;
    }

    public void setRating(int rating) {
        Rating = rating;
    }

    public int getWatched() {
        return Watched;
    }

    public void setWatched(int watched) {
        Watched = watched;
    }

    public boolean isLocalUrl() {
        return isLocalUrl;
    }

    public int getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLocalUrl(boolean localUrl) {
        this.isLocalUrl=localUrl;
    }
    
    public void setLocalUrl(int localUrl) {
        if ( localUrl == DBConstants.LOCAL_URL ) {
            this.isLocalUrl=true;
        } else {
            this.isLocalUrl = false;
        }
    }
}
