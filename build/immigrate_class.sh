#!/usr/bin/env bash

#!/bin/bash

walk_dir () {
    # nullglob 如果设置，bash允许没有匹配任何文件的文件名模式扩展成一个空串，而不是它们本身
    # noglob -d 禁止用路径名扩展。即关闭通配符。
    shopt -s nullglob dotglob

    for pathname in "$1"/*; do
        # 判断是否为文件夹
        if [ -d "$pathname" ]; then
            # 递归调用遍历子目录
            walk_dir "$pathname"
        else
        	# 输出想要的文件
            case "$pathname" in
                *.txt|*.java)
                    printf '%s\n' "$pathname"
                    # 实现自己的需求
                    sed -i '' "s#\SyncConf #SyncConfig #g; s#\SyncConf;#SyncConfig;#g" "${pathname}"
                    sed -i '' "s#\FieldConf #FieldConfig #g; s#\FieldConf;#FieldConfig;#g" "${pathname}"
                    sed -i '' "s#\ BaseFileConf # BaseFileConfig #g; s#\BaseFileConf;#BaseFileConfig;#g" "${pathname}"
            esac
        fi
    done
}

DOWNLOADING_DIR="/Users/lisai/projects/chunjun-1.16.0-221130/chunjun-connectors/"

walk_dir "$DOWNLOADING_DIR"

