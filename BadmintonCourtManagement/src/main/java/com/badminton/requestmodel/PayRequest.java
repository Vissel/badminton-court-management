package com.badminton.requestmodel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayRequest extends AvaPlayerDTO {
    @NotBlank
    private String payType;
}
