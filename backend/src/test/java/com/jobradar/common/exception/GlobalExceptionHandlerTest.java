package com.jobradar.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    @DisplayName("CompanyNotFoundException maps to 404 Not Found")
    void testCompanyNotFoundException() {
        CompanyNotFoundException ex = new CompanyNotFoundException(42L);
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("42");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("CareerSiteNotFoundException maps to 404 Not Found")
    void testCareerSiteNotFoundException() {
        CareerSiteNotFoundException ex = new CareerSiteNotFoundException(99L);
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("99");
    }

    @Test
    @DisplayName("DuplicateCompanyNameException maps to 409 Conflict")
    void testDuplicateCompanyNameException() {
        DuplicateCompanyNameException ex = new DuplicateCompanyNameException("acme");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).contains("acme");
    }

    @Test
    @DisplayName("DuplicateCareerUrlException maps to 409 Conflict")
    void testDuplicateCareerUrlException() {
        DuplicateCareerUrlException ex = new DuplicateCareerUrlException("https://acme.com/jobs");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("https://acme.com/jobs");
    }

    @Test
    @DisplayName("InvalidInputException maps to 400 Bad Request")
    void testInvalidInputException() {
        InvalidInputException ex = new InvalidInputException("Invalid company name");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidInputException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid company name");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException maps to 400 Bad Request")
    void testHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error",
                new org.springframework.mock.http.MockHttpInputMessage("test".getBytes())
        );
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON request or invalid field format");
    }

    @Test
    @DisplayName("Unexpected Exception maps to 500 without leaking sensitive details")
    void testUnexpectedException(CapturedOutput output) {
        Exception ex = new RuntimeException("Database password was: secret123");
        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected internal server error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("secret123");

        // Verify that sensitive details do not appear in captured application logs
        assertThat(output.getAll()).doesNotContain("secret123");
        assertThat(output.getAll()).contains("Unhandled unexpected exception on path '/api/test' [type=java.lang.RuntimeException]");
    }
}
