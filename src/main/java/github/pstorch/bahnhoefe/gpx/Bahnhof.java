package github.pstorch.bahnhoefe.gpx;

public class Bahnhof {

	private int id;
	
	private String title;
	
	private double lat;
	
	private double lon;

	private boolean photo;

	public Bahnhof(final int id, final String title, final double lat, final double lon) {
		super();
		this.id = id;
		this.title = title;
		this.lat = lat;
		this.lon = lon;
		this.photo = false;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(final double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(final double lon) {
		this.lon = lon;
	}

	public boolean hasPhoto() {
		return photo;
	}

	public void setHasPhoto(final boolean hasPhoto) {
		this.photo = hasPhoto;
	}
	
}
