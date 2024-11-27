package org.ifsoft.chordpro;

import java.util.* ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Song2mid {
    private static final Logger Log = LoggerFactory.getLogger( Song2mid.class );	
    List<SongElement> elementList ;

    String title ;
    String composer ;
    String artist ;
    String copyright ;
    String timeSignature ;
    String tempo ;
    int key ;
    int keyScale ;

    int divisionsPerBar,   // Numerator in the time signature
	division,          // Denominator in the time signature
	ticksPerDivision,  // MIDI ticks per time division
	ticksPerBar,       // MIDI ticks per bar (measure)
	bpm ;              // Integer representation of songTempo

    long tickDelay,        // Ticks until next chord
	timeStamp ;        // Current MIDI time in ticks

    boolean instrumentalSection,
	precheck,          // Enables precheck before converting 1st chord
	lenientMode,       // Disables precheck and stricter checking
	tempoFlag,         // Set true by tempo directive
	timeFlag,          // Set true by time directive
	keyFlag,           // Set true by key directive
	stylecodeFlag ;    // Set true by stylecode directive

    public Song2mid() {
	title = "" ;
	composer = "" ;
	artist = "" ;
	copyright = "" ;
	timeSignature = "4/4" ;
	tempo = "120" ;
	bpm = 120 ;
	key = 0 ;
	keyScale = 0 ;

	divisionsPerBar = 4 ;     // AKA 4/4
	division = 4 ;
	ticksPerDivision = 1920 ; // Ticks per quarter
	ticksPerBar = divisionsPerBar * ticksPerDivision ;
	tickDelay = (long) 0 ;
	timeStamp = (long) 0 ;

	// Internal control flags
	instrumentalSection = false ;
	lenientMode = true ;
	precheck = true ;

    }

    // Advance time by the specified number of ticks
    // Only one song element advances time: chord. All other song elements
    // occur at the currne MIDI time (timeStamp). This may change...
    private void advanceTime(long ticks) {
	timeStamp = timeStamp + ticks ;
    }

    // Parse the incoming string which consists of a letter (ABCDEFG),
    // followed by an optional '#' or 'b', followed by an optional 'm' or
    // 'min'.
    // The MIDI representation is based on the major/minor circle of fifths.
    private void convertKey(String k) {
	key = 0 ;       // Number of sharps (>0) or flats (<0)
	keyScale = 0 ;  // 0: Major key, 1: Minor key
	if (k.isEmpty()) {
	    // No key specified, so default to C major
	    return ;
	}

	// Determine key scale (major or minor)
	k = k.toLowerCase() ;
	if ((k.indexOf("min") != -1) || (k.indexOf("m") != -1)) {
	    // Minor key
	    keyScale = 1 ;
	    if (k.startsWith("eb")) key = -6 ;
	    else if (k.startsWith("bb")) key = -5 ;
	    else if (k.startsWith("f")) key = -4 ;
	    else if (k.startsWith("c")) key = -3 ;
	    else if (k.startsWith("g")) key = -2 ;
	    else if (k.startsWith("d")) key = -1 ;
	    else if (k.startsWith("a")) key = 0 ;
	    else if (k.startsWith("e")) key = 1 ;
	    else if (k.startsWith("b")) key = 2 ;
	    else if (k.startsWith("f#")) key = 3 ;
	    else if (k.startsWith("c#")) key = 4 ;
	    else if (k.startsWith("g#")) key = 5 ;
	    else if (k.startsWith("d#")) key = 6 ;
	    else key = 0 ;
	} else {
	    // Major key
	    keyScale = 0 ;
	    if (k.startsWith("gb")) key = -6 ;
	    else if (k.startsWith("db")) key = -5 ;
	    else if (k.startsWith("ab")) key = -4 ;
	    else if (k.startsWith("eb")) key = -3 ;
	    else if (k.startsWith("bb")) key = -2 ;
	    else if (k.startsWith("f")) key = -1 ;
	    else if (k.startsWith("c")) key = 0 ;
	    else if (k.startsWith("g")) key = 1 ;
	    else if (k.startsWith("d")) key = 2 ;
	    else if (k.startsWith("a")) key = 3 ;
	    else if (k.startsWith("e")) key = 4 ;
	    else if (k.startsWith("b")) key = 5 ;
	    else if (k.startsWith("f#")) key = 6 ;
	    else key = 0 ;
	}
    }

    private boolean isPowerOfTwo(int x) {
	return( (x > 0) && ((x & (x - 1)) == 0) ) ;
    }

    private int getPowerOfTwo(int x) {
	return( (x == 0) ? 0 : (31 - Integer.numberOfLeadingZeros(x)) ) ;
    }

    // Convert a time signature string to division ("denominator") and
    // divisions per bar ("numerator"). Compute ticks per division and
    // ticks per bar.
    private void convertTimeSignature(String t, int resolution) {
	// Set defaults in case of error
	divisionsPerBar = 4 ;
	division = 4 ;
	ticksPerDivision = resolution ;
	ticksPerBar = divisionsPerBar * ticksPerDivision ;
	Log.debug("Resolution: " + resolution) ;

	int slash ;
	if (t.isEmpty() || ((slash = t.indexOf("/")) == -1)) {
	    Log.error("Check time signature syntax (no '/')") ;
	    return ;
	}
	String num = t.substring(0, slash) ;
	String den = t.substring(slash+1) ;
	if (num.isEmpty() || den.isEmpty()) {
	    Log.error("Check time signature syntax (num/den)") ;
	    return ;
	}
	try {
	    divisionsPerBar = Integer.parseInt(num) ;
	    division = Integer.parseInt(den) ;
	} catch(Exception exc) {
	    Log.error("Check time signature syntax (not integer)") ;
	    return ;
	}
	// Compute correct ticks per division
	if (isPowerOfTwo(division)) {
	    switch(division) {
	    case 1 :  ticksPerDivision = resolution*4 ; break ;
	    case 2 :  ticksPerDivision = resolution*2 ; break ;
	    case 4 :  ticksPerDivision = resolution*1 ; break ;
	    case 8 :  ticksPerDivision = resolution/2 ; break ;
	    case 16 : ticksPerDivision = resolution/4 ; break ;
	    case 32 : ticksPerDivision = resolution/8 ; break ;
	    case 64 : ticksPerDivision = resolution/16 ; break ;
	    default:
		Log.error("Time sig denomination is >64") ;
		division = 64 ;
		ticksPerDivision = resolution/16 ;
	    }
	    ticksPerBar = divisionsPerBar * ticksPerDivision ;
	} else {
	    Log.error("Check time signature (division is not power of 2)") ;
	}
    }

    private void convertComment(SongElement e, MidiFile m) {
	// Nothing to see here; move along.
	Log.debug("'" + e.elementText + "'") ;
    }

    // Encode a chord note into Yamaha format
    // A root is one of 'A', 'B', 'C', 'D', 'E', 'F', 'G'
    // A modifier is one of '#', 'b', ' '
    private int encodeChordNote(char root, char modifier) {
	int note = 0x1 ;
	switch(root) {
	case 'C' :
	case 'c' :
	    note = 0x1 ;
	    break ;
	case 'D' :
	case 'd' :
	    note = 0x2 ;
	    break ;
	case 'E' :
	case 'e' :
	    note = 0x3 ;
	    break ;
	case 'F' :
	case 'f' :
	    note = 0x4 ;
	    break ;
	case 'G' :
	case 'g' :
	    note = 0x5 ;
	    break ;
	case 'A' :
	case 'a' :
	    note = 0x6 ;
	    break ;
	case 'B' :
	case 'b' :
	    note = 0x7 ;
	    break ;
	}

	if (modifier == '#') {
	    note = note | 0x40 ; // One sharp
	} else if (modifier == 'b') {
	    note = note | 0x20 ; // One flat
	} else {
	    note = note | 0x30 ; // Natural
	}

	return( note ) ;
    }

    // Encode a chord type into Yamaha format
    // A qualifier is one of "", "maj", "min", "minmaj", "aug", "dim"
    // An extension is one of the recognized extensions, e.g., 7, 9, sus4, ...
    private int encodeChordType(String qualifier, String extensions) {
	int type = 0 ;
	if (qualifier.equalsIgnoreCase("")) {
	    // Major triad or dominant
	    if (extensions.equalsIgnoreCase("6")) {
		type = 0x01 ;
	    } else if (extensions.equalsIgnoreCase("7")) {
		type = 0x13 ;
	    } else if (extensions.equalsIgnoreCase("7sus4")) {
		type = 0x14 ;
	    } else if (extensions.equalsIgnoreCase("7b5")) {
		type = 0x15 ;
	    } else if (extensions.equalsIgnoreCase("7-9")) {
		type = 0x16 ;
	    } else if (extensions.equalsIgnoreCase("7#11")) {
		type = 0x17 ;
	    } else if (extensions.equalsIgnoreCase("7-13")) {
		type = 0x18 ;
	    } else if (extensions.equalsIgnoreCase("7b9")) {
		type = 0x19 ;
	    } else if (extensions.equalsIgnoreCase("7b13")) {
		type = 0x1A ;
	    } else if (extensions.equalsIgnoreCase("7#9")) {
		type = 0x1B ;
	    } else if (extensions.equalsIgnoreCase("7aug")) {
		type = 0x1D ; // 7aug
	    } else if (extensions.equalsIgnoreCase("8")) {
		type = 0x1E ; // 1+8
	    } else if (extensions.equalsIgnoreCase("5")) {
		type = 0x1F ; // 1+5
	    } else if (extensions.equalsIgnoreCase("sus4")) {
		type = 0x20 ;
	    } else if (extensions.equalsIgnoreCase("sus2")) {
		type = 0x21 ; // 1+2+5
	    } else {
		type = 0x00 ;
	    }
	} else if (qualifier.equalsIgnoreCase("maj")) {
	    // Major 7th
	    if (extensions.equalsIgnoreCase("6")) {
		type = 0x01 ;
	    } else if (extensions.equalsIgnoreCase("7")) {
		type = 0x02 ;
	    } else if (extensions.equalsIgnoreCase("7#11")) {
		type = 0x03 ;
	    } else if (extensions.equalsIgnoreCase("9")) {
		type = 0x04 ;
	    } else if (extensions.equalsIgnoreCase("7-9")) {
		type = 0x05 ;
	    } else if (extensions.equalsIgnoreCase("6-9")) {
		type = 0x06 ;
	    } else if (extensions.equalsIgnoreCase("7aug")) {
		type = 0x1C ;
	    } else {
		type = 0x00 ;
	    }
	} else if (qualifier.equalsIgnoreCase("min")) {
	    // Minor chord
	    if (extensions.equalsIgnoreCase("6")) {
		type = 0x09 ;
	    } else if (extensions.equalsIgnoreCase("7")) {
		type = 0x0A ;
	    } else if (extensions.equalsIgnoreCase("7b5")) {
		type = 0x0B ;
	    } else if (extensions.equalsIgnoreCase("9")) {
		type = 0x0C ;
	    } else if (extensions.equalsIgnoreCase("7-9")) {
		type = 0x0D ;
	    } else if (extensions.equalsIgnoreCase("7-11")) {
		type = 0x0E ;
	    } else {
		type = 0x08 ;
	    }
	} else if (qualifier.equalsIgnoreCase("minmaj")) {
	    // MinMaj chord
	    if (extensions.equalsIgnoreCase("7")) {
		type = 0x0F ;
	    } else if (extensions.equalsIgnoreCase("7-9")) {
		type = 0x10 ;
	    } else {
		type = 0x0F ;
	    }
	} else if (qualifier.equalsIgnoreCase("aug")) {
	    // Augmented chord
	    if (extensions.equalsIgnoreCase("7")) {
		type = 0x1D ;
	    } else {
		type = 0x07 ;
	    }
	} else if (qualifier.equalsIgnoreCase("dim")) {
	    // Diminished chord
	    if (extensions.equalsIgnoreCase("7")) {
		type = 0x12 ;
	    } else {
		type = 0x11 ;
	    }
	}
	return( type ) ;
    }

    private void convertDirective(SongElement e, MidiFile m) {
	// Recognize and handle our subset of ChordPro directives
	Log.debug(e.elementDirective + " " + "'" + e.elementValue + "'") ;

	String dir = e.elementDirective.toLowerCase() ;
	if (dir.equals("title") || dir.equals("t")) {
	    // Song title
	    // Generate MIDI track name message
	    try {
		title = e.elementValue ;
		// For now, preamble MIDI events are hardcoded for the
		// beginning of the MIDI sequence, i.e., timestamp zero.
		m.addTrackName(title, 0) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add title info") ;
	    }
	} else if (dir.equals("comment") || dir.equals("c")) {
	    // Comment
	    // Ignore comments; No further action
	} else if (dir.equals("start_of_chorus") || dir.equals("soc")) {
	    // Start of chorus; Generate a lyric page break
	    try {
		m.addPageBreak(timeStamp + ticksPerDivision) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add page break (soc)") ;
	    }
	} else if (dir.equals("end_of_chorus") || dir.equals("eoc")) {
	    // End of chorus
	    // TBD: Maybe change to an appropriate accompaniment section?
	} else if (dir.equals("start_of_instrumental") || dir.equals("soi")) {
	    // Start of instrumental section; Generate a lyric page break
	    try {
		// Set instrumental section flag
		instrumentalSection = true ;
		m.addPageBreak(timeStamp + ticksPerDivision) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add page break (soi)") ;
	    }
	} else if (dir.equals("end_of_instrumental") || dir.equals("eoi")) {
	    // End of instrumental section
	    // Clear instrumental section flag
	    instrumentalSection = false ;
	} else if (dir.equals("chorus")) {
	    // Chorus
	    // TBD: Maybe change to an appropriate accompaniment section?
	    // Start of chorus; Generate a lyric page break
	    try {
		m.addPageBreak(timeStamp + ticksPerDivision) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add page break (chorus)") ;
	    }
	} else if (dir.equals("start_of_verse") || dir.equals("sov")) {
	    // Start of verse
	    // TBD: Maybe change to an appropriate accompaniment section?
	} else if (dir.equals("end_of_verse") || dir.equals("eov")) {
	    // end of verse
	    // TBD: Maybe change to an appropriate accompaniment section?
	} else if (dir.equals("start_of_bridge") || dir.equals("sob")) {
	    // Start of bridge
	    // TBD: Maybe change to an appropriate accompaniment section?
	} else if (dir.equals("end_of_bridge") || dir.equals("eob")) {
	    // End of bridge
	    // TBD: Maybe change to an appropriate accompaniment section?
	} else if (dir.equals("artist")) {
	    // Artist
	    try {
		artist = e.elementValue ;
		m.addArtist(artist, 0) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add artist info") ;
	    }
	} else if (dir.equals("composer")) {
	    // Composer
	    try {
		composer = e.elementValue ;
		m.addComposer(composer, 0) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add composer info") ;
	    }
	} else if (dir.equals("copyright")) {
	    // Copyright
	    try {
		copyright = e.elementValue ;
		m.addCopyright(copyright, 0) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add copyright info") ;
	    }
	} else if (dir.equals("key")) {
	    // Set the key signature
	    keyFlag = true ;
	    convertKey(e.elementValue) ;
	    try {
		m.addKeySignature(key, keyScale, (timeStamp + tickDelay)) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add key signature") ;
	    }
	} else if (dir.equals("time")) {
	    // Set the time signature
	    timeFlag = true ;
	    timeSignature = e.elementValue ;
	    convertTimeSignature(timeSignature, m.getResolution()) ;
	    try {
		// Time signature could change in the middle of a song
		m.addTimeSignature(divisionsPerBar, division,  (timeStamp + tickDelay)) ;
		Log.debug("Div: " + division + " PerBar: " + divisionsPerBar) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add time signature") ;
	    }
	} else if (dir.equals("tempo")) {
	    // Set the tempo; Tempo is specified in BPM
	    tempoFlag = true ;
	    try {
		// Tempo could change in the middle of a song
		tempo = e.elementValue ;
		bpm = Integer.parseInt(tempo) ;
		m.addTempo(bpm, (timeStamp + tickDelay)) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add tempo (BPM)") ;
	    }
	} else if (dir.equals("stylecode")) {
	    // Set the accompaniment style and start accompaniment
	    // Must be the final directive before lyrics and chords
	    // Must follow all other set-up, especially time signature
	    stylecodeFlag = true ;
	    try {
		// Style code occurs once at the end of the song preamble
		int code = Integer.parseInt(e.elementValue) ;
		m.addStyleCode(code, (long)0) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add accompaniment style") ;
	    }
	} else if (dir.equals("start_accomp")) {
	    // Start accompaniment (start song playing)
	    try {
		tickDelay = (long) ticksPerBar ;  // Was ticksPerBar
		m.addAccompStart((long)(timeStamp + tickDelay)) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add start accompaniment") ;
	    }
	} else if (dir.equals("stop_accomp")) {
	    // Stop accompaniment (stop song playing)
	    try {
		long finalTimestamp = timeStamp + ticksPerBar ;
		m.addAccompStop(finalTimestamp) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add stop accompaniment") ;
	    }
	} else {
	    // Ignore all other directives; No further action
	}
    }

    private void convertLyric(SongElement e, MidiFile m) {
	// Generate a MIDI Meta message for lyrics

	String lyric = e.elementText ;
	if (lyric.isEmpty()) return ;

	Log.debug("'" + lyric + "'") ;
	Log.debug("Tick delay: " + tickDelay) ;

	try {
	    // A lyric occurs at the same timestamp as the preceding chord
	    m.addLyric(" " + lyric + " ", timeStamp);
	} catch (Exception exc) {
	    Log.error("Couldn't add lyric '" + lyric + "'") ;
	}
    }

    // Execute this function before converting the first chord. Make sure
    // a tempo, time signature, key, and style code directive have been
    // performed.
    private void firstTimePrecheck(MidiFile m) {
	if (lenientMode) {
	    // Return if lenient mode is enabled
	    return ;
	}

	if (! tempoFlag) {
	    Log.error("Tempo directive is needed before 1st chord") ;
	    // Assume 120 BPM
	    try {
		// Tempo could change in the middle of a song
		tempo = "120" ;
		bpm = Integer.parseInt(tempo) ;
		m.addTempo(bpm, timeStamp) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add tempo (pre)") ;
	    }
	}
	if (! timeFlag) {
	    Log.error("Time directive is needed before 1st chord") ;
	    // Assume 4/4
	    timeSignature = "4/4" ;
	    convertTimeSignature(timeSignature, m.getResolution()) ;
	    try {
		// Time signature could change in the middle of a song
		m.addTimeSignature(divisionsPerBar, division, timeStamp) ;
		Log.debug("Div: " + division + " PerBar: " + divisionsPerBar) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add time signature (pre)") ;
	    }
	}
	if (! keyFlag) {
	    Log.error("Key directive is needed before 1st chord") ;
	    // Assume C major
	    convertKey("C") ;
	    try {
		m.addKeySignature(key, keyScale, (long)0) ;
	    } catch(Exception exc) {
		Log.error("Couldn't add key signature (pre)") ;
	    }
	}
	if (! stylecodeFlag) {
	    Log.error("Stylecode directive is needed before 1st chord") ;
	    // Assume "ContempPop", style code: 8210
	    try {
		// Style code occurs once at the end of the song preamble
		int code = 8210 ;
		m.addStyleCode(code, 0) ;
		m.addAccompStart(0) ;
		tickDelay = (long)0 ;  // Was ticksPerBar
	    } catch(Exception exc) {
		Log.error("Couldn't add accompaniment style (pre)") ;
	    }
	}
	// Perform the precheck only once
	precheck = false ;
    }

    private void convertChord(SongElement e, MidiFile m) {
	// Recognize the subset of chords supported by Yamaha arrangers
	// Generate a Yamaha SysEx message for a chord
	/*
	System.out.print("'" + e.elementText + "' ") ;
	System.out.print(" " + e.rootNote + e.rootModifier) ;
	System.out.print(" " + e.qualifier + " " + e.extensions) ;
	
	if ((e.bassNote != e.rootNote) || (e.bassModifier != e.rootModifier)) {
		System.out.print(" /" + e.bassNote + e.bassModifier) ;
	}
	if (e.beatCount != -1) {
		System.out.print(" " + e.beatCount) ;
	}
	System.out.println("") ;
	*/
	
	// If this is the first chord, then perform a first time precheck
	if (precheck) {
	    firstTimePrecheck(m) ;
	    precheck = false ;
	}

	int chordRoot = encodeChordNote(e.rootNote, e.rootModifier) ;
	int chordType = encodeChordType(e.qualifier, e.extensions) ;
	int chordBass = encodeChordNote(e.bassNote, e.bassModifier) ;

	Log.debug("Tick delay: " + tickDelay) ;
	try {
	    // Advance time by the chord hold (delay)
	    advanceTime(tickDelay) ;
	    m.addChord(chordRoot, chordType, chordBass, 0x7F, timeStamp);
	    //m.addChordXF(chordRoot, chordType, chordBass, 0x7F, timeStamp);
		
	} catch (Exception exc) {
	    Log.error("Couldn't add chord") ;
	}

	// Experimental!
	// If processing a chord in an instrumental section, add a blank
	// comment to put space between chords.

	if (instrumentalSection) {
	    try {
		m.addLyric("-------- ", timeStamp) ;
	    } catch (Exception exc) {
		Log.error("Couldn't add blank lyric for a chord") ;
	    }
	}

	// Hold this chord for the specified time, i.e., compute the delay
	// before the next chord in the song. A lyric following this chord
	// should have the same timestamp as the chord.
	if (e.beatCount == -1) {
	    tickDelay = ticksPerBar ;
	} else {
	    tickDelay = e.beatCount * ticksPerDivision ;
	}
    }

    private void convertAnnotation(SongElement e, MidiFile m) {
	// Recognize and handle section control, e.g., MA, FA, etc.
	Log.debug("'" + e.elementText + "'") ;

	// First character should be '*'
	if (e.elementText.charAt(0) != '*') {
	    Log.error("*First char in annotation is not '*'") ;
	    return ;
	}

	String ann = (e.elementText.substring(1)).toUpperCase() ;
	Log.debug("'" + ann + "'") ;
	int section = -1 ;
	if (ann.equals("MA")) section = 0x08 ;
	else if (ann.equals("MB")) section = 0x09 ;
	else if (ann.equals("MC")) section = 0x0A ;
	else if (ann.equals("MD")) section = 0x0B ;
	else if (ann.equals("FA")) section = 0x10 ;
	else if (ann.equals("FB")) section = 0x11 ;
	else if (ann.equals("FC")) section = 0x12 ;
	else if (ann.equals("FD")) section = 0x13 ;
	else if (ann.equals("BR")) section = 0x18 ;
	else if (ann.equals("IA")) section = 0x00 ;
	else if (ann.equals("IB")) section = 0x01 ;
	else if (ann.equals("IC")) section = 0x02 ;
	else if (ann.equals("ID")) section = 0x03 ;
	else if (ann.equals("EA")) section = 0x20 ;
	else if (ann.equals("EB")) section = 0x21 ;
	else if (ann.equals("EC")) section = 0x22 ;
	else if (ann.equals("ED")) section = 0x23 ;

	// If section is the sentinel -1, it's not section control. Ignore it.
	if (section == -1) return ;

	// Add section control to the MIDI file
	try {
	    m.addSectionControl(section, timeStamp) ;
	} catch(Exception exception) {
	    Log.error("Could not add section control '" + ann + "'") ;
	    return ;
	}
    }

    private void convertTab(SongElement e, MidiFile m) {
	// TBD stub
	Log.debug("'" + e.elementTab + "'") ;
    }

    // Yamaha support line and page break suggestions in lyrics
    // This function generates a lyric line break meta message
    private void convertLine(SongElement e, MidiFile m) {
	Log.debug(" ") ;
	try {
	    // Adding ticksPerBar is cheating a little on time advancement
	    m.addLineBreak(timeStamp + ticksPerDivision) ;
	} catch(Exception exception) {
	    Log.error("*fatal* Could not add lyric line break") ;
	    return ;
	}
    }

    public void convertSongToMidi(Song song, MidiFile midiFile) {
	elementList = song.getSongElements() ;
	String type ;

	// Add mandatory boilerplate to the MIDI file
	try {
	    midiFile.makeMidiSequence() ;
	    midiFile.addGMReset(0) ;
	    midiFile.addXGSystemOn(0) ;
	} catch(Exception exception) {
	    Log.error("*fatal* Could not add MIDI boilerplate") ;
	    return ;
	}

	Iterator<SongElement> iter = elementList.iterator() ;
	while (iter.hasNext()) {
	    SongElement e = iter.next() ;
	    type = e.getType() ;
	    Log.debug(type + " ") ;

	    if (type.equals("Comment")) {
		convertComment(e, midiFile) ;
	    } else if (type.equals("Lyric")) {
		convertLyric(e, midiFile) ;
	    } else if (type.equals("Chord")) {
		convertChord(e, midiFile) ;
	    } else if (type.equals("Annotation")) {
		convertAnnotation(e, midiFile) ;
	    } else if (type.equals("Directive")) {
		convertDirective(e, midiFile) ;
	    } else if (type.equals("Tab")) {
		convertTab(e, midiFile) ;
	    } else if (type.equals("Line")) {
		convertLine(e, midiFile) ;
	    }
	}

	// Terminate the MIDI sequencer track  (FIX UP)
	try {
	    long finalTimestamp = timeStamp + ticksPerBar ;
	    midiFile.addAccompStop(finalTimestamp) ;
	    midiFile.addEndOfTrack(finalTimestamp) ;
	} catch(Exception exception) {
	    Log.error("*fatal* Could not add MIDI end of track") ;
	    return ;
	}
    }
}

