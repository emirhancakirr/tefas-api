package com.tefasfundapi.tefasFundAPI.dto;

import java.util.List;

/**
 * Genel paginasyon şablonu: data + meta.
 * T generic tip olabilir (FundDto, PriceRowDto vs.)
 */
public class PagedResponse<T> {

    private List<T> data;
    private Meta meta;

    public PagedResponse(List<T> data, Meta meta) {
        this.data = data;
        this.meta = meta;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    /** Sayfalama bilgisi */
    public static class Meta {
        private int page;
        private int size;
        private int totalElements;
        private int totalPages;

        public Meta(int page, int size, int totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(int totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }
}