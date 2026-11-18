#ifndef __KSU_H_KSU_MANAGER
#define __KSU_H_KSU_MANAGER

#include <linux/cred.h>
#include <linux/types.h>

#define KSU_INVALID_APPID -1
#define KSU_PER_USER_RANGE 100000
#define KSU_MAX_MANAGERS 2

#ifdef CONFIG_KSU_DISABLE_MANAGER
static inline bool ksu_is_manager_appid_valid()
{
    return true;
}

static inline bool is_manager()
{
    return current_uid().val == 0;
}

static inline bool is_uid_manager(uid_t uid)
{
    return uid == 0;
}

static inline uid_t ksu_get_manager_appid()
{
    return 0;
}

static inline void ksu_set_manager_appid(uid_t appid)
{
    (void)appid;
}

static inline void ksu_invalidate_manager_uid()
{
}
#else
extern uid_t ksu_manager_appids[KSU_MAX_MANAGERS];

static inline bool ksu_is_manager_appid_valid()
{
	int i;
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		if (ksu_manager_appids[i] != KSU_INVALID_APPID)
			return true;
	}
	return false;
}

static inline bool is_uid_manager(uid_t uid)
{
	int i;
	uid_t appid = uid % KSU_PER_USER_RANGE;
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		if (unlikely(ksu_manager_appids[i] != KSU_INVALID_APPID &&
			     ksu_manager_appids[i] == appid))
			return true;
	}
	return false;
}

static inline bool is_manager()
{
	return is_uid_manager(current_uid().val);
}

static inline uid_t ksu_get_manager_appid()
{
	int i;
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		if (ksu_manager_appids[i] != KSU_INVALID_APPID)
			return ksu_manager_appids[i];
	}
	return KSU_INVALID_APPID;
}

static inline void ksu_set_manager_appid(uid_t appid)
{
	int i;
	if (appid == KSU_INVALID_APPID)
		return;
	// Cek jika sudah terdaftar
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		if (ksu_manager_appids[i] == appid)
			return;
	}
	// Masukkan ke slot kosong
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		if (ksu_manager_appids[i] == KSU_INVALID_APPID) {
			ksu_manager_appids[i] = appid;
			return;
		}
	}
	// Jika penuh, timpa slot terakhir
	ksu_manager_appids[KSU_MAX_MANAGERS - 1] = appid;
}

static inline void ksu_invalidate_manager_uid_single(uid_t appid)
{
	int i;
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		if (ksu_manager_appids[i] == appid)
			ksu_manager_appids[i] = KSU_INVALID_APPID;
	}
}

static inline void ksu_invalidate_manager_uid()
{
	int i;
	for (i = 0; i < KSU_MAX_MANAGERS; i++) {
		ksu_manager_appids[i] = KSU_INVALID_APPID;
	}
}
#endif

#endif