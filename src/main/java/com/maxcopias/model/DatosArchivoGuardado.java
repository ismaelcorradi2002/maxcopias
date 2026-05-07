package com.maxcopias.model;

public class DatosArchivoGuardado {

    private final String originalFilename;
    private final String storedFilename;
    private final String relativePath;
    private final String contentType;
    private final long sizeInBytes;
    private final int pageCount;

    public DatosArchivoGuardado(
        String originalFilename,
        String storedFilename,
        String relativePath,
        String contentType,
        long sizeInBytes,
        int pageCount
    ) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.relativePath = relativePath;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.pageCount = pageCount;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getReadableSize() {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        }

        double sizeInKb = sizeInBytes / 1024.0;
        if (sizeInKb < 1024) {
            return String.format("%.1f KB", sizeInKb);
        }

        return String.format("%.2f MB", sizeInKb / 1024.0);
    }

    public boolean isRemote() {
        return relativePath != null
            && (relativePath.startsWith("https://") || relativePath.startsWith("http://"));
    }
}

