package com.badminton.requestmodel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** ROOT-only bulk removal: the accounts ticked in user management. */
@Getter
@Setter
@NoArgsConstructor
public class DeleteUsersRequest {
    private List<Long> userIds;
}
