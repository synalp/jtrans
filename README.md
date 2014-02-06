# JTrans

## Purpose

JTrans aims to bring text-to-speech alignment in a user-friendly package.

It is being developed by SYNALP, a research team at LORIA, to assist in the
semi-automatic alignment of large annotated text/speech corpora.

## About text-to-speech alignment

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

As of today, JTrans is still alpha-quality software:

- It only works with French texts for now, but we plan to introduce support for
  other languages in the future (particularly English).

- It is tailored to the conventions used in a certain set of corpora.

- Over the years, JTrans has gone through many iterations brought by several
  developers. The codebase is in the process of being cleaned up, so don't be
  surprised if some parts are still "hectic".

## License

JTrans is distributed under the CeCILL-C license.

