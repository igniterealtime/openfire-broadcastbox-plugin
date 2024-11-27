package org.ifsoft.chordpro;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChordPro2Midi extends HttpServlet {
    private static final Logger Log = LoggerFactory.getLogger( ChordPro2Midi.class );	

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Log.debug("post started");	
		response.setHeader("Content-Type", "audio/midi");

		try {
			BufferedReader reader =  request.getReader();
			
			Song song = new Song() ;
			song.readChordpro(reader) ;
			song.createSong() ;

			Song2mid song2mid = new Song2mid() ;
			MidiFile midiFile = new MidiFile() ;
			song2mid.convertSongToMidi(song, midiFile) ;

			ServletOutputStream stream = response.getOutputStream();
			midiFile.writeMidiStream(stream) ;	
			Log.debug("post stopped");				
		} catch (Exception e) {
			Log.error("post failed", e);
		}
	}
}

