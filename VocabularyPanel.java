/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package julsvocab;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.sql.*;
import javax.sql.*;
import java.util.*;
import javax.swing.text.*;

/**
 *
 * @author Jul
 */
public class VocabularyPanel extends JPanel
{
    JScrollPane JSP1;
    JScrollPane JSP2;
    JTextPane JTP;
    JPanel left;
    java.awt.List list;
    JPanel averageChillPanel;
    LanguageFrame original;
    JTextField JTF;
    VocabularyPanel(LanguageFrame LF)
    {
        original = LF;
        setLayout(new BorderLayout());
        averageChillPanel = new JPanel();
        averageChillPanel.setLayout(new BoxLayout(averageChillPanel, BoxLayout.X_AXIS));
        left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        list = new java.awt.List();
        list.addActionListener(
                (e)->
                {
                    Document doc = JTP.getStyledDocument();
                    String find = list.getSelectedItem();
                    LinkedList<String> rs = original.getWord(find);
                    try
                    {
                        doc.remove(0, doc.getLength());
                        if (rs != null) {
                            doc.insertString(doc.getLength(), "word: " + find + "\n", original.black);
                            for (String r : rs)
                                original.writeInDoc(r, doc, JTP);
                        } else {
                            doc.insertString(0, "Couldn't find word: " + find + "\n", original.red);
                        }
                    }
                    catch (Exception ex)
                    {
                        System.out.println("Shouldn't get here");
                        ex.printStackTrace();
                    }
                }
        );
        //TODO: Category list

        JButton backToMenu = new JButton("<< Back to menu");
        backToMenu.addActionListener(
            (e)->
            {
                LF.cl.show(LF.getContentPane(), "mainMenu");
            }
        );
        JTF = new JTextField();
        JTF.addActionListener(
                (e)->
                {
                    updateList();
                }
        );
        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent evt)
            {
                Component src = (Component)evt.getSource();
                int width = src.getWidth();
                left.setMaximumSize(new Dimension(width / 2, src.getHeight()));
                JSP2.setMaximumSize(new Dimension(width / 2, src.getHeight()));
                left.setMinimumSize(new Dimension(width / 3, src.getHeight() / 2));
                JSP2.setMinimumSize(new Dimension(width / 3, src.getHeight() / 2));
                left.setSize(new Dimension(width / 3, src.getHeight()));
                JSP2.setSize(new Dimension(width / 3, src.getHeight()));
                JSP1.setMaximumSize(new Dimension(width, src.getHeight()));
                JSP1.setMinimumSize(new Dimension(width / 3, src.getHeight() / 2));
                JSP1.setSize(new Dimension(width / 3, src.getHeight()));
                backToMenu.setMaximumSize(new Dimension(width / 2, 30));
                backToMenu.setMinimumSize(new Dimension(width / 3, 30));
                JTF.setMaximumSize(new Dimension(width / 2, 30));
                JTF.setMinimumSize(new Dimension(width / 3, 30));
            }
        });
        JSP1 = new JScrollPane(list);
        JTP = new JTextPane();
        JSP2 = new JScrollPane(JTP);
        JTP.setEditable(false);
        left.add(backToMenu);
        left.add(JTF);
        left.add(JSP1);
        averageChillPanel.add(left);
        averageChillPanel.add(JSP2);
        add(averageChillPanel, BorderLayout.CENTER);
        updateList();
    }
    void updateList()
    {
        list.removeAll();
        try
        {
            ArrayList<String> strings = new ArrayList<>();
            String text = JTF.getText();
            String searchTerm = LanguageFrame.encodeWord(text);
            while (searchTerm.endsWith("="))
                searchTerm = searchTerm.substring(0, searchTerm.length() - 1);
            if (searchTerm.length() > 0)
                searchTerm = searchTerm.substring(0, searchTerm.length() - 1);
            if (searchTerm.length() > 0)
                searchTerm = searchTerm.substring(0, searchTerm.length() - 1);
            Statement stmt = original.sqlConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Vocabulary_" + original.languageName +
                    " WHERE Word LIKE '" + searchTerm + "%';");
            while (rs.next())
            {
                String term = LanguageFrame.decodeWord(rs.getString("Word"));
                if (term.startsWith(text))
                    strings.add(LanguageFrame.decodeWord(rs.getString("Word")));
            }
            strings.sort(String.CASE_INSENSITIVE_ORDER);
            for (String s: strings)
                list.add(s);
        }
        catch (SQLException sqle)
        {
            sqle.printStackTrace();
        }
    }
}
