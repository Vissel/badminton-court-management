package com.badminton.repository.filter;

import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

@Data
public class SessionParam {
    private Instant from;
    private Instant to;
    private Pageable pageable;
}
