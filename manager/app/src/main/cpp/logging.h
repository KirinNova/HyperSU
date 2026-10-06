#pragma once

#include <android/log.h>
#include <cerrno>
#include <cstring>
#include <string>

#ifndef LOG_TAG
# define LOG_TAG "KernelSU"
#endif

#ifndef NDEBUG
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...)  do {} while (0)
#define LOGV(...)  do {} while (0)
#endif
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__)

#define PLOGE(fmt, ...)                                              \
    do {                                                             \
        const int _ploge_errno = errno;                              \
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,              \
            fmt " failed with %d: %s", ##__VA_ARGS__,                \
            _ploge_errno, strerror(_ploge_errno));                   \
    } while (0)
