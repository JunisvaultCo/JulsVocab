/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package julsvocab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import com.google.gson.*;

import java.sql.*;

/**
 *
 * @author Jul
 */

public class JulsVocab extends JFrame
{
    JTextField progressField;
    JComboBox languageSelecter;
    JPanel savedLanguages;
    ArrayList<JButton> languageButtons;
    ArrayList<String> savedLanguagesStrings;
    ArrayList<String> availableLanguages;
    JScrollPane savedLanguagesSP;
    JButton updateLanguagesButton;
    static final String FOLDER_NAME = "JulsVocab";
    static java.sql.Connection sqlConnection;
    boolean downloading = false;
    int totalDownloads = 0;
    int currentDownloaded = 0;
    Queue<QueueElement>[] threadQueues;
    
    void readAvailableLanguages() throws Exception
    {
        //check if the required databases are created
        Statement stmt = sqlConnection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Languages WHERE 1");
        while (rs.next())
        {
            String name = rs.getString("Language");
            if (!savedLanguagesStrings.contains(name))
            {
                JButton jb = new JButton(name);
                jb.addActionListener(
                (e)->
                {
                   LanguageFrame LF = new LanguageFrame(name, sqlConnection);
                   LF.setVisible(true);
                });
                languageButtons.add(jb);
                savedLanguagesStrings.add(name);
                savedLanguages.add(jb);
            }
        }
        for (JButton jb : languageButtons)
        {
            jb.setMinimumSize(new Dimension(getWidth(), 30));
            jb.setMaximumSize(new Dimension(getWidth(), 30));
            jb.setSize(new Dimension(getWidth(), 30));
        }
        pack();
    }
    
    //https://kaikki.org/dictionary/ each <li> from the second <li>
    //we're interested in the a href before /
    //example Old Norse/index.html we're interested in Old Norse
    void initialiseLanguages() throws Exception
    {
        languageButtons = new ArrayList<>();
        availableLanguages = new ArrayList<>();
        savedLanguagesStrings = new ArrayList<>();
        
        readAvailableLanguages();
        //parsing the list of languages from kaikki
      /*  Document doc = Jsoup.connect("https://kaikki.org/dictionary/").get();
        Elements languages = doc.select("li");
        for (Element language : languages)
        {
            StringBuilder name = new StringBuilder();
            if (language.getElementsByTag("a") == null
             || language.getElementsByTag("a").first() == null
             || language.getElementsByTag("a").first().attr("href") == null)
                continue;
            name.append(language.getElementsByTag("a").first().attr("href").split("/")[0]);
            if (name.toString().trim().equals(".."))
                continue;
            if (name.toString().trim().equals("All languages combined"))
                continue;
            availableLanguages.add(name.toString());
            availableLanguages.add(name.toString());
        }
        availableLanguages.sort(String.CASE_INSENSITIVE_ORDER);
        for (String i: availableLanguages)
            languageSelecter.addItem(i);*/
    }
    
    void updateWords(int id)
    {
        try
        {
            Statement stmt = sqlConnection.createStatement();
            while (downloading || !threadQueues[id].isEmpty())
            {
                try
                {
                    if (threadQueues[id].isEmpty() || threadQueues[id].peek() == null
                            || threadQueues[id].peek().json == null
                            || threadQueues[id].peek().language == null)
                    {
                        Thread.sleep(100);
                        continue;
                    }
                }
                catch (Exception e)
                {
                    System.out.println("HAHAHAHAHA CAN'T SLEEP");
                    continue;
                }
                QueueElement qe = threadQueues[id].poll();
                String line = qe.json;
                String s = qe.language;
                Base64.Encoder B64E = Base64.getEncoder();
                Gson gson = new Gson();
                Word a = gson.fromJson(line, Word.class);
                String name = B64E.encodeToString(a.word.toLowerCase().getBytes());
                
                StringBuilder commands = new StringBuilder();
                commands.append("INSERT INTO ");
                commands.append(s);
                commands.append(" VALUES");
                if (a.forms != null)
                {
                    for (Form f: a.forms)
                    {
                        if (f.tags[0].equals("canonical") || f.tags[0].equals("word-tags") || f.form.trim().equals("-"))
                            continue;
                        Form f2 = new Form(a.word, f.tags);
                        Word w = new Word(f.form, f2, a.pos);
                        String jsonLine = gson.toJson(w);
                        String encodedLine = B64E.encodeToString(jsonLine.getBytes());
                        String name2 = B64E.encodeToString(f.form.toLowerCase().getBytes());
                        commands.append("('");
                        commands.append(name2);
                        commands.append("', '");
                        commands.append(encodedLine);
                        commands.append("'),");
                    }
                }
                String encodedLine = B64E.encodeToString(line.getBytes());
                commands.append("('");
                commands.append(name);
                commands.append("', '");
                commands.append(encodedLine);
                commands.append("');");
                stmt.executeUpdate(commands.toString());
                currentDownloaded++;
                if (!downloading && currentDownloaded != totalDownloads)
                    progressField.setText("Updated forms " + currentDownloaded + " / " + totalDownloads);
                else if (!downloading)
                    progressField.setText("Done with " + s + "!");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    void getPage(String jsonPage, String s, String size) throws Exception
    {
        Statement stmt = sqlConnection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Languages WHERE Language='" + s + "';");
        if (!rs.next())
        {
            stmt.executeUpdate("INSERT INTO Languages VALUES (0, '" + s + "');");
            stmt.executeUpdate("CREATE TABLE " + s + " (Word varchar(255), JSON varchar(10000));");
            stmt.executeUpdate("CREATE INDEX " + s + "_index ON " + s + "(Word);");
        }
        //TODO: instead ask the user if they really want to redownload all of it.
        else
            stmt.executeUpdate("DELETE FROM " + s + ";");
        stmt.close();
        URL url = new URL(jsonPage);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        int currentSize = 0;
        String line = br.readLine();
        for (int i = 0; i < threadQueues.length; i++)
        {
            final int a = i;
            Thread t = new Thread(()->
            {
                updateWords(a);
            });
            t.start();
        }
        while (line != null)
        {
            currentSize += line.length();
            totalDownloads++;
            Queue minn = threadQueues[0];
            for (Queue q: threadQueues)
                if (minn.size() > q.size())
                    minn = q;
            minn.add(new QueueElement(line, s));
            line = br.readLine();
            String sizeString = " bytes";
            double sizeD = currentSize;
            if (sizeD > 1024)
            {
                sizeD = sizeD / 1024;
                sizeString = "KB";
            }
            if (sizeD > 1024)
            {
                sizeD = sizeD / 1024;
                sizeString = "MB";
            }
            if (sizeD > 1024)
            {
                sizeD = sizeD / 1024;
                sizeString = "GB";
            }
            String doubleString = String.format("%.1f", sizeD);
            sizeString = doubleString + sizeString;
            progressField.setText("Downloaded: " + sizeString + " / " + size);
        }
        progressField.setText("Updated " + s + ".");
        downloading = false;
        readAvailableLanguages();
    }
    
    void download(String s) throws Exception
    {
        if (downloading) return;
        downloading = true;
        String base = "https://kaikki.org/dictionary/" + s;
        // we look on the https://kaikki.org/dictionary/s/index.html page
        //for the download link (the element with tagname a whose innerHTML
        //is Download
        Document doc = Jsoup.connect(base + "/index.html").get();
        Elements links = doc.select("a");
        String jsonPage = null;
        String size = null;
        for (Element link : links)
        {
            if (link.text().trim().equals("Download"))
            {
                String href = link.attr("href");
                jsonPage = base + "/" + href;
                java.util.List<Node> nodes = link.siblingNodes();
                Node node = nodes.get(nodes.size() - 1);
                size = node.toString().split("[()]")[1];
                break;
            }
        }
        if (jsonPage == null)
            throw new Exception("Couldn't get page download link");
        final String jsonPage_f = jsonPage;
        final String s_f = s;
        final String size_f = size;
        Thread thread = new Thread(
            ()->
            {
                try
                {
                    getPage(jsonPage_f, s_f, size_f);
                    downloading = false;
                }
                catch (Exception e)
                {
                    progressField.setText("Couldn't download json file");
                    downloading = false;
                    e.printStackTrace();
                }
            });
       thread.start();
    }
    
    JulsVocab()
    {
        super("Jul's Vocab");
        threadQueues = new Queue[8];
        for (int i = 0; i < 8; i++)
            threadQueues[i] = new ArrayDeque<>();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JPanel selecterPane = new JPanel();
        progressField = new JTextField();
        progressField.setEnabled(false);
        progressField.setMaximumSize(new Dimension(2500, 50));
        progressField.setBackground(Color.LIGHT_GRAY);
        progressField.setForeground(Color.red);
        progressField.setDisabledTextColor(Color.red);
        languageSelecter = new JComboBox();
        languageSelecter.setMaximumSize(new Dimension(2500, 50));
        selecterPane.add(new JLabel("Select new language: "));
        selecterPane.add(languageSelecter);

        savedLanguages = new JPanel();
        savedLanguages.setLayout(new BoxLayout(savedLanguages, BoxLayout.Y_AXIS));
        savedLanguagesSP = new JScrollPane(savedLanguages);
        savedLanguagesSP.getVerticalScrollBar().setUnitIncrement(10);
        savedLanguagesSP.setPreferredSize(new Dimension(200, 200));
        
        updateLanguagesButton = new JButton("Update database");
        updateLanguagesButton.addActionListener(
                (e)->
                {
                    try
                    {
                        download((String)languageSelecter.getSelectedItem());
                    }
                    catch (IOException ioe)
                    {
                        downloading = false;
                        progressField.setText("Couldn't write file!");
                        ioe.printStackTrace();
                    }
                    catch (Exception ex)
                    {
                        downloading = false;
                        progressField.setText("Couldn't download file!");
                        ex.printStackTrace();
                    }
                }
        );
        
        add(savedLanguagesSP);
        add(selecterPane);
        add(updateLanguagesButton);
        add(progressField);
        
        try
        {
            addComponentListener(new ComponentAdapter()
            {
                public void componentResized(ComponentEvent evt)
                {
                    try
                    {
                        readAvailableLanguages();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            initialiseLanguages();
            pack();
            setMinimumSize(new Dimension(300, 0));
            setResizable(false);
            setLocationRelativeTo(null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }
    private static void GUI()
    {
        JulsVocab JV = new JulsVocab();
        JV.setVisible(true);
    }
    public static void main(String[] args)
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            sqlConnection = DriverManager.getConnection("jdbc:sqlite:JulsVocab.db");
            //check if the required databases are created
            Statement stmt = sqlConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_schema WHERE type ='table' AND name NOT LIKE 'sqlite_%'");
            if (!rs.next())
                stmt.executeUpdate("CREATE TABLE Languages (ID int, Language varchar(255));");
            long a = System.currentTimeMillis();
        }
        catch (Exception e)
        {
           System.err.println( e.getClass().getName() + ": " + e.getMessage() );
           System.exit(0);
        }
        SwingUtilities.invokeLater(
                ()->
                {
                    GUI();
                }
        );
    }
    
}
