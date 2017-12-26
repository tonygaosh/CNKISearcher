package gaotong.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Indexer {

    private static final String FIELD_PATTERN = "<(.*)>=(.*)";
    private static final String BEGINNER = "<REC>";

    private static Pattern pattern;
    private IndexWriter indexWriter;

    public Indexer(String indexPath) throws IOException {
        Path indexDir = Paths.get(indexPath);
        Directory dir = FSDirectory.open(indexDir);
        Analyzer smartCnAnalyzer = new SmartChineseAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(smartCnAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        indexWriter = new IndexWriter(dir, config);

        if (pattern == null)
            pattern = Pattern.compile(FIELD_PATTERN);
    }

    public int createIndex(String dataFile) throws IOException {
        File f = new File(dataFile);

        // Index docs in the file
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String parseString = "", tempString;
        Document d = new Document();
        while ((tempString = reader.readLine()) != null) {
            if (tempString.equals(BEGINNER)) {
                if (d.getFields().size() != 0) {
                    logAddFile(d);
                    indexWriter.addDocument(d);
                    d = new Document();
                }
                continue;
            }
            // Deal with multi-line fields.
            if (tempString.length() == 0)
                continue;
            if (tempString.charAt(0) == '<') {
                if (!parseString.equals(""))
                    d.add(parseField(parseString));
                parseString = tempString;
            } else {
                parseString += tempString;
            }
        }
        if (!parseString.equals(""))
            d.add(parseField(parseString));
        logAddFile(d);
        indexWriter.addDocument(d);
        reader.close();

        return indexWriter.numDocs();
    }

    public void closeWriter() throws IOException {
        indexWriter.close();
    }

    private Field parseField(String line) throws AssertionError {
        Matcher m = pattern.matcher(line);
        boolean b = m.matches();
        assert b;
        return new TextField(m.group(1), m.group(2), Field.Store.YES);
    }

    private void logAddFile(Document d) {
        System.out.println("Added file #" + indexWriter.numDocs() + " with " + d.getFields().size() + " fields.");
    }
}