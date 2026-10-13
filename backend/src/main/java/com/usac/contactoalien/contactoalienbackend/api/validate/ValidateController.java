package com.usac.contactoalien.contactoalienbackend.api.validate;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ValidateController {

    private final ValidatorService validatorService;

    public ValidateController(ValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    @PostMapping("/validate")
    public ValidateResponse validate(@RequestBody ValidateRequest request) {
        return validatorService.validate(request);
    }
}