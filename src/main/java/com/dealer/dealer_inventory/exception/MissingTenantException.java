package com.dealer.dealer_inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingTenantException extends RuntimeException {
    public MissingTenantException() {
        super("Missing required header: X-Tenant-Id");
    }
}

