package com.simple.ipeer.iutil2.youtube;

public class YouTubeSearchResult {
	
	private String title, author, description, formattedLength, videoID;
	private int length, views, likes, dislikes, comments;
	
	public YouTubeSearchResult (String videoID, String author, String title, String description, String formattedLength, int... data) {
		/* INT ORDER SHOULD BE:
		 * 0 = length
		 * 1 = views
		 * 2 = likes
		 * 3 = dislikes
		 * 4 = comments
		 */
		
		if (data.length < 5)
			throw new IllegalArgumentException("Information is missing from this search result.");
		
		this.videoID = videoID;
		this.author = author;
		this.title = title;
		this.description = description.replaceAll("\\[rn]+", " ");
		this.formattedLength = formattedLength;
		this.length = data[0];
		this.views = data[1];
		this.likes = data[2];
		this.dislikes = data[3];
		this.comments = data[4];
		
	}
	
	public String getAuthor() {
		return this.author;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public String getFormattedLength() {
		return getFormattedTime();
	}
	
	public String getFormattedTime() {
		return this.formattedLength;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public int getTime() {
		return getLength();
	}
	
	public int getDuration() {
		return getLength();
	}
	
	public int getViews() {
		return this.views;
	}
	
	public int getLikes() {
		return this.likes;
	}
	
	public int getDislikes() {
		return this.dislikes;
	}
	
	public int getComments() {
		return this.comments;
	}
	
	public String getID() {
		return this.videoID;
	}
	
	public boolean hasDescription() {
		return this.description != "" && this.description != null;
	}
	
	@Override
	public String toString() {
		return "ID:"+getID()+"\nA:"+getAuthor()+"\nT:"+getTitle()+"\nD:"+getDescription()+"\nFL:"+getFormattedLength()+"\nL:"+getLength()+"\nV:"+getViews()+"\nLI:"+getLikes()+"\nDL:"+getDislikes()+"\nC:"+getComments();
	}

}
