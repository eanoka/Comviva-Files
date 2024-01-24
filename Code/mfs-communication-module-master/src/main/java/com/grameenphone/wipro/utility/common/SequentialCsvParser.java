package com.grameenphone.wipro.utility.common;

import com.grameenphone.wipro.exception.TaggedCheckedException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SequentialCsvParser implements Iterator<String[]>, Closeable {
    private BufferedReader reader;
    private boolean eolReached = false;
    private boolean eofReached = false;

    private long readCount;
    private int lastReadChar;
    private int carryChar;
    private long fileSize;

    public SequentialCsvParser(String file) {
        fileSize = file.length();
        reader = new BufferedReader(new StringReader(file));
    }

    private void nextLine() throws IOException {
        if(eofReached) {
            throw new EOF();
        }
        if(!eolReached) {
            skipUpTo('\r', '\n');
        }
        if(lastReadChar == '\r') {
            int nextChar = read();
            if(nextChar != '\n') {
                carryChar = nextChar;
            }
        }
        eolReached = false;
    }

    private String getNextCell() throws IOException {
        if(eolReached || eofReached) {
            return null;
        }
        char firstChar;
        try {
            firstChar = (char)read();
        } catch(EOF e) {
            return null;
        }
        if(firstChar == ',') {
            return "";
        }
        if(firstChar == '\n' || firstChar == '\r') {
            eolReached = true;
            return "";
        }
        StringBuffer cellValue = new StringBuffer();
        char[] matchedChar = new char[] {0};

        //parsing quoted column
        if(firstChar == '"') {
            while(true) {
                cellValue.append(captureUpTo(matchedChar, '"'));
                if(matchedChar[0] == -1) {
                    eofReached = true;
                    return cellValue.toString();
                }
                char nextChar;
                try {
                    nextChar = (char)read();
                } catch (EOF e) {
                    return cellValue.toString();
                }
                if (nextChar == ',') {
                    return cellValue.toString();
                }
                if (nextChar == '\r' || nextChar == '\n') {
                    eolReached = true;
                    return cellValue.toString();
                }
                cellValue.append(nextChar);
                if (nextChar != '"') {
                    break;
                }
            }
        }

        //parsing non quoted column
        cellValue.append(firstChar);
        cellValue.append(captureUpTo(matchedChar, ',', '\r', '\n'));
        if(matchedChar[0] == -1) {
            eofReached = true;
        }
        if(matchedChar[0] == '\r' || matchedChar[0] == '\n') {
            eolReached = true;
        }
        return cellValue.toString();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    static class EOF extends Error {
        public EOF() {
        }
    }

    private boolean isMatch(char toCheck, char[] toMatch) {
        for(char _toMatch : toMatch) {
            if(_toMatch == toCheck) {
                return true;
            }
        }
        return false;
    }

    private String captureUpTo(char[] outMatchedChar, char... lookup) throws IOException {
        StringBuilder builder = new StringBuilder();
        int c;
        while (true) {
            try {
                c = read();
            } catch (EOF e) {
                outMatchedChar[0] = (char)-1;
                return builder.toString();
            }
            if (isMatch((char)c, lookup)) {
                outMatchedChar[0] = (char)c;
                return builder.toString();
            }
            builder.append((char) c);
        }
    }

    private char skipUpTo(char... lookup) throws IOException {
        int c;
        while (true) {
            c = read();
            if (isMatch((char)c, lookup)) {
                return (char)c;
            }
        }
    }

    private int read() throws IOException {
        if(carryChar != 0) {
            int _carry = carryChar;
            carryChar = 0;
            return _carry;
        }
        int c = lastReadChar = reader.read();
        if (c == -1) {
            eofReached = true;
            throw new EOF();
        }
        readCount++;
        return c;
    }

    public Stream<String[]> toLineStream() {
        Spliterator<String[]> spliterator = Spliterators.spliteratorUnknownSize(this, 0);
        return StreamSupport.stream(spliterator, false);
    }

    @Override
    public boolean hasNext() {
        return !eofReached && readCount != fileSize;
    }

    @Override
    public String[] next() {
        synchronized (this) {
            if (!hasNext()) {
                return null;
            }
            LinkedList<String> cellList = new LinkedList<>();
            String cell;
            try {
                while (true) {
                    cell = getNextCell();
                    if (cell == null) {
                        break;
                    }
                    cellList.add(cell);
                }
                try {
                    nextLine();
                } catch (EOF g) {
                }
                if(cellList.size() == 0 && eofReached) {
                    return null; //To handle last new line
                }
            } catch (Throwable t) {
                throw new TaggedCheckedException(t);
            }
            return cellList.toArray(new String[]{});
        }
    }
}