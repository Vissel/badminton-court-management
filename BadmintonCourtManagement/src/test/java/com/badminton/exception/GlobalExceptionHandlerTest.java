package com.badminton.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whatever goes wrong in the database, the shop owner must read a sentence they
 * can act on - never a SQL statement.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    private static final String DEADLOCK = "could not execute statement "
            + "[Deadlock found when trying to get lock; try restarting transaction] "
            + "[update inventory_item set is_active=?,item_name=?,retail_price=? where item_id=?]";

    @Test
    void deadlockIsReportedAsBusyInsteadOfLeakingTheStatement() {
        var response = handler.handleLockContention(
                new DeadlockLoserDataAccessException(DEADLOCK, null), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        String message = message(response.getBody());
        assertEquals(GlobalExceptionHandler.MSG_DB_BUSY, message);
        assertFalse(message.contains("update inventory_item"));
    }

    @Test
    void databaseFailureKeepsTheStatementInTheLogOnly() {
        var response = handler.handleDataAccess(
                new DataIntegrityViolationException(DEADLOCK), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(GlobalExceptionHandler.MSG_SERVER_ERROR, message(response.getBody()));
    }

    @Test
    void technicalTextOnAnUnexpectedErrorBecomesTheGenericMessage() {
        var response = handler.handleResourceNotFoundException(
                new IllegalStateException("nested exception is org.hibernate.exception.SQLGrammarException"), request);

        assertEquals(GlobalExceptionHandler.MSG_SERVER_ERROR, message(response.getBody()));
    }

    @Test
    void businessMessageIsStillShownToTheUser() {
        var response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Giá bán không được để trống"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(message(response.getBody()).contains("Giá bán"));
    }

    private String message(Object body) {
        return String.valueOf(((Map<?, ?>) body).get("message"));
    }
}
