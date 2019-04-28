This directory will host a desktop service that will listen to a port, waiting for client connection.
Each client (in particular small-speech in github) will send the server a batch of audio files,
and the service will use JTrans to recognize these files, following a predefined recognition grammar.

