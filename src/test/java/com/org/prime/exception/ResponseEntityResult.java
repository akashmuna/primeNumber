package com.org.prime.exception;

import com.org.prime.model.ErrorResponse;
import org.springframework.http.HttpStatusCode;

public record ResponseEntityResult(HttpStatusCode status, ErrorResponse body) {}
