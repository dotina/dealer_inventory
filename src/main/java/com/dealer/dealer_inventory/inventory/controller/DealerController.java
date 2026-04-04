package com.dealer.dealer_inventory.inventory.controller;

import com.dealer.dealer_inventory.inventory.dto.DealerCreateRequest;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.DealerUpdateRequest;
import com.dealer.dealer_inventory.inventory.service.DealerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dealers")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealerResponse create(@Valid @RequestBody DealerCreateRequest request) {
        return dealerService.create(request);
    }

    @GetMapping("/{id}")
    public DealerResponse getById(@PathVariable UUID id) {
        return dealerService.getById(id);
    }

    @GetMapping
    public Page<DealerResponse> list(Pageable pageable) {
        return dealerService.list(pageable);
    }

    @PatchMapping("/{id}")
    public DealerResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody DealerUpdateRequest request) {
        return dealerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        dealerService.delete(id);
    }
}

