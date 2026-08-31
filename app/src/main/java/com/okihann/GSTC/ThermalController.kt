package com.okihann.GSTC

object ThermalController {

    fun setProfile(profile: String) {
        val limit = when (profile) {
            "MAX PERFORMANCE (100°C)" -> 100000
            "UNLOCKED / GAMING (80°C)" -> 80000
            "CPU INTENSE (90°C)" -> 90000
            "BATTERY SAVER (55°C)" -> 55000
            else -> 70000 // BALANCED (70°C)
        }

        val baseCommands = if (profile != "BALANCED (70°C)") {
            "stop thermal-engine
" +
            "stop thermald
" +
            "stop vendor.thermal-hal-2-0
" +
            "stop vendor.thermal-hal-3-0
" +
            "stop android.hardware.thermal@2.0-service
" +
            "stop vendor.qti.hardware.thermal-service
" +
            "stop mi_thermald
" +
            "stop joyose
" +
            "for file in /vendor/etc/thermal-*.conf /system/vendor/etc/thermal-*.conf /vendor/bin/thermal-engine; do
" +
            "    if [ -f "\$file" ] && ! grep -q "\$file" /proc/mounts; then
" +
            "        mount --bind /dev/null "\$file" 2>/dev/null
" +
            "    fi
" +
            "done
" +
            "start thermal-engine
" +
            "start thermald
" +
            "start vendor.thermal-hal-2-0
" +
            "start vendor.hal-3-0
" +
            "start vendor.qti.hardware.thermal-service
" +
            "start mi_thermald
" +
            "start joyose
" +
            "echo 0 > /proc/sys/kernel/sched_child_runs_first 2>/dev/null
"
        } else {
            "stop thermal-engine
" +
            "stop thermald
" +
            "stop vendor.thermal-hal-2-0
" +
            "stop vendor.thermal-hal-3-0
" +
            "stop vendor.qti.hardware.thermal-service
" +
            "stop mi_thermald
" +
            "stop joyose
" +
            "for file in /vendor/etc/thermal-*.conf /system/vendor/etc/thermal-*.conf /vendor/bin/thermal-engine; do
" +
            "    if grep -q "\$file" /proc/mounts; then
" +
            "        umount "\$file" 2>/dev/null
" +
            "    fi
" +
            "done
" +
            "start thermal-engine
" +
            "start thermald
" +
            "start vendor.thermal-hal-2-0
" +
            "start vendor.thermal-hal-3-0
" +
            "start vendor.qti.hardware.thermal-service
" +
            "start mi_thermald
" +
            "start joyose
" +
            "echo 1 > /proc/sys/kernel/sched_child_runs_first 2>/dev/null
"
        }

        val spoofing = if (profile != "BALANCED (70°C)") {
            "for zone in 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
" +
            "    if grep -qiE "battery|bms" /sys/class/thermal/thermal_zone\$zone/type 2>/dev/null; then
" +
            "        if [ -f "/sys/class/thermal/thermal_zone\$zone/emul_temp" ]; then
" +
            "            chmod 644 /sys/class/thermal/thermal_zone\$zone/emul_temp 2>/dev/null
" +
            "            echo 35000 > /sys/class/thermal/thermal_zone\$zone/emul_temp 2>/dev/null
" +
            "        fi
" +
            "    fi
" +
            "done
"
        } else ""

        val cpuLock = if (profile != "BALANCED (70°C)") {
            "echo "performance" > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor 2>/dev/null
" +
            "echo "performance" > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor 2>/dev/null
" +
            "echo "MAX" > /data/local/tmp/gstc_mode
" +
            "( 
" +
            "  while [ "\$(cat /data/local/tmp/gstc_mode 2>/dev/null)" = "MAX" ]; do
" +
            "      for cd in /sys/class/thermal/cooling_device*; do
" +
            "          echo 0 > "\$cd/cur_state" 2>/dev/null
" +
            "      done
" +
            "      if [ -f "/sys/class/kgsl/kgsl-3d0/devfreq/governor" ]; then
" +
            "          echo "performance" > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null
" +
            "          cat /sys/class/kgsl/kgsl-3d0/max_gpuclk > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq 2>/dev/null
" +
            "      fi
" +
            "      
" +
            "      CUR_TEMP=\$(for z in /sys/class/thermal/thermal_zone*; do if grep -qiE "cpu|soc|tsens" "\$z/type" 2>/dev/null; then cat "\$z/temp" 2>/dev/null; fi; done | sort -nr | head -n 1)
" +
            "      if [ ! -z "\$CUR_TEMP" ]; then
" +
            "          PANIC_LIMIT=$limit
" +
            "          WARN_LIMIT=\$((PANIC_LIMIT - 3000))
" +
            "          SOFT_LIMIT=\$((PANIC_LIMIT - 6000))
" +
            "          
" +
            "          if [ "\$CUR_TEMP" -ge "\$PANIC_LIMIT" ]; then
" +
            "              for cpu in 0 1 2 3 4 5 6 7; do
" +
            "                  cat /sys/devices/system/cpu/cpu\$cpu/cpufreq/cpuinfo_min_freq > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_max_freq 2>/dev/null
" +
            "              done
" +
            "          elif [ "\$CUR_TEMP" -ge "\$WARN_LIMIT" ]; then
" +
            "              for cpu in 0 1 2 3 4 5 6 7; do
" +
            "                  MAX=\$(cat /sys/devices/system/cpu/cpu\$cpu/cpufreq/cpuinfo_max_freq 2>/dev/null)
" +
            "                  TARGET=\$((MAX * 6 / 10))
" +
            "                  echo \$TARGET > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_max_freq 2>/dev/null
" +
            "              done
" +
            "          elif [ "\$CUR_TEMP" -ge "\$SOFT_LIMIT" ]; then
" +
            "              for cpu in 0 1 2 3 4 5 6 7; do
" +
            "                  MAX=\$(cat /sys/devices/system/cpu/cpu\$cpu/cpufreq/cpuinfo_max_freq 2>/dev/null)
" +
            "                  TARGET=\$((MAX * 8 / 10))
" +
            "                  echo \$TARGET > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_max_freq 2>/dev/null
" +
            "              done
" +
            "          else
" +
            "              for cpu in 0 1 2 3 4 5 6 7; do
" +
            "                  cat /sys/devices/system/cpu/cpu\$cpu/cpufreq/cpuinfo_max_freq > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_max_freq 2>/dev/null
" +
            "              done
" +
            "          fi
" +
            "      fi
" +
            "      
" +
            "      for batt in /sys/class/power_supply/battery/temp /sys/class/power_supply/battery/batt_temp /sys/class/power_supply/bms/temp; do
" +
            "          if [ -f "\$batt" ]; then
" +
            "              chmod 644 "\$batt" 2>/dev/null
" +
            "              echo 350 > "\$batt" 2>/dev/null
" +
            "              echo 35000 > "\$batt" 2>/dev/null
" +
            "          fi
" +
            "      done
" +
            "      if [ -f "/sys/class/thermal/thermal_message/sconfig" ]; then
" +
            "          echo 10 > /sys/class/thermal/thermal_message/sconfig 2>/dev/null
" +
            "      fi
" +
            "      sleep 1
" +
            "  done
" +
            ") &
"
        } else {
            "echo "BALANCED" > /data/local/tmp/gstc_mode
" +
            "for cpu in 0 1 2 3 4 5 6 7; do
" +
            "    chmod 644 /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_governor /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_max_freq /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_min_freq 2>/dev/null
" +
            "    echo "walt" > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_governor 2>/dev/null || echo "schedutil" > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_governor 2>/dev/null
" +
            "    cat /sys/devices/system/cpu/cpu\$cpu/cpufreq/cpuinfo_max_freq > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_max_freq 2>/dev/null
" +
            "    cat /sys/devices/system/cpu/cpu\$cpu/cpufreq/cpuinfo_min_freq > /sys/devices/system/cpu/cpu\$cpu/cpufreq/scaling_min_freq 2>/dev/null
" +
            "done
" +
            "if [ -f "/sys/class/thermal/thermal_message/sconfig" ]; then
" +
            "    echo 0 > /sys/class/thermal/thermal_message/sconfig 2>/dev/null
" +
            "fi
"
        }

        val shellScript = baseCommands + spoofing + cpuLock
        Thread { RootShell.runCommand(shellScript) }.start()
    }
}