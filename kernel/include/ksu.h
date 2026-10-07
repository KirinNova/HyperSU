#ifndef __KSU_H_KSU
#define __KSU_H_KSU

#include <linux/types.h>
#include <linux/cred.h>
#include <linux/workqueue.h>

#define KERNEL_SU_VERSION KSU_VERSION

extern struct cred *ksu_cred;
extern bool allow_shell;
#if LINUX_VERSION_CODE >= KERNEL_VERSION(5, 10, 0) || defined(KSU_COMPAT_HAS_SELINUX_POLICY_STRUCT)
extern struct selinux_policy *backup_sepolicy;
#else
extern struct policydb *backup_policydb;
extern struct sidtab *backup_sidtab;
#endif
extern bool ksu_no_custom_rc;

static inline int startswith(char *s, char *prefix)
{
    return strncmp(s, prefix, strlen(prefix));
}

static inline int endswith(const char *s, const char *t)
{
    size_t slen = strlen(s);
    size_t tlen = strlen(t);
    if (tlen > slen)
        return 1;
    return strcmp(s + slen - tlen, t);
}

#endif
