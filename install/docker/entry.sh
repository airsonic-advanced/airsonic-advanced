#!/bin/bash

set -e

ue=$(id -u)
echo "Docker USER id: $ue"
echo "Docker PUID env: $PUID"
ge=$(id -g)
echo "Docker USER group: $ge"
echo "Docker PGID env: $PGID"
if [ $ue != '0' ] || [ $ge != '0' ]; then
  # specified from USER directive, run as is
  run.sh "$@"
else
  # No USER specified, guaranteed to be root
  gn=$(getent group $PGID | cut -d":" -f1)
  if [ -z $gn ]; then
    # group doesn't exist, create it
    gn=ag
    groupadd -r -g $PGID $gn
  fi
  un=$(getent passwd $PUID | cut -d":" -f1)
  if [ -z $un ]; then
    # user doesn't exist, create it
    un=au
    useradd -r -u $PUID $un
  fi
  # add user to group
  usermod -g $gn $un
  # execute as user
  exec gosu $un run.sh "$@"
fi
