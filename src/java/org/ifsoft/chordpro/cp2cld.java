package org.ifsoft.chordpro;
/*
 * Chordpro to Yamaha MIDI chords
 */

import java.util.* ;

class cp2cld{
    public static void main(String args[])throws Exception{
        cp2cld ob = new cp2cld();

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

	// Replace the ".cho" extension with ".cld"
	int i = choName.lastIndexOf('.') ;
	String midiName = choName.substring(0,i) + ".cld" ;
	// System.out.println("CLD file: " + midiName) ;

	// Create a Song from a Chordpro file
	Song song = new Song() ;
	song.readChordproFile(choName) ;
	song.createSong() ;
	// song.printSong() ;

	// Convert the internal representation of a song to MIDI
	Song2cld song2cld = new Song2cld() ;
	MidiFile midiFile = new MidiFile() ;
	song2cld.convertSongToMidi(song, midiFile) ;

	// Write the MIDI file
	midiFile.writeMidiFile(midiName) ;
    }

}
