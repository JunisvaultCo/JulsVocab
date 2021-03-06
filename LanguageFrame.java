/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package julsvocab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;

import java.sql.*;
import java.util.*;
import com.google.gson.*;

/**
 *
 * @author Jul
 */
public class LanguageFrame extends JFrame
{
    String languageName;
    java.sql.Connection sqlConnection;
    CardLayout cl;
    JButton testsButton;
    JButton vocabButton;
    JButton transButton;
    ArrayList<String> words;
    SimpleAttributeSet red = new SimpleAttributeSet();
    SimpleAttributeSet green = new SimpleAttributeSet();
    SimpleAttributeSet black = new SimpleAttributeSet();
    SimpleAttributeSet blue = new SimpleAttributeSet();
    
    void writeInDoc(String json, Document doc, JTextPane JTP)
    {
        try
        {
            Gson gson = new Gson();
            Word w = gson.fromJson(json, Word.class);
            StyleConstants.setForeground(red, Color.red);
            StyleConstants.setForeground(green, Color.green);
            StyleConstants.setForeground(black, Color.black);
            StyleConstants.setForeground(blue, Color.blue);
            doc.insertString(doc.getLength(), "Part of speech: " + w.pos + "\n\n", black);
            if (w.formOf != null)
            {
                for (String tag: w.formOf.tags)
                    doc.insertString(doc.getLength(), tag + " ", blue);
                Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
                Style s = JTP.addStyle("button", def);
                StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
                JButton button = new JButton(w.formOf.form);
                StyleConstants.setComponent(s, button);
                button.addActionListener(
                (e)->
                {
                    try
                    {
                        ResultSet rs = getWord(w.formOf.form);
                        doc.remove(0, doc.getLength());
                        while (rs != null && rs.next())
                        {
                            doc.insertString(doc.getLength(), "word: " + w.formOf.form + "\n", black);
                            String result = new String(decodeWord(rs.getString("JSON")));
                            writeInDoc(result, doc, JTP);
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                });
                doc.insertString(doc.getLength(), " ", JTP.getStyle("button"));
                doc.insertString(doc.getLength(), "\n", black);
            }
            if (w.etymology_text != null && w.etymology_text.trim().length() != 0)
                doc.insertString(doc.getLength(), "Etymology:\n" + w.etymology_text + "\n\n", black);
            if (w.senses != null)
            {
                doc.insertString(doc.getLength(), "Raw glosses:\n", black);
                for (Sense s: w.senses)
                    if (s.raw_glosses != null)
                        for (String gloss: s.raw_glosses)
                            doc.insertString(doc.getLength(), gloss + "\n", black);
                doc.insertString(doc.getLength(), "\n", black);
            }
            if (w.forms != null)
            {
                doc.insertString(doc.getLength(), "Forms:\n", black);
                for (Form f: w.forms)
                {
                    for (String tag: f.tags)
                        doc.insertString(doc.getLength(), tag + " ", blue);
                    doc.insertString(doc.getLength(), f.form + "\n", black);
                }
            }
            JTP.setCaretPosition(0);
        }
        catch (BadLocationException e)
        {
            System.out.println("Once again, technically, we should never get here."
                    + " Unless I really messed up I guess.");
            e.printStackTrace();
        }
    }
    //just thinking that in the future I might have to change the encoding / decoding
    //and this would ease my pain between the switches. Plus, it's an annoying
    //section of steps to follow
    static String encodeWord(String word)
    {
        Base64.Encoder EN = Base64.getEncoder();
        return EN.encodeToString(word.toLowerCase().getBytes());
    }
    
    static String decodeWord(String word)
    {
        Base64.Decoder DE = Base64.getDecoder();
        return new String(DE.decode(word.getBytes()));
    }
    
    ResultSet getWord(String word)
    {
        try
        {
            String s = encodeWord(word);
            s = "SELECT * FROM " + languageName + " WHERE Word='" + s + "';";
            Statement stmt = sqlConnection.createStatement();
            ResultSet rs = stmt.executeQuery(s);
            return rs;
        }
        catch (SQLException e)
        {
            return null;
        }
    }
    
    ResultSet getVocabularyWord(String word)
    {
        try
        {
            ResultSet form = getWord(word);
            StringBuilder sb = new StringBuilder("SELECT * FROM Vocabulary_");
            sb.append(languageName);
            sb.append(" WHERE Word='");
            sb.append(encodeWord(word));
            sb.append("'");
            while (form != null && form.next())
            {
                Gson gson = new Gson();
                Base64.Decoder DE = Base64.getDecoder();
                Word w = gson.fromJson(new String(DE.decode(form.getString("JSON"))), Word.class);
                if (w.formOf != null)
                {
                    Form f = w.formOf;
                    sb.append(" OR Word='");
                    sb.append(encodeWord(f.form));
                    sb.append("'");
                }
            }
            sb.append(";");
            Statement stmt = sqlConnection.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            return rs;
        }
        catch (SQLException e)
        {
            return null;
        }
    }
    
    void resizeButtons()
    {
        testsButton.setMinimumSize(new Dimension(getWidth(), 30));
        vocabButton.setMinimumSize(new Dimension(getWidth(), 30));
        transButton.setMinimumSize(new Dimension(getWidth(), 30));
        testsButton.setSize(new Dimension(getWidth(), 30));
        vocabButton.setSize(new Dimension(getWidth(), 30));
        transButton.setSize(new Dimension(getWidth(), 30));
        testsButton.setMaximumSize(new Dimension(getWidth(), 30));
        vocabButton.setMaximumSize(new Dimension(getWidth(), 30));
        transButton.setMaximumSize(new Dimension(getWidth(), 30));
    }
    
    LanguageFrame(String languageName, java.sql.Connection sqlConnection)
    {
        super(languageName);
        this.languageName = languageName;
        this.sqlConnection = sqlConnection;
        cl = new CardLayout();
        
        //main menu
        JPanel mainMenu = new JPanel();
        mainMenu.setLayout(new BoxLayout(mainMenu, BoxLayout.Y_AXIS));
        testsButton = new JButton("Tests");
        vocabButton = new JButton("Vocabulary notebook");
        transButton = new JButton("Quick translate");
        mainMenu.add(testsButton);
        mainMenu.add(vocabButton);
        mainMenu.add(transButton);
        
        getContentPane().setLayout(cl);
        getContentPane().add("mainMenu", mainMenu);
        
        //TODO: Tests
        
        VocabularyPanel VP = new VocabularyPanel(this);
        getContentPane().add("Vocabulary", VP);
        vocabButton.addActionListener(
                (e)->
                {
                    cl.show(getContentPane(), "Vocabulary");
                }
        );
        TranslatePanel TP = new TranslatePanel(this);
        getContentPane().add("Quick translate", TP);

        transButton.addActionListener(
                (e)->
                {
                    cl.show(getContentPane(), "Quick translate");
                }
        );
        
        mainMenu.addComponentListener(new ComponentAdapter()
        {
            public void componentShown(ComponentEvent evt)
            {
                resizeButtons();
            }
            public void componentResized(ComponentEvent evt)
            {
                resizeButtons();
            }
        });
        cl.show(getContentPane(), "mainMenu");
        setMinimumSize(new Dimension(500, 500));
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }
}
