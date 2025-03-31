package com.modulith.infraagentteam.domain.webHook.usecase;

import com.modulith.infraagentteam.infra.config.NoPrException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Builder
@Slf4j
public record BitbucketPrMergeUseCase(String commitId, String repositoryName, String branchName) {
    public static BitbucketPrMergeUseCase toDomain(Map<String, Object> payload) {
        try {
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pullrequest");
            Map<String, Object> destination = (Map<String, Object>) pullRequest.get("destination");
            Map<String, Object> mergeCommit = (Map<String, Object>) pullRequest.get("merge_commit");

            String isPrMerged = (String) pullRequest.get("state");
            if (!isPrMerged.equals("MERGED")) {
                throw new NoPrException();
            }

            String branchName =  ((Map<String, Object>) destination.get("branch")).get("name").toString();
            String commitSha = (String) mergeCommit.get("hash");
            String repoName = (String) ((Map<String, Object>) payload.get("repository")).get("full_name");

            return BitbucketPrMergeUseCase.builder()
                    .branchName(branchName)
                    .commitId(commitSha)
                    .repositoryName(repoName)
                    .build();
        }catch (Exception e){
            throw new NoPrException();
        }
    }
}
