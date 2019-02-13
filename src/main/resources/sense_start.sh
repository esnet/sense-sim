#! /bin/bash

export HOME=.

for i in `ls $HOME/config/sense*.yaml`; do
  if [ -f $i ]; then
    path=${i%.*}
    root=${path##*/}

    nohup /usr/bin/java \
        -Xmx1024m -Djava.net.preferIPv4Stack=true  \
        -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
        -Dbasedir="$HOME" \
        -Dlogback.configurationFile="file:$path.xml" \
        -XX:+StartAttachListener \
        -jar "$HOME/rm/target/rm-0.1.0.jar" \
        --spring.config.name="$root" > /dev/null 2>&1 &
    echo $! > $root.pid
  fi
done
