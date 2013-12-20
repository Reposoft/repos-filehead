/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cms.backend.filehead;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;

import se.repos.authproxy.ReposCurrentUser;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemLock;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.commit.CmsItemLockedException;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;

public class LocalCmsItemLock implements CmsItemLock {
    private CmsItemPath lockPath;

    private CmsRepository repository;
    private ReposCurrentUser currentUser;

    @Inject
    public void setRepository(CmsRepository repository) {
        this.repository = repository;
    }

    @Inject
    public void setReposCurrentUser(ReposCurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    private LocalCmsItemLock(CmsItemPath lockPath) {
        this.lockPath = lockPath;
    }

    public static LocalCmsItemLock createLocalLock(CmsRepository repository,
            CmsItemPath lockedItem, String lockComment) throws CmsItemLockedException {
        if (isLocked(repository, lockedItem)) {
            throw new CmsItemLockedException(repository, lockedItem);
        }

        Date creationDate = new Date();
        CmsItemPath lockPath = getLockPath(lockedItem);
        File lockFile = LocalCmsItemLock.getLockFile(repository, lockPath);
        String lockFileContents = creationDate.getTime() + ":" + lockComment;
        try {
            FileUtils.write(lockFile, lockFileContents);
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
        return new LocalCmsItemLock(lockPath);
    }

    public static LocalCmsItemLock getLocalLock(CmsRepository repository,
            CmsItemPath lockedItem) {
        if (!isLocked(repository, lockedItem)) {
            return null;
        }
        CmsItemPath lockPath = getLockPath(lockedItem);
        return new LocalCmsItemLock(lockPath);
    }

    public static boolean isLocked(CmsRepository repository, CmsItemPath item) {
        return getLockFile(repository, getLockPath(item)).exists();
    }

    private static CmsItemPath getLockPath(CmsItemPath lockedItem) {
        // The lock file has the same name as the item it locks, but with a
        // ".lock" suffix.
        return new CmsItemPath(lockedItem.getPath() + ".lock");
    }

    private static File getLockFile(CmsRepository repository, CmsItemPath lockPath) {
        String filePath = repository.getPath() + lockPath.getPath();
        return new File(filePath);
    }

    private static String getLockFileContents(CmsRepository repository,
            CmsItemPath lockPath) {
        try {
            return FileUtils.readFileToString(getLockFile(repository, lockPath));
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public String getComment() {
        return LocalCmsItemLock.getLockFileContents(this.repository, this.lockPath)
                .split(":")[1];
    }

    @Override
    public Date getDateCreation() {
        try {
            long epochTime = Long.parseLong(LocalCmsItemLock.getLockFileContents(
                    this.repository, this.lockPath).split(":")[0]);
            return new Date(epochTime);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public Date getDateExpiration() {
        // There is no defined expiration date for this lock.
        return null;
    }

    @Override
    public String getToken() {
        // The token is the lock file contents.
        return getLockFileContents(this.repository, this.lockPath);
    }

    @Override
    public String getOwner() {
        return this.currentUser.getUsername();
    }

    @Override
    public CmsItemId getItemId() {
        return new CmsItemIdUrl(this.repository, this.lockPath);
    }

    /**
     * Unlocks this lock by deleting the lock file. Using the receiving object
     * after calling this method is undefined.
     */
    public void unlock() {
        LocalCmsItemLock.getLockFile(this.repository, this.lockPath).delete();
    }
}
