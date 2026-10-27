// SPDX-License-Identifier: GPL-2.0-only
/*
 * Copyright (C) 2026 \xx
 *
 * Test No-Op Vmap Patch for Bootloop Isolation
 */

#ifndef __KSU_H_VMAP_PATCH
#define __KSU_H_VMAP_PATCH

#include <linux/mm.h>

static inline void patch_ptr_slot_kick_cpu(void **target_slot, void *new_ptr)
{
	// No-op test
}

static noinline void read_and_replace_syscall(void *old_ptr, unsigned long syscall_nr, void *new_ptr, void *target_table)
{
	// Sengaja dikosongkan sementara untuk tes bootloop
	pr_info("ksu: [TEST] read_and_replace_syscall bypassed for syscall %lu\n", syscall_nr);
}

static noinline void restore_syscall(void *old_ptr, unsigned long syscall_nr, void *new_ptr, void *target_table)
{
	// Sengaja dikosongkan sementara untuk tes bootloop
	pr_info("ksu: [TEST] restore_syscall bypassed for syscall %lu\n", syscall_nr);
}

// just so we can print out syscall_nr
#define do_read_and_replace_syscall read_and_replace_syscall
#define read_and_replace_syscall(old_p, syscall_nr, new_p, table) ({		\
	pr_info("read_and_replace_syscall: hooking %s\n", #syscall_nr);		\
	do_read_and_replace_syscall(old_p, syscall_nr, new_p, table);		\
})

#define do_restore_syscall restore_syscall
#define restore_syscall(old_p, syscall_nr, new_p, table) ({			\
	pr_info("restore_syscall: unhooking %s\n", #syscall_nr);		\
	do_restore_syscall(old_p, syscall_nr, new_p, table);			\
})

#endif // __KSU_H_VMAP_PATCH
