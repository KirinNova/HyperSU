#include <string>
#include <cstring>
#include <cstdarg>
#include <cerrno>
#include <dlfcn.h>
#include <cstdlib>
#include <unistd.h>
#include <vector>
#include <sys/system_properties.h>

// Skip drop privileges
// https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/modules/adb/daemon/main.cpp;l=123;drc=3b74954ec50836e0c8faeed1877fefb1dc2de006
extern "C"
[[gnu::visibility("default"), gnu::used]]
// NOLINTNEXTLINE(bugprone-reserved-identifier)
int __android_log_is_debuggable() {
    return 1;
}

struct prop_info {};

static struct prop_info g_adb_root_prop;

// Enable adb root by default, without run `adb root` command explicitly or setprop
// https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/modules/adb/daemon/main.cpp;l=84;drc=3b74954ec50836e0c8faeed1877fefb1dc2de006
// https://cs.android.com/android/platform/superproject/+/android-latest-release:system/libbase/properties.cpp;l=159-175;drc=6d19b5c690fa4220c10e42c2326150bbd4d4b7bb
// android::base::GetProperty is from libbase.so, but some adbd are statically-linked to it,
// so we replace these two underlying functions.

extern "C" [[gnu::visibility("default"), gnu::used]]
const prop_info* __system_property_find(const char* name) {
    if (name != nullptr && strcmp(name, "service.adb.root") == 0) {
        return &g_adb_root_prop;
    }

    static decltype(__system_property_find) *system_property_find_fn = nullptr;
    if (!system_property_find_fn) {
        system_property_find_fn = reinterpret_cast<decltype(system_property_find_fn)>(
            dlsym(RTLD_NEXT, "__system_property_find"));
    }
    if (!system_property_find_fn) {
        return nullptr;
    }
    return system_property_find_fn(name);
}

extern "C" [[gnu::visibility("default"), gnu::used]]
void __system_property_read_callback(const prop_info* pi,
    void (*callback)(void* cookie, const char* name, const char* value, uint32_t serial),
    void* cookie) {
    if (pi == &g_adb_root_prop) {
        if (callback) {
            callback(cookie, "service.adb.root", "1", 0);
        }
        return;
    }

    static decltype(__system_property_read_callback) *orig_fn = nullptr;
    if (!orig_fn) {
        orig_fn = reinterpret_cast<decltype(orig_fn)>(
            dlsym(RTLD_NEXT, "__system_property_read_callback"));
    }
    if (!orig_fn) {
        return;
    }
    orig_fn(pi, callback, cookie);
}

// If the user creates ksurc, use ksurc instead of mkshrc
// https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/modules/adb/daemon/shell_service.cpp;l=389-394
extern "C" [[gnu::visibility("default"), gnu::used]]
int execle(const char *pathname, const char *arg, ...) {
    if (pathname == nullptr) {
        errno = EINVAL;
        return -1;
    }

    std::vector<const char *> argv_list;
    va_list va;
    va_start(va, arg);

    // start dump argv
    if (arg != nullptr) {
        argv_list.push_back(arg);
        const char *next;
        while ((next = va_arg(va, const char*)) != nullptr) {
            argv_list.push_back(next);
        }
    }
    argv_list.push_back(nullptr);

    char *const *old_envp = va_arg(va, char* const*);
    va_end(va);

    // start dump envp
    std::vector<const char *> env_list;
    bool ksurc_exists = (access("/data/adb/ksu/.ksurc", F_OK) == 0);
    const char *ksu_env_str = "ENV=/data/adb/ksu/.ksurc";

    if (old_envp != nullptr) {
        for (size_t i = 0; old_envp[i] != nullptr; ++i) {
            // skip original ENV in envp
            // even there shouldn't exists in normal android, but let's still check it, avoid newer android/custom rom modify there
            if (ksurc_exists && strncmp(old_envp[i], "ENV=", 4) == 0) {
                continue;
            }
            env_list.push_back(old_envp[i]);
        }
    }

    if (ksurc_exists) {
        // push our ENV in here!!!
        env_list.push_back(ksu_env_str);
    }
    env_list.push_back(nullptr);

    // skip origin execle, because we already completed everything of execle
    return execve(pathname,
                  const_cast<char *const *>(argv_list.data()),
                  const_cast<char *const *>(env_list.data()));
}

// skip setcon
extern "C"
[[gnu::visibility("default"), gnu::used]]
int selinux_android_setcon(const char *con) {
    return 0;
}

[[gnu::used, gnu::constructor(0)]]
void Init() {
    unsetenv("LD_PRELOAD");
    unsetenv("LD_LIBRARY_PATH");
    const char *old_path = getenv("PATH");
    std::string path = old_path ? old_path : "";
    if (!path.empty()) {
        path += ":/data/adb/ksu/bin";
    } else {
        path += "/data/adb/ksu/bin";
    }
    setenv("PATH", path.c_str(), 1);
}
