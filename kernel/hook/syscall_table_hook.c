// SPDX-License-Identifier: GPL-2.0-only
/*
 * Copyright (C) 2026 \xx
 *
 * This file is a downstream extension and NOT affiliated, endorsed by,
 * or maintained by the official KernelSU developers.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Optimized for ARM64 Kernel 4.4 - 4.9
 *
 */

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/version.h>
#include <linux/linkage.h>
#include <linux/sched.h>
#include <linux/uaccess.h>
#include <linux/kthread.h>
#include <linux/delay.h>
#include <linux/mutex.h>
#include <linux/slab.h>
#include "syscall_table_hook.h"

#ifndef nullptr
#define nullptr NULL
#endif

#ifndef CONFIG_ARM64
#error "This module is strictly optimized and meant only for ARM64 kernels (4.4 - 4.9)!"
#endif

// Global pointers for syscall tables
void **ksu_sys_call_table = NULL;
void **ksu_compat_sys_call_table = NULL;
extern bool ksu_vfs_read_hook;

// Syscall Numbers for ARM64 & AMEABI (Compat 32-bit)
#define __AARCH64_reboot	142
#define __AARCH64_execve	221
#define __AARCH64_execveat	281
#define __AARCH64_faccessat	48
#define __AARCH64_newfstatat	79
#define __AARCH64_newfstat	80
#define __AARCH64_read		63

#define __ARMEABI_reboot	88
#define __ARMEABI_execve	11
#define __ARMEABI_execveat	387
#define __ARMEABI_faccessat	334
#define __ARMEABI_fstatat64	327
#define __ARMEABI_fstat64	197
#define __ARMEABI_read		3

/* ==========================================================
 * NATIVE AARCH64 SYSCALL HOOK HANDLERS (KERNEL 4.4 - 4.9)
 * ========================================================== */

static void *aarch64_reboot __read_mostly = nullptr;
asmlinkage long hook_aarch64_reboot(int magic1, int magic2, unsigned int cmd, void __user *arg)
{
	ksu_handle_sys_reboot(magic1, magic2, cmd, &arg);
	return sys_reboot(magic1, magic2, cmd, arg);
}

static void *aarch64_execve __read_mostly = nullptr;
asmlinkage long hook_aarch64_execve(const char __user *filename, const char __user *const __user *argv, const char __user *const __user *envp)
{
	ksu_handle_sys_execve(&filename, (void ***)&argv, (void ***)&envp);
	return sys_execve(filename, argv, envp);
}

static void *aarch64_execveat __read_mostly = nullptr;
asmlinkage long hook_aarch64_execveat(int fd, const char __user *filename, const char __user *const __user *argv, const char __user *const __user *envp, int flags)
{
	ksu_handle_sys_execveat(&fd, &filename, (void ***)&argv, (void ***)&envp, &flags);
	return sys_execveat(fd, filename, argv, envp, flags);
}

static void *aarch64_faccessat __read_mostly = nullptr;
asmlinkage long hook_aarch64_faccessat(int dfd, const char __user *filename, int mode)
{
	ksu_handle_faccessat(&dfd, &filename, &mode, NULL);
	return sys_faccessat(dfd, filename, mode);
}

static void *aarch64_newfstatat __read_mostly = nullptr;
asmlinkage long hook_aarch64_newfstatat(int dfd, const char __user *filename, struct stat __user *statbuf, int flag)
{
	ksu_handle_stat(&dfd, &filename, &flag);
	return sys_newfstatat(dfd, filename, statbuf, flag);
}

static void *aarch64_newfstat __read_mostly = nullptr;
asmlinkage long hook_aarch64_newfstat_ret(unsigned int fd, struct stat __user *statbuf)
{
	long ret = sys_newfstat(fd, statbuf);
	ksu_handle_newfstat_ret(&fd, &statbuf);
	return ret;
}

static void *aarch64_read __read_mostly = nullptr;
asmlinkage long hook_aarch64_read(unsigned int fd, char __user *buf, size_t count)
{
	ksu_handle_sys_read_fd(fd);
	return sys_read(fd, buf, count);
}

/* ==========================================================
 * COMPAT (ARMEABI 32-BIT) SYSCALL HOOK HANDLERS
 * ========================================================== */
#ifdef CONFIG_COMPAT

static void *armeabi_reboot __read_mostly = nullptr;
asmlinkage long hook_armeabi_reboot(int magic1, int magic2, unsigned int cmd, void __user *arg)
{
	ksu_handle_sys_reboot(magic1, magic2, cmd, &arg);
	return sys_reboot(magic1, magic2, cmd, arg);
}

static void *armeabi_execve __read_mostly = nullptr;
asmlinkage long hook_armeabi_execve(const char __user *filename,
				const compat_uptr_t __user *argv,
				const compat_uptr_t __user *envp)
{
	ksu_handle_sys_execve(&filename, (void ***)&argv, (void ***)&envp);
	return compat_sys_execve(filename, argv, envp);
}

static void *armeabi_execveat __read_mostly = nullptr;
asmlinkage long hook_armeabi_execveat(int fd, const char __user *filename,
				const compat_uptr_t __user *argv,
				const compat_uptr_t __user *envp, int flags)
{
	ksu_handle_sys_execveat(&fd, &filename, (void ***)&argv, (void ***)&envp, &flags);
	return compat_sys_execveat(fd, filename, argv, envp, flags);
}

static void *armeabi_faccessat __read_mostly = nullptr;
asmlinkage long hook_armeabi_faccessat(int dfd, const char __user *filename, int mode)
{
	ksu_handle_faccessat(&dfd, &filename, &mode, NULL);
	return sys_faccessat(dfd, filename, mode);
}

static void *armeabi_fstatat64 __read_mostly = nullptr;
asmlinkage long hook_armeabi_fstatat64(int dfd, const char __user *filename, struct stat64 __user *statbuf, int flag)
{
	ksu_handle_stat(&dfd, &filename, &flag);
	return sys_fstatat64(dfd, filename, statbuf, flag);
}

static void *armeabi_fstat64 __read_mostly = nullptr;
asmlinkage long hook_armeabi_fstat64_ret(unsigned long fd, struct stat64 __user *statbuf)
{
	extern typeof(hook_armeabi_fstat64_ret) sys_fstat64;
	long ret = sys_fstat64(fd, statbuf);
	ksu_handle_fstat64_ret(&fd, &statbuf);
	return ret;
}

static void *armeabi_read __read_mostly = nullptr;
asmlinkage long hook_armeabi_read(unsigned int fd, char __user *buf, size_t count)
{
	ksu_handle_sys_read_fd(fd);
	return sys_read(fd, buf, count);
}

#endif // CONFIG_COMPAT

/* ==========================================================
 * MANAGEMENT & INITIALIZATION LOGIC
 * ========================================================== */

static int ksu_syscall_table_restore(void *data)
{
	set_user_nice(current, 19);

loop_start:
	msleep(1000);

	if (*(volatile bool *)&ksu_vfs_read_hook)
		goto loop_start;

	restore_syscall((void *)&aarch64_newfstat, __AARCH64_newfstat, (void *)hook_aarch64_newfstat_ret, (void *)ksu_sys_call_table);
	restore_syscall((void *)&aarch64_read, __AARCH64_read, (void *)hook_aarch64_read, (void *)ksu_sys_call_table);

#if defined(CONFIG_COMPAT)
	restore_syscall((void *)&armeabi_fstat64, __ARMEABI_fstat64, (void *)hook_armeabi_fstat64_ret, (void *)ksu_compat_sys_call_table);
	restore_syscall((void *)&armeabi_read, __ARMEABI_read, (void *)hook_armeabi_read, (void *)ksu_compat_sys_call_table);
#endif
	
	return 0;
}

static DEFINE_MUTEX(sucompat_toggle_mutex);

static void syscall_table_sucompat_enable(void)
{
	mutex_lock(&sucompat_toggle_mutex);

	read_and_replace_syscall((void *)&aarch64_execve, __AARCH64_execve, (void *)hook_aarch64_execve, (void *)ksu_sys_call_table);
	read_and_replace_syscall((void *)&aarch64_execveat, __AARCH64_execveat, (void *)hook_aarch64_execveat, (void *)ksu_sys_call_table);
	read_and_replace_syscall((void *)&aarch64_faccessat, __AARCH64_faccessat, (void *)hook_aarch64_faccessat, (void *)ksu_sys_call_table);
	read_and_replace_syscall((void *)&aarch64_newfstatat, __AARCH64_newfstatat, (void *)hook_aarch64_newfstatat, (void *)ksu_sys_call_table);

#if defined(CONFIG_COMPAT)
	read_and_replace_syscall((void *)&armeabi_execve, __ARMEABI_execve, (void *)hook_armeabi_execve, (void *)ksu_compat_sys_call_table);
	read_and_replace_syscall((void *)&armeabi_execveat, __ARMEABI_execveat, (void *)hook_armeabi_execveat, (void *)ksu_compat_sys_call_table);
	read_and_replace_syscall((void *)&armeabi_faccessat, __ARMEABI_faccessat, (void *)hook_armeabi_faccessat, (void *)ksu_compat_sys_call_table);
	read_and_replace_syscall((void *)&armeabi_fstatat64, __ARMEABI_fstatat64, (void *)hook_armeabi_fstatat64, (void *)ksu_compat_sys_call_table);
#endif

	mutex_unlock(&sucompat_toggle_mutex);
}

static void syscall_table_ksud_hook_init(void)
{
	read_and_replace_syscall((void *)&aarch64_reboot, __AARCH64_reboot, (void *)hook_aarch64_reboot, (void *)ksu_sys_call_table);
	read_and_replace_syscall((void *)&aarch64_newfstat, __AARCH64_newfstat, (void *)hook_aarch64_newfstat_ret, (void *)ksu_sys_call_table);
	read_and_replace_syscall((void *)&aarch64_read, __AARCH64_read, (void *)hook_aarch64_read, (void *)ksu_sys_call_table);

#if defined(CONFIG_COMPAT)
	read_and_replace_syscall((void *)&armeabi_reboot, __ARMEABI_reboot, (void *)hook_armeabi_reboot, (void *)ksu_compat_sys_call_table);
	read_and_replace_syscall((void *)&armeabi_fstat64, __ARMEABI_fstat64, (void *)hook_armeabi_fstat64_ret, (void *)ksu_compat_sys_call_table);
	read_and_replace_syscall((void *)&armeabi_read, __ARMEABI_read, (void *)hook_armeabi_read, (void *)ksu_compat_sys_call_table);
#endif

	kthread_run(ksu_syscall_table_restore, NULL, "unhook");
}

__init int ksu_syscall_table_hook_init(void)
{
	ksu_sys_call_table = (void **)(kallsyms_lookup_name("sys_call_table"));
	if (!ksu_sys_call_table) {
		pr_err("ksu: failed to lookup sys_call_table\n");
		return -ENODEV;
	}

	ksu_compat_sys_call_table = (void **)(kallsyms_lookup_name("compat_sys_call_table"));

	pr_info("ksu: starting direct syscall table hook for ARM64 4.4-4.9...\n");
	syscall_table_sucompat_enable();
	syscall_table_ksud_hook_init();

	return 0;
}
