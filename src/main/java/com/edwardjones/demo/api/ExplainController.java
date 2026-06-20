package com.edwardjones.demo.api;

import com.edwardjones.demo.ai.ExplainRequest;
import com.edwardjones.demo.ai.ExplainResponse;
import com.edwardjones.demo.ai.TaxExplanationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/explain")
@CrossOrigin(origins = "*")
public class ExplainController {

    private final TaxExplanationService explanationService;

    public ExplainController(TaxExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    @PostMapping
    public ResponseEntity<?> explain(@Valid @RequestBody ExplainRequest request) {
        try {
            ExplainResponse response = explanationService.explain(request.lotId(), request.context());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
