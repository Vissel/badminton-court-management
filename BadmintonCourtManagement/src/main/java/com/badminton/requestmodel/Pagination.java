package com.badminton.requestmodel;

import lombok.Data;

@Data
public class Pagination {
    private int current;
    private int pageSize;
    private int totalPage;
}
