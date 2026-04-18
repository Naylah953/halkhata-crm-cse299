package com.dbinbox.aiinbox.dto;


//this DTO is used to map the incoming profile data (fname, lname) that we fetch
//from Graph API
public record UserFacebookProfile(String first_name, String last_name) {}
