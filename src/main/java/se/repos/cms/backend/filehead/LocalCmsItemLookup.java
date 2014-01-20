/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cms.backend.filehead;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import se.repos.authproxy.ReposCurrentUser;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemLock;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.info.CmsConnectionException;
import se.simonsoft.cms.item.info.CmsItemLookup;
import se.simonsoft.cms.item.info.CmsItemNotFoundException;

public class LocalCmsItemLookup implements CmsItemLookup {
    private CmsRepository repository;
    private ReposCurrentUser currentUser;
    private RepoRevision currentRevision;

    @Inject
    public LocalCmsItemLookup(CmsRepository repository, ReposCurrentUser currentUser,
            RepoRevision currentRevision) {
        if (repository == null || currentUser == null || currentRevision == null) {
            throw new NullPointerException();
        }
        this.repository = repository;
        this.currentUser = currentUser;
        this.currentRevision = currentRevision;
    }

    @Override
    public CmsItem getItem(CmsItemId id) throws CmsConnectionException,
            CmsItemNotFoundException {
        return this.getLocalCmsItem(id);
    }

    private LocalCmsItem getLocalCmsItem(CmsItemId id) throws CmsItemNotFoundException {
        CmsItemPath itemPath = id.getRelPath();
        LocalCmsItem file = new LocalCmsItem(this.repository, this.currentUser, itemPath,
                this.currentRevision);
        if (!file.exists()) {
            String newPathString;
            if (itemPath.getPath().startsWith(this.repository.getPath())) {
                newPathString = itemPath.getPath().substring(
                        this.repository.getPath().length());
            } else {
                newPathString = itemPath.getPath();
            }
            CmsItemPath newPath = new CmsItemPath(newPathString);
            throw new CmsItemNotFoundException(this.repository, newPath);
        }
        return file;
    }

    @Override
    public Set<CmsItemId> getImmediateFolders(CmsItemId parent)
            throws CmsConnectionException, CmsItemNotFoundException {
        Set<CmsItemId> immediates = new LinkedHashSet<CmsItemId>();
        for (LocalCmsItem item : this.getLocalImmediates(parent, ItemType.FOLDER)) {
            immediates.add(item.getId());
        }
        return immediates;
    }

    @Override
    public Set<CmsItemId> getImmediateFiles(CmsItemId parent)
            throws CmsConnectionException, CmsItemNotFoundException {
        Set<CmsItemId> immediates = new LinkedHashSet<CmsItemId>();
        for (LocalCmsItem item : this.getLocalImmediates(parent, ItemType.FILE)) {
            immediates.add(item.getId());
        }
        return immediates;
    }

    @Override
    public Set<CmsItem> getImmediates(CmsItemId parent) throws CmsConnectionException,
            CmsItemNotFoundException {
        Set<CmsItem> immediates = new LinkedHashSet<CmsItem>();
        immediates.addAll(this.getLocalImmediates(parent, ItemType.BOTH));
        return immediates;
    }

    private Set<LocalCmsItem> getLocalImmediates(CmsItemId parent, ItemType itemType) {
        return LocalCmsItemLookup.getLocalImmediates(this.getLocalCmsItem(parent),
                itemType);
    }

    private static Set<LocalCmsItem> getLocalImmediates(LocalCmsItem parent,
            ItemType itemType) {
        Set<LocalCmsItem> localImmediates = new LinkedHashSet<LocalCmsItem>();
        for (LocalCmsItem child : parent.getChildItems()) {
            boolean add = false;
            switch (itemType) {
            case BOTH:
                add = true;
                break;
            case FILE:
                add = child.getKind() == CmsItemKind.File;
                break;
            case FOLDER:
                add = child.getKind() == CmsItemKind.Folder;
                break;
            }
            if (add) {
                localImmediates.add(child);
            }
        }
        return localImmediates;
    }

    @Override
    public Iterable<CmsItemId> getDescendants(CmsItemId parent) {
        Set<CmsItemId> children = new LinkedHashSet<CmsItemId>();
        this.getLocalDescendants(children, this.getLocalCmsItem(parent));
        return children;
    }

    private void getLocalDescendants(Set<CmsItemId> children, LocalCmsItem parent) {
        for (LocalCmsItem child : LocalCmsItemLookup.getLocalImmediates(parent,
                ItemType.BOTH)) {
            children.add(child.getId());
        }
        for (LocalCmsItem folder : LocalCmsItemLookup.getLocalImmediates(parent,
                ItemType.FOLDER)) {
            this.getLocalDescendants(children, folder);
        }
    }

    @Override
    public CmsItemLock getLocked(CmsItemId itemId) {
        return LocalCmsItemLock.getLocalLock(this.repository, this.currentUser,
                itemId.getRelPath());
    }
}
