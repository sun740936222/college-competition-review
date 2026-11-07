package com.zhk.contest.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class CollegeLookupResponse {
    private String universityName;
    private List<String> colleges;
    private String source;
    private String error;

    public CollegeLookupResponse() {}

    public static CollegeLookupResponse success(String universityName, List<String> colleges, String source) {
        CollegeLookupResponse r = new CollegeLookupResponse();
        r.universityName = universityName;
        r.colleges = colleges;
        r.source = source;
        return r;
    }

    public static CollegeLookupResponse error(String universityName, String errorMsg) {
        CollegeLookupResponse r = new CollegeLookupResponse();
        r.universityName = universityName;
        r.colleges = List.of();
        r.error = errorMsg;
        return r;
    }

}
