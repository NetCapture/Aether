#!/usr/bin/env bash

source_common() {
  pwd=$(
    cd $(dirname $0)
    pwd
  )
  source $pwd/common.sh
}

clean_task() {
  logw "[$filename]>>>>clean project<<<<"
  dir=("app" "netbare-core" "netbare-injector")
  for element in "${dir[@]}"; do
    #clean task
    rm -rf $element/build/
    rm -rf $element/bin/
    rm -rf $element/gen/
    rm -rf $element/.externalNativeBuild
    logd "[$filename]clean $element over."
  done

  rm -rf build/
  rm -rf release/
  rm -rf releasebak/
  rm -rf sh.exe.stackdump
  rm -rf classes.dex
  rm -rf .vs/
  rm -rf .vscode/

  if [ $# == 0 ]; then
    logi "[$filename]clean project success. "
  else
    loge "[$filename]clean project Failed!"
  fi

}

main() {
  source_common
  clean_task
}

main
