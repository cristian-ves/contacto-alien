package com.usac.contactoalien.contactoalienbackend.api.tokenize;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TokenizeController {

    private final TokenizerService tokenizerService;

    public TokenizeController(TokenizerService tokenizerService) {
        this.tokenizerService = tokenizerService;
    }

    @PostMapping("/tokenize")
    public TokenizeResponse tokenize(@RequestBody TokenizeRequest request) {
        return tokenizerService.tokenize(request);
    }
}