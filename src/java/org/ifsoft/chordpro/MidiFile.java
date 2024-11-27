package org.ifsoft.chordpro;
//
// Create and write a MIDI file to hold the chord track
// 
//

import java.lang.* ;
import java.util.* ;
import java.io.* ;
import javax.sound.midi.* ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Author:  P.J. Drongowski
// Version: 1.2
// Date:    November 24, 2021
//
// Copyright (c) 2018-2021 Paul J. Drongowski
//               Permission explicitly granted to modify and distribute

// This class generates the MIDI data needed to support and contain
// chord track(s). The MIDI file starts with some stock boilerplate and must
// end with an end track meta event. A MidiFile object keeps track of MIDI
// time in cumulative ticks (not SMF delta time). The Java API needs
// cumulative ticks, so that's what we remember.

public class MidiFile {
    private static final Logger Log = LoggerFactory.getLogger(MidiFile.class);
    String midiFileName = "NoName.mid" ;

    int trackFileFormat ;         // Format 0, 1 or 2
    int numberOfTracks ;          // Should be 1 for SMF type 0
    int resolution ;              // Expecting 480, 960, 1920, ...
    float divisionType ;          // PPQ, SMPTE_24, ...

    int beats ;                   // Time signature: beats per measure
    int division ;                // Time signative: beat unit (2, 4, 8, 16)
    int tempo ;                   // Tempo (BPM)

    long midiTime = 0 ;           // Cumulative MIDI clock ticks

    Sequence midiSequence ;       // Build the MIDI data in this object
    Track track ;                 // Sequence has only one track (Type 0)

    // MIDI time is cumulative ticks (i.e., ticks since the beginning of the
    // sequence.
    long midiTIme = 0 ;

    // Track On/Off flags (emitted with set-up messages). The flags are
    // stored in the low order byte.
    // Low order bit is RHYTHM1 and high order bit is PHRASE2
    int trackOnOff = 0xFF ;

    // Yamaha arrangers use a default 1920 ticks per quarter resolution
    public final int defaultResolution = 1920 ;

    public MidiFile() {
	trackFileFormat = 0 ;
	numberOfTracks = 1 ;
	divisionType = Sequence.PPQ ;
	resolution = defaultResolution ;

	// These defaults must match the defaults for Time objects
	beats = 4 ;
	division = 4 ;
	tempo = 120 ;

	midiTime = 0 ;
	trackOnOff = 0xFF ;
    }

    private boolean isPowerOfTwo(int x) {
	return( (x > 0) && ((x & (x - 1)) == 0) ) ;
    }

    private int getPowerOfTwo(int x) {
	return( (x == 0) ? 0 : (31 - Integer.numberOfLeadingZeros(x)) ) ;
    }

    public void setTimeSignature(int b, int d) {
	if (! isPowerOfTwo(d)) {
	    Log.debug("*error* Time sig division is not power of 2") ;
	}
	beats = b ;
	division = Integer.highestOneBit(d) ;
    }

    public void setTempo(int bpm) {
	tempo = bpm ;
    }

    public void setTrackOnOff(int flags) {
	trackOnOff = flags ;
    }

    public void setMidiTime(long ticks) {
	midiTime = ticks ;
    }

    public int getResolution() { return( defaultResolution ) ; }

    private long beats2ticks(int beatUnit) {
	long ticks = defaultResolution ;
	switch( beatUnit )
	    {
	    case 1: { ticks = resolution * 4 ; break ; }
	    case 2: { ticks = resolution * 2 ; break ; }
	    case 4: { ticks = resolution * 1 ; break ; }
	    case 8: { ticks = resolution / 2 ; break ; }
	    case 16: { ticks = resolution / 4 ; break ; }
	    case 32: { ticks = resolution / 8 ; break ; }
	    default: { ticks = resolution * 1 ; break ; }
	    }
	return( ticks ) ;
    }

    // Advance to the beginning of the specified measure
    public void setMeasure(int m) {
	// Compute the length of a one measure using the current
	// time signature.
	long delta = beats2ticks(division) * beats ;
	// Set the MIDI time
	midiTime = (m-1) * delta ;
    }

    // Advance to the beginning of the next measure
    public void nextMeasure() {
	long delta = beats2ticks(division) * beats ;
	midiTime = midiTime + delta ;
    }

    // Create a MIDI sequence
    public void makeMidiSequence() throws InvalidMidiDataException {
	midiSequence = new Sequence(divisionType, resolution) ;

	Log.debug("Division type: " + divisionType) ;
	Log.debug("Resolution: " + resolution) ;

	track = midiSequence.createTrack() ;
	numberOfTracks = 1 ;
    }

    // Track/sequence name
    public void addTrackName(String title, long ticks) 
	throws InvalidMidiDataException {
	if ((title != null) && (! title.isEmpty())) {
	    MetaMessage trackNameMsg = new MetaMessage() ;
	    trackNameMsg.setMessage(0x03, title.getBytes(), title.length()) ;
	    MidiEvent midiEvent = new MidiEvent(trackNameMsg, (long)0) ;
	    track.add(midiEvent) ;
	}
    }

    // Copyright
    public void addCopyright(String copyright, long ticks) 
	throws InvalidMidiDataException {
	if ((copyright != null) && (! copyright.isEmpty())) {
	    MetaMessage copyrightMsg = new MetaMessage() ;
	    copyrightMsg.setMessage(0x02, copyright.getBytes(), copyright.length()) ;
	    MidiEvent midiEvent = new MidiEvent(copyrightMsg, (long)0) ;
	    track.add(midiEvent) ;
	}
    }

    // Composer
    public void addComposer(String composer, long ticks) 
	throws InvalidMidiDataException {
	if ((composer != null) && (! composer.isEmpty())) {
	    MetaMessage composerMsg = new MetaMessage() ;
	    composerMsg.setMessage(0x01, composer.getBytes(), composer.length()) ;
	    MidiEvent midiEvent = new MidiEvent(composerMsg, (long)0) ;
	    track.add(midiEvent) ;
	}
    }

    // Artist
    public void addArtist(String artist, long ticks) 
	throws InvalidMidiDataException {
	if ((artist != null) && (! artist.isEmpty())) {
	    MetaMessage artistMsg = new MetaMessage() ;
	    artistMsg.setMessage(0x01, artist.getBytes(), artist.length()) ;
	    MidiEvent midiEvent = new MidiEvent(artistMsg, (long)0) ;
	    track.add(midiEvent) ;
	}
    }

    public void addPartInfo(long ticks) throws InvalidMidiDataException {
	// Tracks 1-16 ON: FF 7F 43 73 0A 00 05 4F 4F 4F 4F ... 4F
	byte[] tr = {0x43, 0x73, 0x0A, 0x00, 0x05, 0x4F, 0x4F, 0x4F,
		     0x4F, 0x4F, 0x4F, 0x4F, 0x4F, 0x4F, 0x4F, 0x4F,
		     0x4F, 0x4F, 0x4F, 0x4F, 0x4F} ;
	// Ancient Yamaha part information META event. I don't think modern
	// arrangers even recognize this event. EXPAND overwrites these values.
	// 'O'  0x4F  Off
        // 'K'  0x4B  Keyboard
	// 'V'  0x56  Vocal Harmony
	// 'Z'  0x5A  Other
	MetaMessage partInfoMsg = new MetaMessage() ;
	partInfoMsg.setMessage(0x7F, tr, 21) ;
	MidiEvent midiEvent = new MidiEvent(partInfoMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Time signature: FF 58 04 ...
    public void addTimeSignature(int beats, int division, long ticks) 
	throws InvalidMidiDataException {

        byte[] tsm = {0x00, 0x00, 0x00, 0x00} ;
	tsm[0] = (byte)beats ;                    // Beats per measure
	tsm[1] = (byte)getPowerOfTwo(division) ;  // Division
	tsm[2] = 24 ;  // MIDI clocks per metronome click
	tsm[3] = 8 ;   // 32nd notes per quarter note
	MetaMessage timeSigMsg = new MetaMessage() ;
	timeSigMsg.setMessage(0x58, tsm, 4) ;
	MidiEvent midiEvent = new MidiEvent(timeSigMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Key Signature: FF 59 02 ...
    // Key is the number of flats (<0) or sharps (>0). Scale is either
    // major (0) or minor (1). It's all based on the circle of fifths.
    public void addKeySignature(int key, int scale, long ticks)
	throws InvalidMidiDataException {

        byte[] ksm = {(byte)0x00, (byte)0x00} ;
	ksm[0] = (byte)(key & 0x7F) ;
	ksm[1] = (byte)(scale & 0x7F) ;
	MetaMessage keyMsg = new MetaMessage() ;
	keyMsg.setMessage(0x59, ksm, 2) ;
	MidiEvent midiEvent = new MidiEvent(keyMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Tempo: FF 51 03 ...
    public void addTempo(int tempo, long ticks)
	throws InvalidMidiDataException {

	int mtempo = Math.round((float)1000000.0 / (float)(tempo/60.0)) ;
        byte[] tm = {0x04, (byte)0xd1, (byte)0x8d} ;
	tm[0] = (byte)((mtempo >> 16) & 0xFF) ;
	tm[1] = (byte)((mtempo >> 8) & 0xFF) ;
	tm[2] = (byte)(mtempo & 0xFF) ;
	MetaMessage tempoMsg = new MetaMessage() ;
	tempoMsg.setMessage(0x51 , tm, 3) ;
	MidiEvent midiEvent = new MidiEvent(tempoMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Song start measure: FF 7F 06 43 73 0A 00 04 01
    public void addSongStartMeasure(long ticks) throws InvalidMidiDataException {
        byte[] ssm = {0x43, 0x73, 0x0A, 0x00, 0x04, 0x01} ;
	MetaMessage songStartMsg = new MetaMessage() ;
	songStartMsg.setMessage(0x7F , ssm, 6) ;
	MidiEvent midiEvent = new MidiEvent(songStartMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Song offset measure: FF 7F 06 43 73 0A 00 07 01
    public void addSongOffset(long ticks) throws InvalidMidiDataException {
        byte[] som = {0x43, 0x73, 0x0A, 0x00, 0x07, 0x01} ;
	MetaMessage songOffsetMsg = new MetaMessage() ;
	songOffsetMsg.setMessage(0x7F , som, 6) ;
	MidiEvent midiEvent = new MidiEvent(songOffsetMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add XF version ID: FF 7F 09 43 7B 00 58 46 30 32 00 00
    public void addXFVersionId(long ticks) throws InvalidMidiDataException {
        byte[] xfvm = {0x43, 0x7B, 0x00, 0x58, 0x46,
				0x30, 0x32, 0x00, 0x00} ;
	MetaMessage xfVersionMsg = new MetaMessage() ;
	xfVersionMsg.setMessage(0x7F, xfvm, 9) ;
	MidiEvent midiEvent = new MidiEvent(xfVersionMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add GM Reset: F0 7E 7F 09 01 F7
    public void addGMReset(long ticks) throws InvalidMidiDataException {
	byte[] gm = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7} ;
	SysexMessage gmSystemOn = new SysexMessage(gm, 6) ;
	gmSystemOn.setMessage(gm, 6) ;
	MidiEvent midiEvent = new MidiEvent(gmSystemOn, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add XG System ON: F0 43 10 4C 00 00 7E 00 F7
    public void addXGSystemOn(long ticks) throws InvalidMidiDataException {
	byte[] xg = {(byte)0xF0, 0x43, 0x10, 0x4C, 0x00, 0x00, 0x7E, 0x00, (byte)0xF7} ;
	SysexMessage xgSystemOn = new SysexMessage(xg, 9) ;
	xgSystemOn.setMessage(xg, 9) ;
	MidiEvent midiEvent = new MidiEvent(xgSystemOn, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add a MIDI end of track meta event
    public void addEndOfTrack(long ticks) throws InvalidMidiDataException {
	MetaMessage endOfTrack = new MetaMessage();
        byte[] et = {}; // An empty array
	endOfTrack.setMessage(0x2F, et, 0) ;
	MidiEvent midiEvent = new MidiEvent(endOfTrack, (long)ticks);
	track.add(midiEvent) ;
    }

    // Add a MIDI lyric meta event
    public void addLyric(String lyric, long ticks)
	throws InvalidMidiDataException {
	MetaMessage lyricMsg = new MetaMessage() ;
	lyricMsg.setMessage(0x05, lyric.getBytes(), lyric.length()) ;
	MidiEvent midiEvent = new MidiEvent(lyricMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add a MIDI lyric line break meta event
    public void addLineBreak(long ticks)
	throws InvalidMidiDataException {
	byte[] line = {0x0d} ;
	MetaMessage lineMsg = new MetaMessage() ;
	lineMsg.setMessage(0x05, line, 1) ;
	MidiEvent midiEvent = new MidiEvent(lineMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add a MIDI lyric page break meta event
    public void addPageBreak(long ticks)
	throws InvalidMidiDataException {
	byte[] page = {0x0a} ;
	MetaMessage pageMsg = new MetaMessage() ;
	pageMsg.setMessage(0x05, page, 1) ;
	MidiEvent midiEvent = new MidiEvent(pageMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Accompaniment Start: F0 43 60 7A F7
    public void addAccompStart(long ticks) throws InvalidMidiDataException {
	byte[] aa = {(byte)0xF0, 0x43, 0x60, 0x7A, (byte)0xF7} ;
	SysexMessage startMsg = new SysexMessage(aa, 5) ;
	startMsg.setMessage(aa, 5) ;
	MidiEvent midiEvent = new MidiEvent(startMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Accompaniment Stop: F0 43 60 7D F7
    public void addAccompStop(long ticks) throws InvalidMidiDataException {
	byte[] as = {(byte)0xF0, 0x43, 0x60, 0x7D, (byte)0xF7} ;
	SysexMessage stopMsg = new SysexMessage(as, 5) ;
	stopMsg.setMessage(as, 5) ;
	MidiEvent midiEvent = new MidiEvent(stopMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Section Control: F0 43 7E 00 xx 7F F7
    public void addSectionControl(int section, long ticks) 
	throws InvalidMidiDataException {
	byte[] sc = {(byte)0xF0, 0x43, 0x7E, 0x00, 0x00, 0x7F, (byte)0xF7} ;
	sc[4] = (byte)(0x7F & section) ;
	SysexMessage sectMsg = new SysexMessage(sc, 7) ;
	sectMsg.setMessage(sc, 7) ;
	MidiEvent midiEvent = new MidiEvent(sectMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Chord SysEx: F0 43 7E 02 cn ct bn bt F7
    public void addChord(int cn, int ct, int bn, int bt, long ticks) 
	throws InvalidMidiDataException {
	Log.debug("addChord Ticks: " + ticks) ;
	byte[] cc = {(byte)0xF0, 0x43, 0x7E, 0x02, 0x00, 
		     0x00, 0x00, 0x00, (byte)0xF7} ;
	cc[4] = (byte)(0x7F & cn) ;
	cc[5] = (byte)(0x7F & ct) ;
	cc[6] = (byte)(0x7F & bn) ;
	cc[7] = (byte)(0x7F & bt) ;
	SysexMessage chordMsg = new SysexMessage(cc, 9) ;
	chordMsg.setMessage(cc, 9) ;
	MidiEvent midiEvent = new MidiEvent(chordMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Chord XF: FF 7F 07 43 7B 01 cn ct bn bt 
    public void addChordXF(int cn, int ct, int bn, int bt, long ticks) 	throws InvalidMidiDataException {
	Log.debug("addChordXF Ticks: " + ticks) ;
	byte[] cc = {(byte)0x43, 0x7B, 0x01,
		     0x00, 0x00, 0x00, 0x00} ;
	cc[3] = (byte)(0x7F & cn) ;
	cc[4] = (byte)(0x7F & ct) ;
	cc[5] = (byte)(0x7F & bn) ;
	cc[6] = (byte)(0x7F & bt) ;
	MetaMessage chordMsg = new MetaMessage() ;
	chordMsg.setMessage(0x7F, cc, 7) ;
	MidiEvent midiEvent = new MidiEvent(chordMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    // Add Style Code: F0 43 73 01 51 05 00 03 04 00 00 dd dd F7
    public void addStyleCode(int code, long ticks) 
	throws InvalidMidiDataException {
	byte[] sc = {(byte)0xF0, 0x43, 0x73, 0x01, 0x51, 0x05,
		     0x00, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00, (byte)0xF7} ;
	sc[11] = (byte)(0x7F & (code >> 7)) ;
	sc[12] = (byte)(0x7F & code) ;
	SysexMessage styleMsg = new SysexMessage(sc, 14) ;
	styleMsg.setMessage(sc, 14) ;
	MidiEvent midiEvent = new MidiEvent(styleMsg, (long)ticks) ;
	track.add(midiEvent) ;
    }

    public void writeMidiFile(String fileName) {
		midiFileName = fileName ;
		File midiFile = new File(fileName);
		try {
			MidiSystem.write(midiSequence, 1, midiFile) ;
		} catch(Exception e) {
			Log.error("Exception caught " + e.toString()) ;
			Log.error("Couldn't write MIDI file: " + fileName) ;
		}
    }
	
    public void writeMidiStream(OutputStream stream) {
		try {
			MidiSystem.write(midiSequence, 1, stream) ;
		} catch(Exception e) {
			Log.error("Exception caught " + e.toString()) ;
		}
    }	
}

