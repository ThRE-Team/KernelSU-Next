#ifndef __MANAGER_LIST_H
#define __MANAGER_LIST_H

#include <linux/types.h>
#include <linux/kernel.h>

struct manager_identity {
	const char *hash;
	const uint32_t sig_size;
};

static const struct manager_identity allowed_managers[] = {
	{
		// KernelSU-Next Official
		.hash = "79e590113c4c4c0c222978e413a5faa801666957b1212a328e46c00c69821bf7",
		.sig_size = 0x3e6
	},
	{
		// Next Manager
		.hash = "1ab6077099505a4f5ff851732d5d965a4908af7f60c871f23b4b3a58e80e6cd3",
		.sig_size = 0x31c
	}
};

#endif
