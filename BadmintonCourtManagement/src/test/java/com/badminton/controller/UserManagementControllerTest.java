package com.badminton.controller;

import com.badminton.requestmodel.DeleteUsersRequest;
import com.badminton.response.UserDeleteBatchResponse;
import com.badminton.response.result.Result;
import com.badminton.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserManagementController userManagementController;

    @Test
    void bulkDeleteForwardsTheSelectedIdsAndReturnsTheBatchOutcome() {
        DeleteUsersRequest request = new DeleteUsersRequest();
        request.setUserIds(List.of(4L, 5L));
        UserDeleteBatchResponse batch = new UserDeleteBatchResponse();
        batch.setRequested(2);
        batch.getRemoved().addAll(List.of("coord1", "coord2"));
        batch.getSkipped().add("admin1: tài khoản Root không thể xoá");
        Result<UserDeleteBatchResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(batch);
        when(userService.deleteUsers(List.of(4L, 5L))).thenReturn(result);

        ResponseEntity<Result<UserDeleteBatchResponse>> response = userManagementController.deleteUsers(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(List.of("coord1", "coord2"), response.getBody().getData().getRemoved());
        assertEquals(1, response.getBody().getData().getSkipped().size());
    }

    @Test
    void bulkDeleteWithNothingRemovableAnswersBadRequest() {
        DeleteUsersRequest request = new DeleteUsersRequest();
        request.setUserIds(List.of(2L));
        Result<UserDeleteBatchResponse> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.BAD_REQUEST.value());
        result.setErrorMessage("Không xoá được tài khoản nào. root2: tài khoản Root không thể xoá");
        when(userService.deleteUsers(List.of(2L))).thenReturn(result);

        ResponseEntity<Result<UserDeleteBatchResponse>> response = userManagementController.deleteUsers(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getErrorMessage().contains("root2"));
    }
}
