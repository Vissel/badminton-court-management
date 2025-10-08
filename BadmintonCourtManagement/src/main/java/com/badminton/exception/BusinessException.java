package com.badminton.exception;

import com.badminton.enums.BizCode;

import lombok.Data;

@Data
public class BusinessException extends Exception {
	private BizCode bizCode;

	public BusinessException(BizCode bizCode, String errorMessage) {
		super(errorMessage);
		this.bizCode = bizCode;
	}
}
