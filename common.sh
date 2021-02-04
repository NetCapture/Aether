#!/usr/bin/env bash
# define  global variable
# file name
filename=$(basename $0)
# define adb
iadb="adb"

ipwd=$(
  cd $(dirname $0)
  pwd
)
readonly winadb="adb.exe"
readonly macadb="adb"
readonly linuxadb="adb"
mdout="mdout"
sed="sed"

checkEnvArgs() {
  unames=$(uname -s)
  local cygwin="CYGWIN"
  local mingw="MINGW"
  local msys_nt="MSYS_NT"
  local macos="Darwin"
  local linux="Linux"
  support_printf_os=""
  if [ "$(echo $unames | grep "$cygwin")" != "" ]; then
    echo "[$filename] your platform is win . cygwin"
    red='\e[0;31m'
    green='\e[0;32m'
    yellow='\e[0;33m'
    blue='\e[0;34m'
    endColor='\e[0m'
    ecs="echo -e"
    dx="dx.bat"
    gw="./gradlew.bat"
    support_printf_os=""
    mdout="${ipwd}\\tools\\windows\\mdout.exe"
    sed="${ipwd}\\tools\\windows\\sed.exe"
    if [ -z $ANDROID_HOME ]; then
      iadb=$winadb
    else
      iadb="$ANDROID_HOME\\platform-tools\\adb.exe"
    fi
  elif [ "$(echo $unames | grep "$mingw")" != "" ]; then
    echo "[$filename] your platform is win . mingw"
    red='\033[31m'
    green='\033[32m'
    yellow='\033[33m'
    blue='\033[34m'
    endColor='\033[0m'
    ecs="echo -e"
    dx="dx.bat"
    gw="./gradlew.bat"
    support_printf_os=""
    mdout="${ipwd}\\tools\\windows\\mdout.exe"
    sed="${ipwd}\\tools\\windows\\sed.exe"
    if [ -z $ANDROID_HOME ]; then
      iadb=$winadb
    else
      iadb="$ANDROID_HOME\\platform-tools\\adb.exe"
    fi
  elif [ "$(echo $unames | grep "$msys_nt")" != "" ]; then
    echo "[$filename] your platform is win10 . mingw"
    red='\e[0;31m'
    green='\e[0;32m'
    yellow='\e[0;33m'
    blue='\e[0;34m'
    endColor='\e[0m'
    ecs="echo -e"
    dx="dx.bat"
    gw="./gradlew.bat"
    support_printf_os=""
    mdout="${ipwd}\\tools\\windows\\mdout.exe"
    sed="${ipwd}\\tools\\windows\\sed.exe"
    if [ -z $ANDROID_HOME ]; then
      iadb=$winadb
    else
      iadb="$ANDROID_HOME\\platform-tools\\adb.exe"
    fi
  elif [ "$(echo $unames | grep "$macos")" != "" ]; then
    echo "[$filename] your platform is macos"
    red='\033[31m'
    green='\033[32m'
    yellow='\033[33m'
    blue='\033[34m'
    endColor='\033[0m'
    ecs="printf"
    dx="dx"
    gw="./gradlew"
    support_printf_os="macos"
    mdout="${ipwd}/tools/macos/mdout"
    sed="${ipwd}/tools/macos/sed"
    if [ -z $ANDROID_HOME ]; then
      iadb=$macadb
    else
      iadb="$ANDROID_HOME/platform-tools/adb"
    fi
  elif [ "$(echo $unames | grep "$linux")" != "" ]; then
    echo "[$filename] your platform is $linux"
    red='\033[31m'
    green='\033[32m'
    yellow='\033[33m'
    blue='\033[34m'
    endColor='\033[0m'
    ecs="printf"
    dx="dx"
    gw="./gradlew"
    mdout="${ipwd}/tools/linux/mdout"
    sed="${ipwd}/tools/linux/sed"
    support_printf_os="$linux"
    if [ -z $ANDROID_HOME ]; then
      iadb=$linuxadb
    else
      iadb="$ANDROID_HOME/platform-tools/adb"
    fi
  else
    echo "[$filename]your platform is $unames"
    red='\033[31m'
    green='\033[32m'
    yellow='\033[33m'
    endColor='\033[0m'
    ecs="echo"
    dx="dx"
    gw="./gradlew"
    support_printf_os=""
    mdout="${ipwd}/tools/linux/mdout"
    sed="${ipwd}/tools/linux/sed"
    if [ -z $ANDROID_HOME ]; then
      iadb=$macadb
    else
      iadb="$ANDROID_HOME/platform-tools/adb"
    fi
  fi
  curtime=$(date "+%Y-%m-%d %H:%M:%S")
}

mdout_init() {
  $mdout install
}

# make sure env
makesureEnv() {
  if [ "$curtime" = "" ]; then
    checkEnvArgs
  fi
}
logd() {
  makesureEnv
  if [ "$1" ] && [ ! "$support_printf_os" ]; then
    $ecs "${blue}$1${endColor}"
  else
    $ecs "${blue}$1${endColor}\n"
  fi
}
logi() {
  makesureEnv
  if [ "$1" ] && [ ! "$support_printf_os" ]; then
    $ecs "${green}$1${endColor}"
  else
    $ecs "${green}$1${endColor}\n"
  fi
}
loge() {
  makesureEnv
  if [ "$1" ] && [ ! "$support_printf_os" ]; then
    $ecs "${red}$1${endColor}"
  else
    $ecs "${red}$1${endColor}\n"
  fi
}
logw() {
  makesureEnv
  if [ "$1" ] && [ ! "$support_printf_os" ]; then
    $ecs "${yellow}$1${endColor}"
  else
    $ecs "${yellow}$1${endColor}\n"
  fi
}
test() {
  logd "test log"
  logi "test log"
  loge "test log"
  logw "test log"
}
#main() {
#    makesureEnv
#    mdout_init
#    test
#    logi $mdout
#}
#
### call method
#main

#logd $0
#logd $1
if [ -n "$1" ]; then
  logi "has one args"
  chmod -R 777 tools/
  $mdout install
  git config core.filemode false
fi
