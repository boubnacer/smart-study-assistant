package com.example.smartstudyassistant.models;

public class Document {
    private int id;
    private String name;
    private String path;

    @Override
    public String toString() {
        return name; // Return document name
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}