#ifndef __KSU_H_SUCOMPAT
#define __KSU_H_SUCOMPAT
#include <asm/ptrace.h>
#include <linux/types.h>

#if defined(CONFIG_KSU_SUSFS) && defined(KSU_COMPAT_USE_STATIC_KEY)
extern struct static_key_true ksu_su_compat_enabled;
#else
extern bool ksu_su_compat_enabled;
#endif

#ifdef CONFIG_KSU_SUSFS
int ksu_handle_faccessat(int *dfd, struct filename **filename, int *mode,
             int *__unused_flags);
int ksu_handle_stat(int *dfd, struct filename **filename, int *flags);
int ksu_handle_execveat_sucompat(int *fd, struct filename **filename_ptr,
                 void *argv_user, void *envp_user,
                 int *__never_use_flags);
int ksu_handle_post_execveat_sucompat(int *fd, struct filename **filename_ptr, void *argv_user, void *envp_user,
                                      int *flags, int *retval);
#else
int ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode, int *__unused_flags);
int ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags);
int ksu_handle_execve_sucompat(int *fd, const char __user **filename_user, void *argv, void *__never_use_envp,
                               int *__never_use_flags);
int ksu_handle_execveat_sucompat(int *fd, struct filename **filename_ptr, void *argv, void *__never_use_envp,
                                 int *__never_use_flags);
#endif

void ksu_sucompat_init(void);
void ksu_sucompat_exit(void);

#endif
