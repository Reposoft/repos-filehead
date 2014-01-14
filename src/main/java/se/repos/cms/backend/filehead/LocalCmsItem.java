/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cms.backend.filehead;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import se.repos.authproxy.ReposCurrentUser;
import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.ChecksumBase;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public class LocalCmsItem implements CmsItem {
    private CmsItemPath path;
    private CmsRepository repository;
    private ReposCurrentUser currentUser;

    @Inject
    public LocalCmsItem(CmsRepository repository, ReposCurrentUser currentUser,
            CmsItemPath path) {
        if (repository == null || currentUser == null) {
            throw new NullPointerException();
        }
        
        if(path == null) {
            this.path = new CmsItemPath(repository.getPath());
        } else {
            this.path = path;
        }
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public boolean exists() {
        return this.getTrackedFile().exists();
    }

    public List<LocalCmsItem> getChildItems() {
        ArrayList<LocalCmsItem> children = new ArrayList<LocalCmsItem>();
        if (this.getKind() != CmsItemKind.Folder) {
            return children;
        }
        for (File child : this.getTrackedFile().listFiles()) {
            children.add(new LocalCmsItem(this.repository, this.currentUser, this.path
                    .append(child.getName())));
        }
        return children;
    }

    private File getTrackedFile() {
        return new File(this.path.getPath());
    }

    @Override
    public CmsItemId getId() {
        return new CmsItemIdUrl(this.repository, this.path);
    }

    @Override
    public RepoRevision getRevisionChanged() {
        long lastModified = this.getTrackedFile().lastModified();
        return new LocalRepoRevision(new Date(lastModified));
    }

    @Override
    public String getRevisionChangedAuthor() {
        return this.currentUser.getUsername();
    }

    @Override
    public CmsItemKind getKind() {
        if (this.getTrackedFile().isDirectory()) {
            return CmsItemKind.Folder;
        }
        return CmsItemKind.File;
    }

    @Override
    public String getStatus() {
        // Never set on these items.
        return null;
    }

    @Override
    public Checksum getChecksum() {
        final String md5 = this.calculateFileMD5();
        return new ChecksumBase() {
            @Override
            public boolean has(Algorithm a) {
                return a == Algorithm.MD5;
            }

            @Override
            public String getHex(Algorithm a) {
                if (a == Algorithm.MD5) {
                    return md5;
                }
                throw new UnsupportedOperationException();
            }
        };
    }

    private String calculateFileMD5() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(this.getTrackedFile());
            return DigestUtils.md5Hex(fis);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getCause());
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    @Override
    public CmsItemProperties getProperties() {
        // This class has no properties.
        return new CmsItemProperties() {
            @Override
            public String getString(String key) {
                return null;
            }

            @Override
            public List<String> getList(String key) throws ClassCastException {
                return null;
            }

            @Override
            public Set<String> getKeySet() {
                return null;
            }

            @Override
            public boolean containsProperty(String key) {
                return false;
            }
        };
    }

    @Override
    public long getFilesize() {
        return this.getTrackedFile().getTotalSpace();
    }

    @Override
    public void getContents(OutputStream receiver) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(this.getTrackedFile());
            IOUtils.copy(fis, receiver);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getCause());
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    /**
     * Overwrites the contents of the file with the data from the given input
     * stream.
     */
    public void writeContents(InputStream data) {
        try {
            FileUtils.copyInputStreamToFile(data, this.getTrackedFile());
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Deletes the file this CmsItem tracks.
     */
    public void delete() {
        this.getTrackedFile().delete();
    }
    
    /**
     * Creates a directory with the path given by this item.
     */
    public void mkdir() {
        this.getTrackedFile().mkdirs();
    }
}
