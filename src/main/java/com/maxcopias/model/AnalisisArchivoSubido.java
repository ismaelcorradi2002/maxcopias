package com.maxcopias.model;

public class AnalisisArchivoSubido {

    private final String originalFilename;
    private final String contentType;
    private final long sizeInBytes;
    private final int pageCount;

    public AnalisisArchivoSubido(String originalFilename, String contentType, long sizeInBytes, int pageCount) {
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.pageCount = pageCount;
    }

    public String getOriginalFilename() {
        return originalFilename;
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
}

