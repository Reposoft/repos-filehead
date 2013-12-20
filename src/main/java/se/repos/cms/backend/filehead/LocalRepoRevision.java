/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cms.backend.filehead;

import java.util.Date;

import se.simonsoft.cms.item.RepoRevision;

public class LocalRepoRevision extends RepoRevision {

    protected LocalRepoRevision(Date revisionTimestamp) {
        super(revisionTimestamp);
    }
}
