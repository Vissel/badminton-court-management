package com.badminton.repository.filter;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
public class SessionParam {
    private Instant from;
    private Instant to;
    OrderBy orderBy;

    @AllArgsConstructor
    public class OrderBy {
        public String by;
        public String order = "DESC";
    }
}
