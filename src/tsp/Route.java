package tsp;

import java.util.Arrays;

public class Route implements Comparable<Route> {

	private City[] route;
	private double length;

	public Route(City[] route, double length) {
		this.setRoute(route);
		this.setRouteLength(length);
	}

	public Route(Route r) {
		this.setRoute(Arrays.copyOf(r.getRoute(), r.getRoute().length));
		this.setRouteLength(r.length);
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

	public void printRoute() {
		for (City c : this.getRoute())
			System.out.print(c.getName() + ",");
		System.out.println("\n");
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
