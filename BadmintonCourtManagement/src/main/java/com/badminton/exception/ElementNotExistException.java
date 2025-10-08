package com.badminton.exception;

import com.badminton.enums.BizCode;

public class ElementNotExistException extends BusinessException {

	public ElementNotExistException(BizCode bizCode, String errorMessage) {
		super(bizCode, errorMessage);
		// TODO Auto-generated constructor stub
	}

}
