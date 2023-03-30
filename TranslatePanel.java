/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package julsvocab;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.sql.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import com.google.gson.*;

/**
 *
 * @author Jul
 */
public class TranslatePanel extends JPanel implements CaretListener
{
    JTextPane definitionPane;
    JTextPane userPane;
    LanguageFrame original;
    boolean updatingText = false;
    final String delims = " ,.;!?/()[]{}\n'\"-";
    final String delimRegex = "[ ,.;!?\\/()\\[\\]{}\n'\"-]+";
    TranslatePanel(LanguageFrame original)
    {
        super();
        this.original = original;
        setLayout(new BorderLayout());
        JButton back = new JButton("<<");
        JButton addToVocab = new JButton("Add to vocabulary");
        //just the two buttons below
        JPanel lowPanel = new JPanel(new BorderLayout());
        JLabel jl = new JLabel();
        lowPanel.add(back, BorderLayout.WEST);
        lowPanel.add(jl, BorderLayout.SOUTH);
        back.addActionListener(
            (e)->
            {
                original.cl.show(original.getContentPane(), "mainMenu");
            }
        );
        addToVocab.addActionListener(
                (e)->
                {
                    try
                    {
                        jl.setText("Updating vocabulary...");
                        updateVocabulary();
                        jl.setText("Vocabulary updated!");
                    }
                    catch (SQLException exx)
                    {
                        exx.printStackTrace();
                    }
                }
        );
        lowPanel.add(addToVocab, BorderLayout.EAST);
        add(lowPanel, BorderLayout.SOUTH);

        JPanel upPanel = new JPanel();
        userPane = new JTextPane();
        userPane.addCaretListener(this);
        JScrollPane JSP1 = new JScrollPane(userPane);
        definitionPane = new JTextPane();
        definitionPane.setEditable(false);
        JScrollPane JSP2 = new JScrollPane(definitionPane);
        upPanel.setLayout(new BoxLayout(upPanel, BoxLayout.X_AXIS));
        upPanel.add(JSP1);
        upPanel.add(JSP2);
        add(upPanel, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent evt)
            {
                Component src = (Component)evt.getSource();
                JSP1.setMaximumSize(new Dimension(src.getWidth() / 2, src.getHeight()));
                JSP2.setMaximumSize(new Dimension(src.getWidth() / 2, src.getHeight()));
                JSP1.setMinimumSize(new Dimension(src.getWidth() / 3, src.getHeight() / 2));
                JSP2.setMinimumSize(new Dimension(src.getWidth() / 3, src.getHeight() / 2));
                JSP1.setSize(new Dimension(src.getWidth() / 3, src.getHeight()));
                JSP2.setSize(new Dimension(src.getWidth() / 3, src.getHeight()));
            }
        });
    }
    @Override
    public void caretUpdate(CaretEvent CE)
    {
        if (updatingText) return;
        if (CE.getDot() != CE.getMark()) return;
        updatingText = true;
        Document doc = userPane.getDocument();
        SwingUtilities.invokeLater(
            ()->
            {
                try
                {
                    String text = doc.getText(0, doc.getLength());
                    java.sql.Connection sqlConnection = original.sqlConnection;
                    Statement stmt = sqlConnection.createStatement();
                    int oldCaret = CE.getDot();
                    doc.remove(0, doc.getLength());
                    StringBuilder currentString = new StringBuilder();
                    boolean lastDelim = true;
                    for (int i = 0; i <= text.length(); i++)
                    {
                        //one more run, hence the i == text.length()
                        if (i == text.length() || delims.contains(text.charAt(i) + ""))
                        {
                            if (lastDelim && i != text.length())
                                currentString.append(text.charAt(i));
                            else if (lastDelim)
                                doc.insertString(doc.getLength(), currentString.toString(), original.black);
                            else
                            {
                                if (currentString.length() != 0)
                                {
                                    LinkedList<String> rs = original.getWord(currentString.toString());
                                    boolean isInVocab = original.getVocabularyWord(currentString.toString());
                                    if (isInVocab)
                                        doc.insertString(doc.getLength(), currentString.toString(), original.green);
                                    else if (rs != null)
                                        doc.insertString(doc.getLength(), currentString.toString(), original.blue);
                                    else
                                        doc.insertString(doc.getLength(), currentString.toString(), original.red);
                                }
                                if (i != text.length())
                                {
                                    lastDelim = true;
                                    currentString = new StringBuilder();
                                    currentString.append(text.charAt(i));
                                }
                            }
                        }
                        else
                        {
                            if (!lastDelim)
                                currentString.append(text.charAt(i));
                            else
                            {
                                doc.insertString(doc.getLength(), currentString.toString(), original.black);
                                lastDelim = false;
                                currentString = new StringBuilder();
                                currentString.append(text.charAt(i));
                            }
                        }
                    }
                    userPane.setCaretPosition(oldCaret);
                    updatingText = false;
                    updateDefinition(oldCaret);
                }
                catch (Exception e)
                {
                    updatingText = false;
                    System.out.println("Technically, we should never get here."
                            + " Unless I really messed up I guess.");
                    e.printStackTrace();
                }
            });
    }

    void updateDefinition(int position)
    {
        Document doc1 = userPane.getDocument();
        Document doc2 = definitionPane.getDocument();
        try
        {
            String text = doc1.getText(0, doc1.getLength());
            int smallest = text.length();
            for (int i = 0; i < delims.length(); i++)
            {
                char c = delims.charAt(i);
                int pos = text.indexOf(c, position);
                if (pos != -1)
                    smallest = Math.min(smallest, pos);
            }
            int biggest = 0;
            for (int i = 0; i < delims.length(); i++)
            {
                char c = delims.charAt(i);
                int pos = text.lastIndexOf(c, position);
                if (pos != -1)
                    biggest = Math.max(biggest, pos + 1);
            }
            if (smallest == -1) return;
            String find = text.substring(biggest, smallest).toLowerCase().trim();
            LinkedList<String> rs = original.getWord(find);
            doc2.remove(0, doc2.getLength());
            if (rs != null) {
                doc2.insertString(doc2.getLength(), "word: " + find + "\n", original.black);
                for (String r : rs)
                    original.writeInDoc(r, doc2, definitionPane);
                if (rs.size() == LanguageFrame.MAX_ENTRIES_BEFORE_CUTOFF) {
                    doc2.insertString(doc2.getLength(),"The amount of entries is over " + LanguageFrame.MAX_ENTRIES_BEFORE_CUTOFF +
                            " and was therefore cut off.\n", original.red);
                }
            } else {
                doc2.insertString(0, "Couldn't find word: " + find + "\n", original.red);
            }
        }
        catch (Exception e)
        {
            System.out.println("Once again, technically, we should never get here."
                    + " Unless I really messed up I guess.");
            e.printStackTrace();
        }
    }
    void updateVocabulary() throws SQLException
    {
        String s = "Vocabulary_" + original.languageName;
        try
        {
            Statement stmt = original.sqlConnection.createStatement();
            ResultSet rs = stmt.executeQuery("Select * from " + s + ";");
        }
        catch (SQLException ex)
        {
            Statement stmt = original.sqlConnection.createStatement();
            stmt.executeUpdate("CREATE TABLE " + s + " (Word varchar(255) UNIQUE);");
            stmt.executeUpdate("CREATE INDEX " + s + "_index ON " + s + "(Word);");
        }
        finally
        {
            try
            {
                Document doc = userPane.getDocument();
                String[] words = doc.getText(0, doc.getLength()).split(delimRegex);
                for (String word : words)
                {
                    if (original.isInVocab.getOrDefault(word, false))
                        continue;
                    LinkedList<String> rs = original.getWord(word);
                    boolean ok = false;
                    ArrayList<String> formsOf = new ArrayList<>();
                    if (rs != null) {
                        for (String r : rs) {
                            ok = true;
                            Gson gson = new Gson();
                            Word w = gson.fromJson(r, Word.class);
                            if (w.formOf != null)
                                formsOf.add(w.formOf.form);
                        }
                    }
                    if (!ok) continue;
                    original.isInVocab.put(word, true);
                    if (formsOf.isEmpty())
                        formsOf.add(word);
                    for (String form : formsOf)
                    {
                        try
                        {
                            Statement stmt = original.sqlConnection.createStatement();
                            String B64 = LanguageFrame.encodeWord(form);
                            stmt.executeUpdate("INSERT INTO " + s + " VALUES ('" + B64 + "');");
                        }
                        catch (SQLException e)
                        {
                            //just ignore it
                        }
                    }
                }
            }
            catch (BadLocationException e)
            {
                System.out.println("Impossible...");
                e.printStackTrace();
            }
        }
    }
}
