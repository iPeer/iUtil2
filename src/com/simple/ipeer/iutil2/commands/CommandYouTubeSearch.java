/*package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.youtube.YouTube;
import com.simple.ipeer.iutil2.youtube.YouTubeSearchResult;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class CommandYouTubeSearch extends Command {
    
    public CommandYouTubeSearch() {
	registerAliases("youtubesearch", "yt", "ytsearch");
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	engine.getProfiler().start("YTSearch");
	try {
	    String query = additionalData;
	    if (query.length() == 0) {
		throw new CommandException("You must provide something to search for. I'm not a mind reader!");
	    }
	    else {
		List<YouTubeSearchResult> results = (engine == null ? new YouTube(null) : (YouTube)engine.getAnnouncers().get("YouTube")).getSearchResults(query);
		int result = 0;
		for (YouTubeSearchResult r : results) {
		    result++;
		    String out = engine.config.getProperty("youtubeSearchFormat")
			    .replaceAll("%RESULT%", Integer.toString(result))
			    //.replaceAll("%(TOTAL)?RESULTS%", Integer.toString(r.getTotalResults()))
			    .replaceAll("%(USER|(VIDEO)?AUTHOR)%", r.getAuthor())
			    .replaceAll("%(VIDEO)?TITLE%", r.getTitle())
			    .replaceAll("%(VIDEO)?LENGTH%", r.getFormattedLength())
			    .replaceAll("%VIEWS%", Integer.toString(r.getViews()))
			    .replaceAll("%COMMENTS%", Integer.toString(r.getComments()))
			    .replaceAll("%LIKES%", Integer.toString(r.getLikes()))
			    .replaceAll("%DISLIKES%", Integer.toString(r.getDislikes()))
			    .replaceAll("%(VIDEO)?URL%", (engine == null ? "https://youtu.be/" : engine.config.getProperty("youtubeURLPrefix"))+r.getID());
		    engine.send(sendPrefix+" :"+out);
		    if (engine.config.getProperty("youtubeSearchDescriptions").equals("true") && r.hasDescription())
			engine.send(sendPrefix+" :"+engine.config.getProperty("youtubeInfoFormatDescription").replaceAll("%DESCRIPTION%", r.getDescription()));
		}
	    }
	}
	catch (RuntimeException e) {
	    sendReply(sender, sendPrefix, e.getMessage());
	}
	
	catch (SAXException | IOException | ParserConfigurationException e) {
	    engine.logError(e);
	    sendReply(sender, sendPrefix, "Couldn't get search results because an error ocurred.");
	    sendReply(sender, sendPrefix, e.toString()+" at "+e.getStackTrace()[0]);
	}
	
	engine.getProfiler().end();
	
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <keyboard(s)>";
    }
    
    @Override
    public String getHelpText() {
	return "Searches YouTube for the specified query.";
	
    }
    
}
*/