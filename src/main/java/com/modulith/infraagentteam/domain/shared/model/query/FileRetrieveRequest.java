package com.modulith.infraagentteam.domain.shared.model.query;

import lombok.Builder;

@Builder
public record FileRetrieveRequest(String repo, String commit, String fileName, GitType gitType
                                  ) {
}
