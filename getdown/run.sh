#!/bin/tcsh

cp -r ../libs jtrans/
cp ../../../softs/getdown-client-1.2.jar .

# java -cp /home/xtof/softs/getdown-tools-1.2.jar com.threerings.getdown.tools.Digester jtrans
pushd .
cd ../
ant digest
popd

scp -r jtrans cerisara@talc1:/var/www/users/cerisara/jtrans/getdown/
scp getdown-client-1.2.jar cerisara@talc1:/var/www/users/cerisara/jtrans/getdown/

