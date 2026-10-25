/*
 *  Limitless S-OC v3.0
 *
 *  Copyright (C) 2026 ThRE Team
 */

#ifndef __LIMITLESS_H
#define __LIMITLESS_H

#include <linux/clk-provider.h>
#include <linux/spinlock.h>

#ifndef OSM_TABLE_SIZE
#define OSM_TABLE_SIZE 40
#endif

struct osm_entry {
	u16 virtual_corner;
	u16 open_loop_volt;
	unsigned long frequency;
	u32 lval;
	u16 ccount;
};

struct clk_osm {
	struct clk_hw hw;
	struct osm_entry osm_table[OSM_TABLE_SIZE];
	struct dentry *debugfs;
	void __iomem *vbase;
	phys_addr_t pbase;
	spinlock_t lock;
	bool per_core_dcvs;
	u32 num_entries;
	u32 cluster_num;
	u32 core_num;
	unsigned long rate;
	u64 total_cycle_counter;
	u32 prev_cycle_counter;
	u32 max_core_count;
	u32 mx_turbo_freq;
};

extern struct clk_osm perfcl_clk;

#endif
