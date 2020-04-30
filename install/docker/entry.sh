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
  ge=$(getent group $PGID | cut -d":" -f1)
  gn=${ge:-ag}
  if [ $gn == 'ag' ]; then
    # group doesn't exist, create it
    groupadd -r -g $PGID ag
  fi
  ue=$(getent passwd $PUID | cut -d":" -f1)
  un=${ue:-au}
  if [ $un == 'au' ]; then
    # user doesn't exist, create it
    useradd -r -u $PUID au
  fi
  # add user to group
  usermod -g $gn $un
  # execute as user
  exec gosu $un run.sh "$@"
fi
