package org.example.controller;

import org.springframework.validation.BindingResult;

import org.example.exception.BusinessException;

final class FormErrors {
    private FormErrors() {}

    static void reject(BindingResult errors, BusinessException exception) {
        if (exception.getField() == null) errors.reject("business", exception.getMessage());
        else errors.rejectValue(exception.getField(), "business", exception.getMessage());
    }
}
