package com.chiveshell.updater;

/**
 * @author wuyr
 * @since 2018-11-02 下午2:10
 */
public interface NetAccessor {

    String getNewestVersionName();

    String getDownloadUrl();
}
