package com.badminton.service.test;

import com.badminton.requestmodel.GameDTO;
import com.google.gson.Gson;

public class GsonTest {
    public static void main(String[] args) {
        Gson gson = new Gson();
        System.out.println(gson.toJson(new GameDTO()));
    }
}
