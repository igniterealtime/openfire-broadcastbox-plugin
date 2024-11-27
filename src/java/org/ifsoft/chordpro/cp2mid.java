package org.ifsoft.chordpro;
/*
 * Chordpro to Yamaha MIDI chords
 */

import java.util.* ;

class cp2mid{
    public static void main(String args[])throws Exception{
        cp2mid ob = new cp2mid();

	if (args.length < 1) {
	    System.err.println("Missing ChordPro file name") ;
	    return ;
	}

	String choName = args[0] ;
	// System.out.println("ChordPro file: " + choName) ;

	if (! choName.endsWith(".cho")) {
	    System.err.println("Is this is a ChordPro file?") ;
	    System.err.println("Expecting '.cho' extension") ;
	    return ;
	}

	// Replace the ".cho" extension with ".mid"
	int i = choName.lastIndexOf('.') ;
	String midiName = choName.substring(0,i) + ".mid" ;
	// System.out.println("MIDI file: " + midiName) ;

	// Create a Song from a Chordpro file
	Song song = new Song() ;
	song.readChordproFile(choName) ;
	song.createSong() ;
	//song.printSong() ;

	// Convert the internal representation of a song to MIDI
	Song2mid song2mid = new Song2mid() ;
	MidiFile midiFile = new MidiFile() ;
	song2mid.convertSongToMidi(song, midiFile) ;

	// Write the MIDI file
	midiFile.writeMidiFile(midiName) ;
    }

}
