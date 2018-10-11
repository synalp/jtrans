# JTrans

## Purpose

JTrans aims to bring text-to-speech alignment in a user-friendly package.
It is the only software we are aware of that can align *long*
audio files (tested on up to 2 hours of speech).

It is being developed by SYNALP, a research team at LORIA, to assist in the
semi-automatic alignment of large annotated text/speech corpora.

### About text-to-speech alignment

"Text-to-speech" alignment is a speech processing task that is related to the
well-known "speech recognition" task. However, unlike speech recognition,
text-to-speech alignment requires the text that is pronounced to be already
known. All that remains to do is to align the words onto the speech signal,
i.e., find the milliseconds at which every word starts and ends in the audio
file.

This is very useful for example to synchronize the lips animation of a cartoon
character with the recording of an actor, or to build "Karaoke"-like
applications, or yet to quickly find where an utterance occurs in a video
database.

Text-to-speech alignment is technically easier to achieve than speech
recognition, thanks to the additional information provided by the (approximate)
transcription. Therefore, this technology is more precise and more efficient
than speech recognition for many corpora where the sound quality is not good
enough and where speech recognition fails.

## Warning

As of today, JTrans:

- Only works with French texts for now, but we plan to introduce support for
  other languages in the future (particularly English).

- Is currently tailored to the conventions used in a certain set of corpora.

## License

JTrans is distributed under the
[CeCILL-C](http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html)
license.

How to cite:

    @InProceedings{cerisara09,
      author =   {Cerisara, C. and Mella, O. and Fohr, D.},
      title =    {JTrans, an open-source software for semi-automatic text-to-speech alignment},
      booktitle =    {Proc. of INTERSPEECH},
      year =     {2009},
      address =      {Brighton, UK},
      month = sep,
    }


## Building JTrans from source

JDK 7 and Apache Ant are required to build JTrans from source.

To create an executable JAR file, run:

	ant jar

This produces a file named `jtrans.jar`. You may then launch JTrans using:

	java -jar jtrans.jar


## Retrieving the dependencies

The binary dependencies are already in the git repository for your convenience,
but you can retrieve them yourself using Apache Ivy if you want.

All dependencies, **except [Sphinx4](http://cmusphinx.sf.net)**,
can be fetched by Ivy automatically.  For these two libraries, you will need to
keep the binary jars that are provided in the git repository, because they are
not available on public repositories yet.

To fetch the binary dependencies, run:

	ant ivy-retrieve-binaries

To fetch the source dependencies, which are not included in the git repository,
run:

	ant ivy-retrieve-sources


## Installing the resources

JTrans needs a resource package to function properly. The first time you run
JTrans, it will download resources and install them in `~/.jtrans`
(`%userprofile%\.jtrans` on Windows).

You may also install resources manually: download
http://talc1.loria.fr/users/cerisara/jtrans/jtrans_res_20140416.zip (about 55
MB) and unzip it in `~/.jtrans`.


## Testing JTrans

Download a small part of the TCOF corpus here:
http://talc1.loria.fr/users/cerisara/jtrans/ag_ael.zip

Unzip both .trs and .wav files somewhere, and launch JTrans either manually:

    java -jar jtrans.jar ag_ael_08.trs

or alternatively graphically, with the menu File/Open or the shortcut Ctrl-o to load the extracted .trs file.

In both cases, JTrans will find and propose to also download the corresponding .wav file, just selects "OK".

When you see both the text and the spectrogram (bottom part), you can then press the button "Align", and just wait until it's finished.
You can then press the button "Play" to check the alignment, and navigate in the transcription by clicking on a word somewhere in the text.

## Troubleshooting

### Memory error

If JTrans hangs or fails to load the .wav file, you might not have booked enough RAM.
Then, re-run JTrans with the option to allocate a larger amount of RAM, depending on your computer, for instance:

    java -Xmx2G -jar jtrans.jar

### Java version error

Please check that you indeed have java version 1.8 or later:

    java -version

### JNI error

This is probably because you use a "slim" jar file, which does not contain all .jar libraries.
You may rather want to use a "fat" jar file, with all libraries included.
This fat jtrans.jar may be compiled with the option

    ant fatjar

## Running the unit tests

Please install the resources before running the unit tests (see above).
Unit tests should be run with apache "ant test".

