package com.example.fotoconmetadatos;

public class PhotoItem {
    private String path;
    private String name;
    private String dateTime;
    private String location;
    private long id;

    public PhotoItem(long id, String path, String name, String dateTime, String location) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.dateTime = dateTime;
        this.location = location;
    }

    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
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