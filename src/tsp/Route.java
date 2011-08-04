package tsp;

public class Route implements Comparable<Route> {

	private City[] route;
	private double length;

	public Route(City[] route, double length) {
		this.setRoute(route);
		this.setRouteLength(length);
	}

	public City[] getRoute() {
		return route;
	}

	public void setRoute(City[] route) {
		this.route = route;
	}

	public double getRouteLength() {
		return length;
	}

	public void setRouteLength(double length) {
		this.length = length;
	}

	@Override
	public int compareTo(Route o) {
		double otherLength = o.getRouteLength();
		if (this.getRouteLength() > otherLength)
			return 1;
		else if (this.getRouteLength() < otherLength)
			return -1;
		else
			return 0;
	}

}
