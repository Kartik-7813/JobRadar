package com.jobradar.common.exception;

public class DuplicateCareerUrlException extends DuplicateResourceException {

    public DuplicateCareerUrlException(String careerUrl) {
        super("A career site with URL '" + careerUrl + "' already exists");
    }

    public DuplicateCareerUrlException(String careerUrl, Throwable cause) {
        super("A career site with URL '" + careerUrl + "' already exists", cause);
    }
}
