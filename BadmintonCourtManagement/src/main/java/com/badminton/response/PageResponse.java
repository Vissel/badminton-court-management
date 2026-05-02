package com.badminton.response;

import com.badminton.requestmodel.Pagination;
import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private Pagination pagination;
    private List<T> list;
    private long total;
}
