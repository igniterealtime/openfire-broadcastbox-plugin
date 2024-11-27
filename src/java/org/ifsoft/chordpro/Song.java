package org.ifsoft.chordpro;

import java.util.*;
import java.io.*;

public class Song {

    // A Song is a list of SongElements plus properties
    List<SongElement> elementList = new ArrayList<SongElement>() ;
    String[] lines ;
    int lineIndex ;

    public Song() {
	lineIndex = 0 ;
    }

    public List<SongElement> getSongElements() {
	return( elementList ) ;
    }

    public void printSong() {
	Iterator<SongElement> iter = elementList.iterator() ;
	while (iter.hasNext()) {
	    SongElement e = iter.next() ;
	    e.printSongElement() ;
	}
    }

    private boolean isNote(char n) {
	switch( n ) {
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
            return( true ) ;
	}
	return( false ) ;
    }

    private boolean isModifier(char m) {
	return( (m == 'b') || (m == '#') ) ;
    }

    public void readChordproFile(String filename) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(filename)) ;		
		readChordpro(in);		
	}
	
    public void readChordproStream(InputStream stream) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream)) ;		
		readChordpro(in);		
	}	

    public void readChordproString(String string) throws Exception {
		BufferedReader in = new BufferedReader( new StringReader(string));
		readChordpro(in);		
	}
	
    public void readChordpro(BufferedReader in) throws Exception {
	// Read a Chordpro file into a List of Strings
        String str ;
        List<String> list = new ArrayList<String>() ;

	// Read each line in the file and add it to the List of Strings
        while((str = in.readLine()) != null ){
            list.add(str);
        }

	// Convert the List of Strings to an Array of Strings
        lines = list.toArray(new String[0]) ;
        // System.out.println(" " + Arrays.toString(lines)) ;
    }

    private String parseNote(String s, SongElement e) {
	if (s.length() < 1) {
	    System.err.println("*error* in line " + lineIndex) ;
	    System.err.println("Root note name is empty") ;
	    return( "" ) ;
	}

	char n = Character.toUpperCase(s.charAt(0)) ;
	if (! isNote(n)) {
	    System.err.println("*error* in line " + lineIndex) ;
	    System.err.println("'"+n+"' not a note name (one of ABCDEFG)") ;
	} else {
	    e.setRootNote(n) ;
	    e.setBassNote(n) ; // A slash (alt bass) could change this
	}
	return( s.substring(1) ) ;
    }

    private String parseModifier(String s, SongElement e) {
	if (s.length() < 1) {
	    return( "" ) ;
	}

	char m = s.charAt(0) ;
	if (m == '#') {
	    e.setRootModifier('#') ;
	    e.setBassModifier('#') ; // Could be changed by a slash (alt bass)
	    return( s.substring(1) ) ;
	} else if (m == 'b') {
	    e.setRootModifier('b') ;
	    e.setBassModifier('b') ; // Could be changed by a slash (alt bass)
	    return( s.substring(1) ) ;
	}
	return( s ) ;
    }

    private String parseQualifier(String s, SongElement e) {
	e.setQualifier("") ;

	if (s.length() < 1) {
	    return( "" ) ;
	}

	String ls = s.toLowerCase() ;
	if (ls.startsWith("minmaj")) {
	    e.setQualifier("minmaj") ;
	    return( s.substring(6) ) ;
	}
	if (ls.startsWith("mmaj") || ls.startsWith("-maj")) {
	    e.setQualifier("minmaj") ;
	    return( s.substring(4) ) ;
	}

	if (ls.startsWith("maj")) {
	    e.setQualifier("maj") ;
	    return( s.substring(3) ) ;
	}
	if (ls.startsWith("min")) {
	    e.setQualifier("min") ;
	    return( s.substring(3) ) ;
	}
	if (ls.startsWith("aug")) {
	    e.setQualifier("aug") ;
	    return( s.substring(3) ) ;
	}
	if (ls.startsWith("dim")) {
	    e.setQualifier("dim") ;
	    return( s.substring(3) ) ;
	}

	switch( s.charAt(0) ) {
	case 'M' :
	case '^' :
	    e.setQualifier("maj") ;
	    return( s.substring(1) ) ;
	case 'm' :
	case '-' :
	    e.setQualifier("min") ;
	    return( s.substring(1) ) ;
	case '+' :
	    e.setQualifier("aug") ;
	    return( s.substring(1) ) ;
	case 'o' :
	case '0' :
	    e.setQualifier("dim") ;
	    return( s.substring(1) ) ;
	}

	return( s ) ;
    }

    private String parseExtensions(String s, SongElement e) {
	String ext = "" ;
	if (s.length() < 1) {
	    return( "" ) ;
	}

	int close = s.indexOf("/") ;
	if (close != -1) {
	    // Beginning of slash chord ends any extensions
	    e.setExtensions(s.substring(0,close).trim()) ;
	    return( s.substring(close).trim() ) ;
	}

	close = s.indexOf(":") ;
	if (close != -1) {
	    // Beginning of beat count ends any extensions
	    e.setExtensions(s.substring(0,close).trim()) ;
	    return( s.substring(close).trim() ) ;
	}

	// EOL ends any extensions (no slash/alternate bass or beat count)
	e.setExtensions(s.trim()) ;
	return( "" ) ;
    }

    private String parseBassNote(String s, SongElement e) {
	if (s.length() < 1) {
	    return( "" ) ;
	}

	char n = s.charAt(0) ;
	if (n != '/') {
	    return( s ) ;
	}
	s = s.substring(1) ;

	if (s.length() < 1) {
	    System.err.println("*error* in line " + lineIndex) ;
	    System.err.println("Expecting a note name after a '/'") ;
	    return( "" ) ;
	}

	n = Character.toUpperCase(s.charAt(0)) ;
	if (! isNote(n)) {
	    System.err.println("*error* in line " + lineIndex) ;
	    System.err.println("'"+n+"' not a note name (one of ABCDEFG)") ;
	    return( "" ) ;
	} else {
	    e.setBassNote(n) ;
	    e.setBassModifier(' ') ;
	}
	s = s.substring(1) ;

	if (s.length() < 1) {
	    return( "" ) ;
	}

	char m = s.charAt(0) ;
	if (m == '#') {
	    e.setBassModifier('#') ;
	    return( s.substring(1) ) ;
	} else if (m == 'b') {
	    e.setBassModifier('b') ;
	    return( s.substring(1) ) ;
	}

	return( s ) ;
    }

    private String parseBeatCount(String s, SongElement e) {
	if (s.length() < 2) {
	    return( "" ) ;
	}

	char c = s.charAt(0) ;
	if (c != ':') {
	    System.err.println("*error* in line " + lineIndex) ;
	    System.err.println("Expected ':' and found '" + c + "'") ;
	    return("") ;
	} else {
	    // Extract the count (i.e., a sequence of one or more digits)
	    String n = s.substring(1) ;
	    if (n.matches("[0-9]+")) {
		e.setBeatCount(Integer.parseInt(n)) ;
	    }
	}

	// Should be the end of the chord, so return an empty string
	return( "" ) ;
    }

    private void parseChord(String chord, SongElement e) {
	String s = chord ;

	// Establish default values
	e.setRootNote('c') ;
	e.setRootModifier(' ') ;
	e.setQualifier("") ;
	e.setExtensions("") ;
	e.setBassNote('c') ;
	e.setBassModifier(' ') ;
	e.setBeatCount(-1) ;

	if (chord.isEmpty()) {
	    System.err.println("*error* in line " + lineIndex) ;
	    System.err.println("Chord is empty") ;
	}

	s = parseNote(s, e) ;
	s = parseModifier(s, e) ;
	s = parseQualifier(s, e) ;
	s = parseExtensions(s, e) ;
	s = parseBassNote(s, e) ;
	s = parseBeatCount(s, e) ;
    }

    // Parse a line into chords, lyrics and annotations. Suggest a line break
    // when appropriate.
    private void parseLine() {
	String s = lines[lineIndex].trim() ;

	// Take a fast out if the incoming line is empty
	if (s.isEmpty()) return ;

	while( ! s.isEmpty() ) {
	    if (s.charAt(0) == '[') {
		// Chord or annotation -- Save to closing ']'
		SongElement e = new SongElement() ;
		int close = s.indexOf(']') ;
		if (close == -1) {
		    System.err.println("*error* in line " + lineIndex) ;
		    System.err.println("No closing ']' in chord/annotation") ;
		    e.setText("*Error*") ;
		    // No element is added to elementList when there is error
		    return ;
		} else {
		    String t = s.substring(1, close).trim() ;
		    e.setText(t) ;
		    if (s.charAt(1) == '*') {
			// Annotation
			e.setType("Annotation") ;
		    } else {
			// Chord
			e.setType("Chord") ;
			parseChord(t, e) ;
		    }
		    elementList.add(e) ;
		}
		s = s.substring(close+1).trim() ;
	    } else {
		// Lyrics
		SongElement e = new SongElement() ;
		e.setType("Lyric") ;
		// Find the end of lyric text (either a '[' or EOL)
		int close = s.indexOf('[') ;
		if (close == -1) {
		    // No more chords or lyric text; save to EOL
		    e.setText(s.trim()) ;
		    elementList.add(e) ;
		    // Nothing left in the line; force loop exit
		    s = "" ;
		} else {
		    // A chord or annotation follows
		    e.setText(s.substring(0, close).trim()) ;
		    elementList.add(e) ;
		    s = s.substring(close).trim() ;
		}
	    }
	}

	// Add a line break formatting suggestion
	SongElement e = new SongElement() ;
	e.setType("Line") ;
	elementList.add(e) ;
    }

    public void createSong() {
	String s ;

	for (lineIndex = 0 ; lineIndex < lines.length ; lineIndex++) {
	    s = lines[lineIndex] ;
	    if (s.isEmpty()) {
		// Empty string? Fuggedaboutit!
	    } else if (s.charAt(0) == '#') {
		// Comment -- ignore to end of line
		SongElement e = new SongElement() ;
		e.setType("Comment") ;
		e.setText(s.substring(1).trim()) ;
		elementList.add(e) ;
	    } else if (s.charAt(0) == '{') {
		// Directive -- Save to closing '}'
		SongElement e = new SongElement() ;
		e.setType("Directive") ;
		e.setText(s.substring(1).trim()) ;
		int close = s.lastIndexOf('}') ;
		if (close == -1) {
		    System.err.println("*error* in line " + lineIndex) ;
		    System.err.println("No closing '}' in directive") ;
		    e.setDirective("*Error*") ;
		} else {
		    int colon = s.indexOf(':') ;
		    if (colon == -1) {
			e.setDirective(s.substring(1, close).trim()) ;
			e.setValue("") ;
		    } else {
			e.setDirective(s.substring(1, colon).trim()) ;
			e.setValue(s.substring(colon+1, close).trim()) ;
		    }
		}
		elementList.add(e) ;
	    } else {
		// Parse a line containing lyrics, chords and annotations
		// May also suggest a formatting line break
		parseLine() ;
	    }
	}
    }

}
