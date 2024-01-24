package com.grameenphone.wipro.fmfs.cbp.model.data.idp;

import java.util.List;
import java.util.Map;

public class Response {
    public boolean success;
    public int code;
    public String message;
    public User data;

    public Map<String, List> errors;

    public class User {
        public int id;
        public String first_name;
        public String last_name;
        public String username;
        public String display_name;
        public String email;
        public String mobile;
        public String bio;
        public String image;
        public String language;
        public String status;
        public int user_id;
    }
}