package com.application.grocertaxistore.Model;

public class City {
    private String name, searchKeyword;

    public City() {
    }

    public City(String name, String searchKeyword) {
        this.name = name;
        this.searchKeyword = searchKeyword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
}
