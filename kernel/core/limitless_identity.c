/*
 *  Limitless Identity v3.0
 *
 *  Copyright (C) 2026 ThRE Team
 */

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/utsname.h>
#include <generated/compile.h>
#include <generated/utsrelease.h>

#ifdef CONFIG_LIMITLESS

extern struct uts_namespace init_uts_ns;

char thre_kernel_name[64] = UTS_RELEASE;
char thre_user_name[64]   = LINUX_COMPILE_BY;
char thre_host_name[64]   = LINUX_COMPILE_HOST;
char thre_compiler[128]   = LINUX_COMPILER;
char thre_build_date[128] = UTS_VERSION;

static char default_kernel_name[64] = UTS_RELEASE;
static char default_user_name[64]   = LINUX_COMPILE_BY;
static char default_host_name[64]   = LINUX_COMPILE_HOST;
static char default_compiler[128]   = LINUX_COMPILER;
static char default_build_date[128] = UTS_VERSION;

module_param_string(default_kernel_name, default_kernel_name, sizeof(default_kernel_name), 0444);
module_param_string(default_user_name,   default_user_name,   sizeof(default_user_name),   0444);
module_param_string(default_host_name,   default_host_name,   sizeof(default_host_name),   0444);
module_param_string(default_compiler,    default_compiler,    sizeof(default_compiler),    0444);
module_param_string(default_build_date,  default_build_date,  sizeof(default_build_date),  0444);

EXPORT_SYMBOL_GPL(thre_kernel_name);
EXPORT_SYMBOL_GPL(thre_user_name);
EXPORT_SYMBOL_GPL(thre_host_name);
EXPORT_SYMBOL_GPL(thre_compiler);
EXPORT_SYMBOL_GPL(thre_build_date);

static int set_kernel_name(const char *val, const struct kernel_param *kp)
{
    char tmp[64];
    size_t len;
    snprintf(tmp, sizeof(tmp), "%s", val);
    len = strlen(tmp);
    if (len > 0 && tmp[len - 1] == '\n')
        tmp[len - 1] = '\0';
    snprintf(thre_kernel_name, sizeof(thre_kernel_name), "%s", tmp);
    snprintf(init_uts_ns.name.release, sizeof(init_uts_ns.name.release), "%s", tmp);
    return 0;
}

static int set_user_name(const char *val, const struct kernel_param *kp)
{
    char tmp[64];
    size_t len;
    snprintf(tmp, sizeof(tmp), "%s", val);
    len = strlen(tmp);
    if (len > 0 && tmp[len - 1] == '\n')
        tmp[len - 1] = '\0';
    
    snprintf(thre_user_name, sizeof(thre_user_name), "%s", tmp);
    return 0;
}

static int set_host_name(const char *val, const struct kernel_param *kp)
{
    char tmp[64];
    size_t len;
    snprintf(tmp, sizeof(tmp), "%s", val);
    len = strlen(tmp);
    if (len > 0 && tmp[len - 1] == '\n')
        tmp[len - 1] = '\0';

    snprintf(thre_host_name, sizeof(thre_host_name), "%s", tmp);
    return 0;
}

static int set_compiler_name(const char *val, const struct kernel_param *kp)
{
    char tmp[128];
    size_t len;
    snprintf(tmp, sizeof(tmp), "%s", val);
    len = strlen(tmp);
    if (len > 0 && tmp[len - 1] == '\n')
        tmp[len - 1] = '\0';

    snprintf(thre_compiler, sizeof(thre_compiler), "%s", tmp);
    return 0;
}

static int set_build_date(const char *val, const struct kernel_param *kp)
{
    char tmp[128];
    size_t len;
    snprintf(tmp, sizeof(tmp), "%s", val);
    len = strlen(tmp);
    if (len > 0 && tmp[len - 1] == '\n')
        tmp[len - 1] = '\0';

    snprintf(thre_build_date, sizeof(thre_build_date), "%s", tmp);
    snprintf(init_uts_ns.name.version, sizeof(init_uts_ns.name.version), "%s", tmp);
    return 0;
}

static int param_get_thre_string(char *buffer, const struct kernel_param *kp)
{
    return scnprintf(buffer, PAGE_SIZE, "%s", (char *)kp->arg);
}

static const struct kernel_param_ops kernel_name_ops = { .set = set_kernel_name, .get = param_get_thre_string };
static const struct kernel_param_ops user_ops = { .set = set_user_name, .get = param_get_thre_string };
static const struct kernel_param_ops host_ops = { .set = set_host_name, .get = param_get_thre_string };
static const struct kernel_param_ops comp_ops = { .set = set_compiler_name, .get = param_get_thre_string };
static const struct kernel_param_ops date_ops = { .set = set_build_date, .get = param_get_thre_string };

module_param_cb(thre_kernel_name, &kernel_name_ops, thre_kernel_name, 0644);
module_param_cb(thre_user_name,   &user_ops,        thre_user_name,   0644);
module_param_cb(thre_host_name,   &host_ops,        thre_host_name,   0644);
module_param_cb(thre_compiler,    &comp_ops,        thre_compiler,    0644);
module_param_cb(thre_build_date,  &date_ops,        thre_build_date,  0644);

#endif
