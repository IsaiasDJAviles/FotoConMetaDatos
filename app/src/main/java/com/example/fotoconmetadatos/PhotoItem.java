package com.example.fotoconmetadatos;

import android.net.Uri;

public class PhotoItem {
    private final long id;
    private final Uri uri;
    private final String name;
    private final String dateTime;
    private final String location;

    public PhotoItem(long id, String uriString, String name, String dateTime, String location) {
        this.id = id;
        this.uri = Uri.parse(uriString);
        this.name = name;
        this.dateTime = dateTime;
        this.location = location;
    }

    public long getId() {
        return id;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getLocation() {
        return location;
    }
}