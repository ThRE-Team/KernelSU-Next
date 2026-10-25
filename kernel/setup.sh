#!/bin/sh
set -eu

GKI_ROOT=$(pwd)
OWNER="ThRE-Team"
REPO="KernelSU-Next"

display_usage() {
    echo "Usage: $0 [--cleanup | <commit-or-tag>]"
    echo "  --cleanup:              Cleans up previous modifications made by the script."
    echo "  <commit-or-tag>:        Sets up or updates the KernelSU-Next to specified tag or commit."
    echo "  -h, --help:             Displays this usage information."
    echo "  (no args):              Sets up or updates the KernelSU-Next environment to the latest tagged version."
}

initialize_variables() {
    if test -d "$GKI_ROOT/common/drivers"; then
         DRIVER_DIR="$GKI_ROOT/common/drivers"
    elif test -d "$GKI_ROOT/drivers"; then
         DRIVER_DIR="$GKI_ROOT/drivers"
    else
         echo '[ERROR] "drivers/" directory not found.'
         exit 127
    fi

    DRIVER_MAKEFILE=$DRIVER_DIR/Makefile
    DRIVER_KCONFIG=$DRIVER_DIR/Kconfig
}

# Reverts modifications made by this script
perform_cleanup() {
    echo "[+] Cleaning up..."
    if [ -L "$DRIVER_DIR/kernelsu" ]; then
        rm "$DRIVER_DIR/kernelsu" && echo "[-] Symlink removed."
    elif [ -d "$DRIVER_DIR/kernelsu" ]; then
        rm -rf "$DRIVER_DIR/kernelsu" && echo "[-] Folder removed."
    fi
    grep -q "kernelsu" "$DRIVER_MAKEFILE" && sed -i '/kernelsu/d' "$DRIVER_MAKEFILE" && echo "[-] Makefile reverted."
    grep -q "drivers/kernelsu/Kconfig" "$DRIVER_KCONFIG" && sed -i '/drivers\/kernelsu\/Kconfig/d' "$DRIVER_KCONFIG" && echo "[-] Kconfig reverted."
    if [ -d "$GKI_ROOT/$REPO" ]; then
        rm -rf "$GKI_ROOT/$REPO" && echo "[-] $REPO directory deleted."
    fi
}

# Sets up or update KernelSU-Next environment
setup_kernelsu() {
    if [ -d "$DRIVER_DIR/kernelsu" ]; then
        echo "[+] Preparing fresh setup (cleaning up old instances)..."
        rm -rf "$DRIVER_DIR/kernelsu"
    fi
    echo "[+] Setting up $REPO..."
    test -d "$GKI_ROOT/$REPO" || git clone "https://github.com/$OWNER/$REPO" && echo "[+] Repository cloned."
    cd "$GKI_ROOT/$REPO"
    git stash && echo "[-] Stashed current changes."

    BRANCH="$(git rev-parse --abbrev-ref origin/HEAD | sed 's@^origin/@@')"
    if [ "$(git status | grep -Po 'v\d+(\.\d+)*' | head -n1)" ]; then
        git checkout $BRANCH && echo "[-] Switched to $BRANCH branch."
    fi

    git pull && echo "[+] Repository updated."
    if [ -z "${1-}" ]; then
        git checkout "$(git describe --abbrev=0 --tags)" && echo "[-] Checked out latest tag."
    else
        git checkout "$1" && echo "[-] Checked out $1." || echo "[-] Checkout default branch"
    fi
    cd "$DRIVER_DIR"
    ln -sf "$(realpath --relative-to="$DRIVER_DIR" "$GKI_ROOT/$REPO/kernel")" "kernelsu" && echo "[+] Symlink created."

    # Add entries in Makefile and Kconfig if not already existing
    grep -q "kernelsu" "$DRIVER_MAKEFILE" || printf "\nobj-\$(CONFIG_KSU) += kernelsu/\n" >> "$DRIVER_MAKEFILE" && echo "[+] Modified Makefile."
    grep -q "source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" || sed -i "/endmenu/i\source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" && echo "[+] Modified Kconfig."
    echo '[+] Done.'
}

# Setup Limitless Addon
limitless_addon() {
echo "[+] Setting up Limitess Addon..."
limitless_identity
limitless_soc
limitless_stealth
}

limitless_identity() {
    if [ -d "$GKI_ROOT/common/init" ]; then
        INIT_DIR="$GKI_ROOT/common/init"
    elif [ -d "$GKI_ROOT/init" ]; then
        INIT_DIR="$GKI_ROOT/init"
    else
        return 0
    fi

    VERSION_C="$INIT_DIR/version.c"
    PROC_VERSION_C="$GKI_ROOT/fs/proc/version.c"

    HAS_INIT_VER=0
    HAS_PROC_VER=0

    [ -f "$VERSION_C" ] && grep -q "CONFIG_LIMITLESS" "$VERSION_C" && HAS_INIT_VER=1
    [ -f "$PROC_VERSION_C" ] && grep -q "CONFIG_LIMITLESS" "$PROC_VERSION_C" && HAS_PROC_VER=1

    if [ "$HAS_INIT_VER" -eq 1 ] && [ "$HAS_PROC_VER" -eq 1 ]; then
        echo "[+] Setting up Limitess Identity..."

        INIT_MAKEFILE="$INIT_DIR/Makefile"
        LIMITLESS_SRC="$GKI_ROOT/$REPO/kernel/core/limitless_identity.c"

        if [ -f "$LIMITLESS_SRC" ]; then
            LIMITLESS_REL_SRC="$(realpath --relative-to="$INIT_DIR" "$LIMITLESS_SRC")"
            ln -sf "$LIMITLESS_REL_SRC" "$INIT_DIR/limitless.c" && echo "[+] Symlink created."

            if ! grep -q "CONFIG_LIMITLESS" "$INIT_MAKEFILE"; then
                echo "[+] Injecting CONFIG_LIMITLESS block into $INIT_MAKEFILE..."
                
                cp "$INIT_MAKEFILE" "$INIT_MAKEFILE.orig"

                sed -i '/obj-y[[:space:]]*:= main.o version.o mounts.o/c\
ifeq ($(CONFIG_LIMITLESS),y)\
obj-y := main.o version.o limitless.o mounts.o\
else\
obj-y := main.o version.o mounts.o\
endif' "$INIT_MAKEFILE" && echo "[+] Makefile successfully patched."
            else
                echo "[-] CONFIG_LIMITLESS entry already exists in Makefile."
            fi
        else
            echo "[-] $LIMITLESS_SRC not found, skipping Limitless Identity."
        fi
    fi
}

limitless_soc() {
    if [ -d "$GKI_ROOT/common/drivers/clk/qcom" ]; then
        CLK_DIR="$GKI_ROOT/common/drivers/clk/qcom"
    elif [ -d "$GKI_ROOT/drivers/clk/qcom" ]; then
        CLK_DIR="$GKI_ROOT/drivers/clk/qcom"
    else
        return 0
    fi

    OSM_C="$CLK_DIR/clk-cpu-osm.c"
    HAS_OSM_SOC=0

    [ -f "$OSM_C" ] && grep -q "CONFIG_LIMITLESS" "$OSM_C" && HAS_OSM_SOC=1

    if [ "$HAS_OSM_SOC" -eq 1 ]; then
        echo "[+] Setting up Limitless S-OC..."
        
        CLK_MAKEFILE="$CLK_DIR/Makefile"
        LIMITLESS_C_SRC="$GKI_ROOT/$REPO/kernel/core/limitless_soc.c"
        LIMITLESS_H_SRC="$GKI_ROOT/$REPO/kernel/core/limitless_soc.h"

        if [ -f "$LIMITLESS_C_SRC" ] && [ -f "$LIMITLESS_H_SRC" ]; then
            ln -sf "$(realpath --relative-to="$CLK_DIR" "$LIMITLESS_C_SRC")" "$CLK_DIR/limitless.c" && echo "[+] Symlink limitless.c created."
            ln -sf "$(realpath --relative-to="$CLK_DIR" "$LIMITLESS_H_SRC")" "$CLK_DIR/limitless.h" && echo "[+] Symlink limitless.h created."

            if [ -f "$CLK_MAKEFILE" ]; then
                if ! grep -q "CONFIG_LIMITLESS" "$CLK_MAKEFILE"; then
                    echo "[+] Injecting CONFIG_LIMITLESS into $CLK_MAKEFILE..."
                    cp "$CLK_MAKEFILE" "$CLK_MAKEFILE.orig"
                    printf "\nobj-\$(CONFIG_LIMITLESS) += limitless.o\n" >> "$CLK_MAKEFILE" && echo "[+] Makefile successfully patched."
                else
                    echo "[-] CONFIG_LIMITLESS entry already exists in Makefile."
                fi
            fi
        else
            echo "[-] Limitless source files not found in $REPO repository, skipping."
        fi
    else
        echo "[-] S-OC hook not found, skipping Limitless S-OC."
    fi
}

# Limitless Stealth | Next Hide
limitless_stealth() {
    if [ -d "$GKI_ROOT/common/fs" ]; then
        FS_DIR="$GKI_ROOT/common/fs"
    elif [ -d "$GKI_ROOT/fs" ]; then
        FS_DIR="$GKI_ROOT/fs"
    else
        return 0
    fi

    STEALTH_C_SRC="$GKI_ROOT/$REPO/kernel/core/limitless_stealth.c"
    STEALTH_H_SRC="$GKI_ROOT/$REPO/kernel/core/limitless_stealth.h"
    NAMEI_C="$FS_DIR/namei.c"
    OPEN_C="$FS_DIR/open.c"
    READDIR_C="$FS_DIR/readdir.c"
    STAT_C="$FS_DIR/stat.c"

    # Gabung pengecekan file sumber dan 4 VFS hook sekaligus
    if [ -f "$STEALTH_C_SRC" ] && [ -f "$STEALTH_H_SRC" ] && \
       [ -f "$NAMEI_C" ] && grep -q "next_hide.h" "$NAMEI_C" && \
       [ -f "$OPEN_C" ] && grep -q "next_hide.h" "$OPEN_C" && \
       [ -f "$READDIR_C" ] && grep -q "next_hide.h" "$READDIR_C" && \
       [ -f "$STAT_C" ] && grep -q "next_hide.h" "$STAT_C"; then
        
        echo "[+] Setting up Limitless Stealth..."

        ln -sf "$(realpath --relative-to="$FS_DIR" "$STEALTH_C_SRC")" "$FS_DIR/next_hide.c" && echo "[+] Symlink next_hide.c created."
        INCLUDE_LINUX_DIR="$GKI_ROOT/include/linux"
        [ -d "$GKI_ROOT/common/include/linux" ] && INCLUDE_LINUX_DIR="$GKI_ROOT/common/include/linux"
        
        mkdir -p "$INCLUDE_LINUX_DIR"
        ln -sf "$(realpath --relative-to="$INCLUDE_LINUX_DIR" "$STEALTH_H_SRC")" "$INCLUDE_LINUX_DIR/next_hide.h" && echo "[+] Symlink next_hide.h created."

        FS_MAKEFILE="$FS_DIR/Makefile"
        if [ -f "$FS_MAKEFILE" ]; then
            if ! grep -q "next_hide.o" "$FS_MAKEFILE"; then
                echo "[+] Injecting CONFIG_LIMITLESS into $FS_MAKEFILE..."
                cp "$FS_MAKEFILE" "$FS_MAKEFILE.orig"
                printf "\nobj-\$(CONFIG_LIMITLESS) += next_hide.o\n" >> "$FS_MAKEFILE" && echo "[+] Makefile successfully patched."
            else
                echo "[-] next_hide.o entry already exists in Makefile."
            fi
        fi
    else
        echo "[-] Limitless Stealth source files or VFS hooks not found, skipping."
    fi
}

# Limitless Cleanup
limitless_cleanup() {
    # Limitless Idenrity
    INIT_DIR_CLEAN="$GKI_ROOT/init"
    [ -d "$GKI_ROOT/common/init" ] && INIT_DIR_CLEAN="$GKI_ROOT/common/init"
    
    if [ -L "$INIT_DIR_CLEAN/limitless.c" ]; then
        rm -f "$INIT_DIR_CLEAN/limitless.c" && echo "[-] Limitless symlink removed."
    fi
    if [ -f "$INIT_DIR_CLEAN/Makefile.orig" ]; then
        mv "$INIT_DIR_CLEAN/Makefile.orig" "$INIT_DIR_CLEAN/Makefile" && echo "[-] init/Makefile.orig restored."
    fi
    # Limitless S-OC
    CLK_DIR_CLEAN="$GKI_ROOT/drivers/clk/qcom"
    [ -d "$GKI_ROOT/common/drivers/clk/qcom" ] && CLK_DIR_CLEAN="$GKI_ROOT/common/drivers/clk/qcom"
    if [ -n "$CLK_DIR_CLEAN" ]; then
        if [ -L "$CLK_DIR_CLEAN/limitless.c" ]; then
            rm -f "$CLK_DIR_CLEAN/limitless.c" && echo "[-] Limitless.c symlink removed."
        fi
        if [ -L "$CLK_DIR_CLEAN/limitless.h" ]; then
            rm -f "$CLK_DIR_CLEAN/limitless.h" && echo "[-] Limitless.h symlink removed."
        fi
        if [ -f "$CLK_DIR_CLEAN/Makefile.orig" ]; then
            mv "$CLK_DIR_CLEAN/Makefile.orig" "$CLK_DIR_CLEAN/Makefile" && echo "[-] drivers/clk/qcom/Makefile.orig restored."
        fi
    fi
    # Limitless Stealth (next_hide)
    FS_DIR_CLEAN="$GKI_ROOT/fs"
    [ -d "$GKI_ROOT/common/fs" ] && FS_DIR_CLEAN="$GKI_ROOT/common/fs"
    INCLUDE_LINUX_CLEAN="$GKI_ROOT/include/linux"
    [ -d "$GKI_ROOT/common/include/linux" ] && INCLUDE_LINUX_CLEAN="$GKI_ROOT/common/include/linux"

    if [ -L "$FS_DIR_CLEAN/next_hide.c" ]; then
        rm -f "$FS_DIR_CLEAN/next_hide.c" && echo "[-] next_hide.c symlink removed."
    fi
    if [ -L "$INCLUDE_LINUX_CLEAN/next_hide.h" ]; then
        rm -f "$INCLUDE_LINUX_CLEAN/next_hide.h" && echo "[-] next_hide.h symlink removed."
    fi
    if [ -f "$FS_DIR_CLEAN/Makefile.orig" ]; then
        mv "$FS_DIR_CLEAN/Makefile.orig" "$FS_DIR_CLEAN/Makefile" && echo "[-] fs/Makefile.orig restored."
    fi
}

# Process command-line arguments
if [ "$#" -eq 0 ]; then
    initialize_variables
    setup_kernelsu
    limitless_addon
elif [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    display_usage
elif [ "$1" = "--cleanup" ]; then
    initialize_variables
    perform_cleanup
    limitless_cleanup
else
    initialize_variables
    setup_kernelsu "$@"
    limitless_addon
fi
