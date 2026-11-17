package com.jobradar.common.exception;

public class CareerSiteNotFoundException extends ResourceNotFoundException {

    public CareerSiteNotFoundException(Long id) {
        super("Career site not found with id: " + id);
    }

    public CareerSiteNotFoundException(String message) {
        super(message);
    }
}
