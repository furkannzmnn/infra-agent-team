package com.modulith.infraagentteam.domain.shared.model.query;

import lombok.Builder;

@Builder
public record BitbucketFileRetrieveRequest(String repo, String commit, String fileName) {
}
