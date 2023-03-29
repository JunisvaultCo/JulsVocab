# JulsVocab
*Requires Google gson and Jsoup to work*

Very much a work-in-progress. The concept is to use the parsed Wiktionary lists at https://kaikki.org/ to make a dictionary for each language that the user might desire. The current idea is to have three functionalities:
  Free mode.
  Vocabulary.
  Test mode.

Free Mode: the user can input any words they might want, paragraphs even. The app will highlight the words as such:

blue - The app knows the word, but it isn't saved in the vocabulary.

green - The app knows the word and it is saved in the vocabulary.

red - The app doesn't know the word (and won't save it in the vocabulary).

The user can move the caret to a certain word and it will show them what the app has about that word: what declension, what etymology, buttons to related words and forms. This is the strong-suite of this dictionary. The concept would be that one could easily see the declension and other grammatical properties of the word, therefore aiding the learning process.

Vocabulary: A scrollable list of the words already known by the dictionary

Test mode: A mode where the user can take random words from the vocabulary and test whether they still know them. Possible additions: graphs and other data visualisation for the tests. 

Currently, only Vocabulary and Free Mode are implemented (and are quite buggy).
This app uses SQLite for quick searches through the myriad of words.
