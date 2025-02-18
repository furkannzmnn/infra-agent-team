package com.modulith.infraagentteam.domain.webHook.api;

import com.modulith.infraagentteam.domain.webHook.service.BitbucketPrMergeUseCaseHandler;
import com.modulith.infraagentteam.domain.webHook.usecase.BitbucketPrMergeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook/bitbucket-webhook")
@Slf4j
@RequiredArgsConstructor
public class BitbucketWebHookController {

    private final BitbucketPrMergeUseCaseHandler bitbucketPrMergeUseCaseHandler;

    @PostMapping("/pr-merge")
    public ResponseEntity<String> handlePullRequestMerge(@RequestBody Map<String, Object> payload) {
        log.info("Received Bitbucket PR merge webhook: {}", payload);
        bitbucketPrMergeUseCaseHandler.handle(BitbucketPrMergeUseCase.toDomain(payload));
        return ResponseEntity.ok("OK");
    }

    }
