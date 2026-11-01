package com.badminton.requestmodel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {
    private String role;
    private Boolean active;
    /** ROOT-only manager assignment: administrator userId, or null to unlink. */
    private Long managerUserId;
}
