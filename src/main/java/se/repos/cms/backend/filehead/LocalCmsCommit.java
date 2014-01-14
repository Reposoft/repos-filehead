/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cms.backend.filehead;

import javax.inject.Inject;

import se.repos.authproxy.ReposCurrentUser;
import se.simonsoft.cms.item.CmsItemLock;
import se.simonsoft.cms.item.CmsItemLockCollection;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.commit.CmsItemLockedException;
import se.simonsoft.cms.item.commit.CmsPatchItem;
import se.simonsoft.cms.item.commit.CmsPatchset;
import se.simonsoft.cms.item.commit.FileAdd;
import se.simonsoft.cms.item.commit.FileDelete;
import se.simonsoft.cms.item.commit.FileModification;
import se.simonsoft.cms.item.commit.FolderAdd;
import se.simonsoft.cms.item.commit.FolderDelete;

public class LocalCmsCommit implements CmsCommit {
    private CmsRepository repository;
    private ReposCurrentUser currentUser;

    @Inject
    public LocalCmsCommit(CmsRepository repository, ReposCurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Override
    public RepoRevision run(CmsPatchset fileModifications) throws CmsItemLockedException {
        for (CmsPatchItem change : fileModifications) {
            LocalCmsItem changedItem = new LocalCmsItem(this.repository,
                    this.currentUser, change.getPath());
            if (change instanceof FileModification) {
                changedItem.writeContents(((FileModification) change).getWorkingFile());
            } else if (change instanceof FileAdd) {
                changedItem.writeContents(((FileAdd) change).getWorkingFile());
            } else if (change instanceof FileDelete) {
                changedItem.delete();
            } else if (change instanceof FolderAdd) {
                changedItem.mkdir();
            } else if (change instanceof FolderDelete) {
                changedItem.delete();
            } else {
                throw new UnsupportedOperationException(
                        "Filesystem modification not supported for change type "
                                + change.getClass().getSimpleName());
            }
        }
        return new LocalRepoRevision();
    }

    @Override
    public CmsItemLockCollection lock(String message, RepoRevision base,
            CmsItemPath... item) throws CmsItemLockedException {
        LocalCmsItemLockCollection locks = new LocalCmsItemLockCollection(this.repository);
        for (CmsItemPath toLock : item) {
            locks.add(LocalCmsItemLock.createLocalLock(this.repository, this.currentUser,
                    toLock, message));
        }
        return locks;
    }

    @SuppressWarnings("serial")
    private class LocalCmsItemLockCollection extends CmsItemLockCollection {

        public LocalCmsItemLockCollection(CmsRepository repository) {
            super(repository);
        }

        public void add(LocalCmsItemLock lock) {
            super.add(lock);
        }
    }

    @Override
    public void unlock(CmsItemLock... lock) {
        for (CmsItemLock toUnlock : lock) {
            if (!(toUnlock instanceof LocalCmsItemLock)) {
                throw new IllegalArgumentException(
                        "Non local lock passed to local commit class!");
            }
            LocalCmsItemLock localLock = (LocalCmsItemLock) toUnlock;
            localLock.unlock();
        }
    }
}
