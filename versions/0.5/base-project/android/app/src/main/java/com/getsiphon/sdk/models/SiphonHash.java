package com.getsiphon.sdk.models;


public class SiphonHash {
    public final String name;
    public final String sha256;

    public SiphonHash(String name, String sha256) {
        this.name = name;
        this.sha256 = sha256;
    }
}
