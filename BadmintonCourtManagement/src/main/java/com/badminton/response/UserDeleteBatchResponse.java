package com.badminton.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of a bulk removal. The request is not all-or-nothing: every account
 * that passes the guards is removed, and the ones that cannot be removed are
 * reported back so the operator knows what stayed in the list.
 */
@Getter
@Setter
public class UserDeleteBatchResponse {
    /** Number of distinct accounts the request asked to remove. */
    private int requested;
    /** Usernames that were soft-deleted by this request. */
    private List<String> removed = new ArrayList<>();
    /** Username (or id) plus the Vietnamese reason it was left untouched. */
    private List<String> skipped = new ArrayList<>();
}
