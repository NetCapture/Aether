#!/usr/bin/env bash

source_common() {
  pwd=$(
    cd $(dirname $0)
    pwd
  )
  source $pwd/common.sh
}

clean_caches() {

  logw "[$filename]clean android studio cache!"
  dir=("app" "netbare-core" "netbare-injector")

  for element in "${dir[@]}"; do
    #clean task
    rm -rf $element/build/
    rm -rf $element/bin/
    rm -rf $element/gen/
    rm -rf $element/.settings/
    rm -rf $element/.externalNativeBuild
    rm -rf $element/$element.iml
    rm -rf $element/.gradle
    logd "[$filename]clean $element over."
  done

  rm -rf build/
  rm -rf release/
  rm -rf releasebak/
  rm -rf *.iml
  rm -rf .gradle/
  rm -rf .idea/
  rm -rf sh.exe.stackdump
  rm -rf classes.dex
  rm -rf local.properties
  rm -rf .vs/
  rm -rf .vscode/

  if [ $# == 0 ]; then
    logw "[$filename]clean project success."
    loge "[$filename]>>>>you must close android studio<<<<"
  else
    loge "[$filename]>>clean project Failed!<<"
  fi

}

main() {
  source_common
  clean_caches
}

main
