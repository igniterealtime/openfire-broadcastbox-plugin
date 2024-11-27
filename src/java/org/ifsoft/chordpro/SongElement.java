package org.ifsoft.chordpro;

import java.util.* ;

class SongElement {

    // "Chord", "Directive", "Lyric", "Comment", "Tab", "Annotation", "Line"
    String elementType ;       // Tag to identify the type of song element
    String elementValue ;      // Value for a directive, etc.
    String elementChord ;
    String elementDirective ;  // Directive name
    String elementText ;       // Raw element text
    String elementTab ;        // TBD
    String elementLine ;       // Line break suggestion

    // Chord-related values
    // A chord consists of a mandatory root note followed by six optional
    // parts (from left to right): root note modifier (b or #), qualifier,
    // (maj, min, or blank), extension (7, etc.), bass note, bass note
    // modifier, beat count.
    char rootNote ;
    char rootModifier ;
    String qualifier ;
    String extensions ;
    char bassNote ;
    char bassModifier ;
    int beatCount ;

    public SongElement() {
	elementType = "" ;
	elementValue = "" ;
	elementChord = "" ;
	elementDirective = "" ;
	elementText = "" ;
	elementTab = "" ;

	rootNote = 'c' ;
	rootModifier = ' ' ;
	qualifier = "" ;
	extensions = "" ;
	bassNote = 'c' ;
	bassModifier = ' ' ;
	beatCount = -1 ;  // -1 is sentinel value meaning not defined
    }

    public void printSongElement() {
	String type = elementType ;

	System.out.print(type + " ") ;

	if (type.equals("Comment")) {
	    System.out.println("'" + elementText + "'") ;
	} else if (type.equals("Lyric")) {
	    System.out.println("'" + elementText + "'") ;
	} else if (type.equals("Chord")) {
	    System.out.print("'" + elementText + "' ") ;
	    System.out.print(" " + rootNote + rootModifier) ;
	    System.out.print(" " + qualifier + " " + extensions) ;
	    if ((bassNote != rootNote) || (bassModifier != rootModifier)) {
		System.out.print(" /" + bassNote + bassModifier) ;
	    }
	    if (beatCount != -1) {
		System.out.print(" " + beatCount) ;
	    }
	    System.out.println("") ;
	} else if (type.equals("Annotation")) {
	    System.out.println("'" + elementText + "'") ;
	} else if (type.equals("Directive")) {
		System.out.print(elementDirective + " ") ;
		System.out.println("'" + elementValue + "'") ;
	} else if (type.equals("Tab")) {
		System.out.println("'" + elementTab + "'") ;
	} else if (type.equals("Line")) {
		System.out.println(" ") ;
	}
    }

    public void setType(String t) {
	elementType = t ;
    }

    public void setValue(String v) {
	elementValue = v ;
    }

    public void setChord(String c) {
	elementChord = c ;
    }

    public void setDirective(String d) {
	elementDirective = d ;
    }

    public void setText(String t) {
	elementText = t ;
    }

    public void setTab(String t) {
	elementTab = t ;
    }

    public void setRootNote(char n) {
	rootNote = n ;
    }

    public void setRootModifier(char m) {
	rootModifier  = m ;
    }

    public void setQualifier(String q) {
	qualifier = q ;
    }

    public void setExtensions(String e) {
	extensions = e ;
    }

    public void setBassNote(char n) {
	bassNote = n ;
    }

    public void setBassModifier(char m) {
	bassModifier = m ;
    }

    public void setBeatCount(int bc) {
	beatCount = bc ;
    }

    public String getType() {
	return( elementType ) ;
    }

    public String getValue() {
	return( elementValue ) ;
    }

    public String getChord() {
	return( elementChord ) ;
    }

    public String getDirective() {
	return( elementDirective ) ;
    }

    public String getText() {
	return( elementText ) ;
    }

    public String getTab() {
	return( elementTab ) ;
    }

    public char getRootNote() {
	return( rootNote ) ;
    }

    public char getRootModifier() {
	return( rootModifier ) ;
    }

    public String getQualifier() {
	return( qualifier ) ;
    }

    public String getExtensions() {
	return( extensions ) ;
    }

    public char getBassNote() {
	return( bassNote ) ;
    }

    public char getBassModifier() {
	return( bassModifier ) ;
    }

    public int getBeatCount() {
	return( beatCount ) ;
    }
}
