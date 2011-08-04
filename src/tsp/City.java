package tsp;

public class City {
	private String name;
	private int id;
	private double x;
	private double y;

	public City(String name, int id, double x, double y) {
		this.setName(name);
		this.setId(id);
		this.setX(x);
		this.setY(y);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

}
