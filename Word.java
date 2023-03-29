package julsvocab;

public class Word
{
    Form formOf;
    String word;
    String pos;
    String etymology_text;
    Sense[] senses;
    Form[] forms;
    // mock constructor
    Word(String word, Form formOf, String pos)
    {
        this.word = word;
        this.formOf = formOf;
        this.pos = pos;
        this.etymology_text = null;
        this.senses = null;
        this.forms = null;
    }
}