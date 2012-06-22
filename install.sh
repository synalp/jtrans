#!/bin/bash

jar cvf jtransres.zip res libs ressources *.txt culture.wav
scp jtransres.zip talc1:/var/www/users/cerisara/jtrans/
scp jtrans.jar talc1:/var/www/users/cerisara/jtrans/
scp docs/index.html talc1:/var/www/users/cerisara/jtrans/

# zip -r -9 jtransres.zip res/ libs/ ressources/ *.txt culture.wav

