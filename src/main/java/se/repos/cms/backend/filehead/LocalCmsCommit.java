/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cms.backend.filehead;

import javax.inject.Inject;

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

    @Inject
    public void setRepository(CmsRepository repository) {
        this.repository = repository;
    }

    @Override
    public RepoRevision run(CmsPatchset fileModifications) throws CmsItemLockedException {
        for (CmsPatchItem change : fileModifications) {
            if (change instanceof FileModification) {
                LocalCmsCommit.handle((FileModification) change);
            } else if (change instanceof FileAdd) {
                LocalCmsCommit.handle((FileAdd) change);
            } else if (change instanceof FileDelete) {
                LocalCmsCommit.handle((FileDelete) change);
            } else if (change instanceof FolderAdd) {
                LocalCmsCommit.handle((FolderAdd) change);
            } else if (change instanceof FolderDelete) {
                LocalCmsCommit.handle((FolderDelete) change);
            } else {
                throw new UnsupportedOperationException(
                        "Filesystem modification not supported for change type "
                                + change.getClass().getSimpleName());
            }
        }
        return new LocalRepoRevision();
    }

    private static void handle(FolderDelete change) {
        // TODO Auto-generated method stub
        
    }

    private static void handle(FolderAdd change) {
        // TODO Auto-generated method stub
        
    }

    private static void handle(FileModification change) {
        LocalCmsItem changedItem = new LocalCmsItem(change.getPath());
        if (!changedItem.exists()) {
            throw new RuntimeException("Could not find modified item: "
                    + change.getPath());
        }
        changedItem.writeContents(change.getWorkingFile());
    }

    private static void handle(FileAdd change) {
        LocalCmsItem newItem = new LocalCmsItem(change.getPath());
        newItem.writeContents(change.getWorkingFile());
    }

    private static void handle(FileDelete change) {
        LocalCmsItem deletedItem = new LocalCmsItem(change.getPath());
        deletedItem.delete();
    }

    @Override
    public CmsItemLockCollection lock(String message, RepoRevision base,
            CmsItemPath... item) throws CmsItemLockedException {
        LocalCmsItemLockCollection locks = new LocalCmsItemLockCollection(this.repository);
        for (CmsItemPath toLock : item) {
            locks.add(LocalCmsItemLock.createLocalLock(this.repository, toLock, message));
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
