package com.jobradar.common.exception;

public class DuplicateCompanyNameException extends DuplicateResourceException {

    public DuplicateCompanyNameException(String normalizedName) {
        super("A company with normalized name '" + normalizedName + "' already exists");
    }
}
