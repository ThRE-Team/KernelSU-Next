/*
 *  Limitless S-OC v3.0
 *
 *  Copyright (C) 2026 ThRE Team
 */

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/moduleparam.h>
#include "limitless.h" 

#ifdef CONFIG_LIMITLESS
static int thre_oc_level = -1;
static int set_oc_level(const char *val, const struct kernel_param *kp);

static const struct kernel_param_ops oc_level_ops = {
	.set = set_oc_level,
	.get = param_get_int,
};

module_param_cb(thre_oc_level, &oc_level_ops, &thre_oc_level, 0644);

static int set_oc_level(const char *val, const struct kernel_param *kp)
{
	int ret, level;
	u32 oc_lval;
	struct clk_osm *c = &perfcl_clk;

	ret = kstrtoint(val, 10, &level);
	if (ret) return ret;

	thre_oc_level = level;

	if (c && c->osm_table) {
		switch (level) {
			case 99: oc_lval = 0x9a; break;
			case 24: oc_lval = 0x99; break;
			case 4:  oc_lval = 0x98; break;
			case 3:  oc_lval = 0x96; break;
			case 2:  oc_lval = 0x94; break;
			case 1:  oc_lval = 0x92; break;
			case 0:  oc_lval = 0x90; break;
			case -1: oc_lval = 0x70; break;
			case -2: oc_lval = 0x50; break;
			case -3: oc_lval = 0x30; break;
			default: oc_lval = 0x75; break;
		}
		
		c->osm_table[OSM_TABLE_SIZE - 1].lval = oc_lval;
		pr_err("Limitless: S-OC Level 0x%x applied by ThRE Team\n", oc_lval);
	}
	return 0;
}
#endif
