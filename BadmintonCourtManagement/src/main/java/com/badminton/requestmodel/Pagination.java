package com.badminton.requestmodel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pagination {
    private int current;
    private int pageSize;
    private int totalPage;
}
