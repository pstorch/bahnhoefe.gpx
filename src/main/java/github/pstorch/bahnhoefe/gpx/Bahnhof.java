package github.pstorch.bahnhoefe.gpx;

public class Bahnhof {

	private int id;
	
	private String title;
	
	private double lat;
	
	private double lon;
	
	private boolean hasPhoto = false;

	public Bahnhof() {
		super();
	}

	public Bahnhof(int id, String title, double lat, double lon) {
		super();
		this.id = id;
		this.title = title;
		this.lat = lat;
		this.lon = lon;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public boolean hasPhoto() {
		return hasPhoto;
	}

	public void setHasPhoto(boolean hasPhoto) {
		this.hasPhoto = hasPhoto;
	}
	
}
